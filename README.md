# Block Nine

A block-dropping puzzle on a sudoku-style 9×9 grid. You get three pieces per turn and have to
place all three before the next three arrive. Filling a 3×3 box, a full row or a full column
clears it and scores. Native Android, Kotlin and Jetpack Compose.

## Building

Requires JDK 21. Gradle's launcher reads `JAVA_HOME` directly, so export it for every
invocation — `org.gradle.java.home` in `gradle.properties` only configures the daemon:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew test          # unit tests
./gradlew assembleDebug # APK at app/build/outputs/apk/debug/
```

`./push.sh` builds and installs straight to a phone paired over wireless debugging.

## Two things a fresh clone does not have

**`app/google-services.json`** — deliberately untracked. Without it the game still builds and
plays; the leaderboard just reports itself as not configured. To turn the leaderboard on:

```sh
npx firebase-tools apps:sdkconfig ANDROID <appId> --project block-nine-game \
    --out app/google-services.json
```

**`blocknine-release.jks` and `keystore.properties`** — the release signing key, also
untracked. Without them `assembleRelease` produces an unsigned APK. This keystore cannot be
regenerated: Android only updates an installed app in place when the new APK carries an
identical signature, so losing it means every existing install has to be uninstalled before it
can be updated again. Keep a backup somewhere other than this machine.

## Layout

| Path | What's in it |
| --- | --- |
| `game/` | Board, pieces and scoring — pure Kotlin, no Android types, all unit-tested |
| `ui/` | Compose screen; the board and pieces are drawn on a `Canvas` |
| `leaderboard/` | Google sign-in and the Firestore daily / monthly / all-time boards |
| `firestore.rules` | Server-side rules; a player can only write their own row, and totals cannot go down |
