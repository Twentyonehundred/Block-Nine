import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 has built-in Kotlin support, so no separate kotlin-android plugin is applied.
    alias(libs.plugins.kotlin.compose)
    // Put on the classpath but applied conditionally below.
    alias(libs.plugins.google.services) apply false
}

// The leaderboard is an optional extra: without google-services.json the game still builds
// and plays with its local best score, and the leaderboard simply reports itself as not set
// up. Applying the plugin unconditionally would hard-fail the build instead.
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.lifecycle("Block Nine: no google-services.json, building without the leaderboard backend.")
}

// Release signing. Locally these come from keystore.properties (gitignored); on CI the same
// values arrive as environment variables from the repository secrets. Both paths must resolve
// to the *same* keystore, because Android only updates an installed app in place when the new
// APK carries the identical signature.
val signingProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, envVar: String): String? =
    signingProps.getProperty(key) ?: System.getenv(envVar)

val releaseStorePath = signingValue("storeFile", "SIGNING_STORE_FILE")
val releaseStore = releaseStorePath?.let { path ->
    // CI hands over an absolute path to a decoded temp file; keystore.properties uses one
    // relative to the repo root.
    file(path).takeIf { it.isAbsolute && it.exists() } ?: rootProject.file(path).takeIf { it.exists() }
}

android {
    namespace = "com.chrissmith.blocknine"
    // Compose 1.12 requires API 37; the SDK now ships minor platform versions (37.1).
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.chrissmith.blocknine"
        minSdk = 26
        targetSdk = 37
        // Every CI run must publish a higher versionCode than the last, or Android refuses the
        // in-place update. The GitHub Actions run number does that for free and never repeats.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.$versionCode"
    }

    signingConfigs {
        if (releaseStore != null) {
            create("release") {
                storeFile = releaseStore
                storePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Left unsigned when no keystore is available, so a plain `assembleRelease` on a
            // fresh clone still builds instead of failing on a missing secret.
            signingConfig = signingConfigs.findByName("release")
        }
        // No applicationIdSuffix on debug: Google Sign-In keys off the package name, and a
        // suffixed debug build would need its own Firebase app registration.
        debug {}
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
}
