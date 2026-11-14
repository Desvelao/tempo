# AGENTS.md

Guidance for AI coding agents working in this repository. `README.md` is for humans.

## What this is

A native Android brain-training app (Kotlin, Jetpack Compose, Material 3): 13 mini-games across five skills, daily sessions,
per-skill adaptive difficulty, streak, progress charts and a local daily reminder. It is **fully offline**.

## Hard constraints (do not break)

1. **No `INTERNET` permission**, and no analytics, ads, remote config or network libraries. After any dependency
   change, check the merged manifest. WorkManager adds `ACCESS_NETWORK_STATE`; that is fine.
2. **Docker-only toolchain.** Never ask the user to install Java, Gradle, Android Studio or SDK packages on the
   host. Every build, test and `adb` command runs through `docker compose run --rm dev ...`.
3. **Name only in `app.properties`.** The app name, id and URL are build parameters kept in the tracked
   `app.properties` (see below). Do not hard-code the name in code, resources or docs: read it from
   `R.string.app_name` / `BuildConfig`. Internal names stay neutral (`Trainer`, `app.trainer`, `android-dev`).
4. **Native only.** No cross-platform frameworks, no WebView.
5. Do not commit secrets: `release.jks` and `keystore.properties` are git-ignored. Check
   `git status` before committing, and never `git add -A` blindly (the user keeps their own notes in the repo
   root, for example `docs/dev/install_androind_sdk.md`).

6. **Changelog.** Every user-visible change adds a line under `[Unreleased]` in `CHANGELOG.md`. CI
   (`.github/workflows/release.yml`) builds on GitHub's runner, not in Docker; the Docker-only rule is about the
   user's host. When a release is tagged, its entries move from `[Unreleased]` to a
   `## [x.y.z] - date` heading and `[Unreleased]` starts empty.

## Commands

```bash
docker compose build                                   # once, and after Dockerfile changes
docker compose run --rm dev ./gradlew :core-game:test testDebugUnitTest lint   # the standard check
docker compose run --rm dev ./gradlew assembleDebug installDebug               # needs a device/emulator
docker compose run --rm dev ./gradlew bundleRelease assembleRelease           # needs signing (README)
docker compose run --rm dev adb devices                # adb runs in the container (network_mode: host)
```

The `Makefile` wraps these commands (`make check`, `make debug`, `make install`, `make release`, ...; `make help`
lists them). `RUN` defaults to the Docker wrapper; CI calls `make check RUN=` and `make release RUN=` so both
use the same Gradle commands. Keep the Makefile and the workflow in sync.

A change is done when tests pass, `lint` reports **0 errors**, and the release build still builds. Run
`lint` with the tests, because some mistakes (for example `Locale.getDefault()` in Compose) only fail lint.
Do not chain `git commit` after a check with `&&` through a pipe or `grep`: a failed build can still let the
commit run. Check the result first, then commit.

## Layout

```
core-game/        pure Kotlin/JVM, no Android dependencies, all game rules and maths, JUnit tests
app/              Android application (namespace app.trainer)
  data/           Room: entities, DAOs, AppDatabase (migrations!), ResultRepository
  settings/       DataStore preferences, reminder scheduling (WorkManager) and the notification worker
  session/        daily session ViewModel
  games/          GameCatalog, RoundGameViewModel (shared engine), one package per game with its screen content
  ui/             navigation, screens, shared components, theme/ (colours, type)
fonts-license/    licence for the bundled font
```

Keep game logic in `core-game` and out of the UI. All randomness goes through an injected `kotlin.random.Random`
so tests are deterministic. Games that need state across rounds (N-back, Rule switch) implement `reset()`.

## Conventions

- **Add a game:** implement `Game<Round, Answer>` in `core-game` with seeded tests; add a content composable under
  `app/.../games/<name>/`; register it in `GameCatalog` and `GameHost`. Round-based games reuse
  `RoundGameViewModel` and `RoundGameScreen`. Add a `Skill` mapping so ratings and charts pick it up.
- **Database:** any schema change needs a version bump and a real `Migration` (users have data), and the exported
  schema in `app/schemas/` must be committed. Never use destructive migration.
- **Ratings:** `ResultRepository.saveGame` stores the result and moves the skill rating to the level the in-game
  staircase ended on. A session plays each skill at most once.
- **UI text** lives in string resources: English in `res/values`, Spanish in `res/values-es`. Add every new string to both. The app name comes from
  `R.string.app_name`, never hard-coded.
- **Accessibility:** touch targets at least 48dp, `contentDescription` on icon-only controls, and never colour as
  the only cue (the Stroop ink colour is the one exception).

## Design: the "Focus" direction

Near-black surfaces, one lime accent, a blue highlight, large numerals, Space Grotesk (bundled in
`res/font/`, so the app stays offline). Tokens live in `ui/theme/Theme.kt`; build screens from the shared pieces in
`ui/Components.kt` (`PrimaryButton`, `ChoiceButton`, `IconAction`, `FocusBar`, `RoundHistory`, ...). Dark is the
default; a light scheme exists and must keep working. Use `isDarkTheme()` when a colour must differ per theme.

## Gotchas learned the hard way

- AGP 9 has built-in Kotlin: do not apply `org.jetbrains.kotlin.android`. `compileSdk` and `targetSdk` are 37;
  `minSdk` is 26.
- Keep `surfaceVariant` different from `surfaceContainer` in `Theme.kt`, or Material picks the muted text colour
  for text on panels.
- The launcher icon folder must stay `mipmap-anydpi-v26`. Lint suggests `mipmap-anydpi`, but that breaks the build.
- Paint the background behind the system bars (the root `Surface` fills the window; only content is inset),
  otherwise the light theme shows dark bars.
- `weight()` does nothing inside a `verticalScroll` column. Pin footers by putting the scrolling part in a
  `weight(1f)` child.
- The debug and release builds use different signatures: uninstall one before installing the other.
- A released keystore cannot be regenerated. Never delete or overwrite `release.jks`.

## Testing on the emulator

The user runs the emulator on the host (see `docs/dev/install_androind_sdk.md`); agents only talk to it
through `docker compose run --rm dev adb ...`. The application id is `io.github.desvelao.tempo` (from `app.properties`).
`uiautomator dump` plus `input tap` works for driving the UI; grant `POST_NOTIFICATIONS` with
`adb shell pm grant` and test the reminder offline by forcing the WorkManager job:
`adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler <app id> 0`.
Clearing app data (`pm clear`) deletes the user's local history, so say so before doing it.

## App parameters

`APP_NAME`, `APP_ID`, `APP_URL` (link shown in Settings > About), `APP_VERSION_NAME`, `APP_VERSION_CODE` come from, in order: a `-P` Gradle property, an
environment variable, the tracked `app.properties` (holds `APP_NAME`, `APP_ID`, `APP_URL` only; the versions are
set by the release workflow), then the neutral default. Signing uses
`SIGNING_KEYSTORE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` or `keystore.properties`.
