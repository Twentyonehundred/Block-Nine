# Working on Block Nine

Read `README.md` first — it covers building, the two untracked files a fresh clone lacks, and
the directory layout. This file is the things that aren't obvious from the code and that have
already cost someone an afternoon.

## Toolchain traps

**Never apply `org.jetbrains.kotlin.android`.** AGP 9 has built-in Kotlin support and the two
conflict. The `kotlin-android` alias still sits in `gradle/libs.versions.toml` looking
available — it is deliberately not applied by anything. Only `kotlin-compose` is.

**Export `JAVA_HOME` on every `./gradlew` invocation.** `org.gradle.java.home` in
`gradle.properties` configures the daemon, not the launcher that reads it.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
```

`apksigner` needs it too, plus `PATH="$JAVA_HOME/bin:$PATH"`.

**adb is not on `PATH`.** Take the SDK root from `local.properties`, as `push.sh` does. The
build tools (`aapt2`, `apksigner`) live under `<sdk>/build-tools/37.0.0/`.

**`versionCode` comes from the environment**, defaulting to 1:

```sh
VERSION_CODE=30 ./gradlew assembleRelease
```

Android refuses an install that goes backwards, so a build without it will fail to install
over anything. `./push.sh` reads the installed code off the phone and builds one past it.

## Release builds

Resource shrinking is on, and `LeaderboardRepository` looks `default_web_client_id` up via
`Resources.getIdentifier()` rather than `R.string`. The shrinker can't see that, so
`res/raw/keep.xml` protects it. It has been dropped once already, which broke sign-in at
runtime with a misleading "Google sign-in is not enabled on the Firebase project". **Verify
per release:**

```sh
aapt2 dump resources app/build/outputs/apk/release/app-release.apk | grep -c default_web_client_id
# expect 1
```

Deploying Firestore config — use `npx firebase-tools`, never `gcloud`:

```sh
npx --yes firebase-tools@latest deploy --only firestore:rules,firestore:indexes --project block-nine-game
```

Adding a game mode means adding its collection to **both** `firestore.rules` and
`firestore.indexes.json` (a day and a month composite index each), then deploying.

## Conventions

`game/` is pure Kotlin with no Android types and is unit-tested; keep it that way. Rules
belong there, not in the ViewModel or the Canvas.

Comments explain **why**, not what, and the existing ones are dense and written in prose.
Match that. A comment restating the code is worse than none.

Board movement uses one signed convention: `shift[i]` is *the row a tile came from minus the
row it is in now*. Positive means it rose (a tide surge), negative means it fell (a collapse).
One convention drives one canvas animation for both.

`Board` stores `List<Int>` where `EMPTY = 0` and a filled cell holds `colorSlot + 1`.
`Pieces.COLOUR_SLOTS = 6`, and **slot 6 is reserved for tide tiles**, which render drab.

## Settled product decisions

Don't re-pitch these; they've been considered and closed.

- **No undo.** Rejected outright.
- **No sound.** Rejected outright.
- Three pieces per turn, and all three must be placed before the next three arrive.
- Modes are **Classic**, **Rising Tide** and **Collapse**. Each keeps separate records, local
  and online, so one mode's difficulty can't flatter or spoil another's board. Classic keeps
  the original unprefixed keys and the original `players` collection — that's what stops
  existing installs losing their history.
- Rising Tide ends **only** on "no moves left". Drowning was retired: reaching over the top
  edge takes a column filled in all nine rows, and a full column clears, so the condition was
  unreachable-if-correct.
- Collapse is **local**: a tile falls by exactly the number of cleared cells beneath it in its
  own column, never further. Whole-board compaction was tried and reverted — it destroyed free
  placement and wasn't traceable. A consequence worth knowing: a row clear drops every column
  by one, which merely translates the board and can never line anything new up, so **chains
  come from box clears**.

`GameMode.rules` currently has nowhere to display for Rising Tide and Collapse — only
`MainMenuScreen` reads it, and only for Classic. Either give it a first-play card or delete
the text; undecided.
