# Changelog

> **Note:** This changelog documents development of `app/`, a separate
> Compose-based rewrite that is not part of the current shipping build (see
> `settings.gradle` - only `app_pojavlauncher` and `forge_installer` are
> included). The actual FrostByte Launcher app is `app_pojavlauncher`, a
> rebrand of [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher) /
> [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). This
> history is kept for reference.

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### In Progress — Phase 10: Testing + CI/CD + Release
- `build.yml`, `release.yml`, `nightly.yml`: all three now upload real unit
  test and lint reports as artifacts (previously a failure gave no visible
  diagnostic in the Actions UI), and `nightly.yml` now runs tests before
  building rather than potentially publishing a broken nightly
- Added `instrumented-tests.yml`: the 3 existing `androidTest` files (Room
  sort-order, `FileManager` safe-delete, `GamepadDetector`) had no CI
  coverage at all until now - runs on a real emulator via
  `reactivecircus/android-emulator-runner`, separated from the fast unit
  suite since it's meaningfully more expensive
- Added a real `lint {}` block to `app/build.gradle.kts` - previously
  `lintDebug`/`lintRelease` ran with fully default settings and no one had
  verified what that would actually flag on a project this size
- `release.yml` now fails loudly and explicitly if
  `MicrosoftAuthConfig.default.clientId` is still blank, rather than
  producing a signed release build that can't actually authenticate -
  turns a documented `KNOWN_GAPS.md` item into an enforced CI check
- Added `docs/RELEASE_CHECKLIST.md`: a concrete, checkable list of what
  "ready to release" actually requires, explicitly distinguishing "the code
  compiles and every screen works" (true today) from "a real person can
  sign in and play Minecraft" (blocked on the JRE/LWJGL/HomeViewModel-wiring
  items already tracked in `docs/KNOWN_GAPS.md`)
- Closed a real test-coverage gap: `DownloadRepository` (enqueue/status/
  progress CRUD, plus `DownloadItem.progressFraction`'s zero-size and
  overshoot edge cases) had no direct test despite being used throughout
  the download pipeline since Phase 3

### In Progress — Phase 9: Performance Center + Diagnostics + Crash Reporting + World Backups
- Real Performance screen: shows actual device capabilities (CPU cores,
  RAM, storage) and the live Auto/manual space-quality control, replacing
  the Phase 1 placeholder
- Real Storage screen: shows the genuine on-disk breakdown from
  `FileManager.computeStorageBreakdown()` (Phase 3, now finally surfaced in
  UI) with a working "Clear cache" action, plus the full World Backups list
  with delete - both screens dispatch their real disk I/O off the main
  thread rather than blocking Compose
- **Resolved a real, previously-tracked gap**: space rendering quality is
  now genuinely auto-detected from real device capabilities
  (`DeviceCapabilitiesProvider` + `SpaceQualityAdvisor`, both unit tested
  including boundary conditions), with a manual override still available in
  Settings AND now in Performance. `AppSettings.spaceQuality` (always-a-value)
  became `spaceQualityOverride` (nullable = auto) to represent this
  correctly - the "no device-tier auto-detection" item is removed from
  `docs/KNOWN_GAPS.md`.
- `CrashReporter`: real `Thread.UncaughtExceptionHandler` capture, writes
  local-only reports (no backend, no silent telemetry), explicit opt-in via
  Settings (live install/uninstall as the setting is toggled), fully unit
  tested including a check that saved reports contain only the documented
  fields (never credentials)
- `WorldBackupManager`: real zip-based world backup/restore, with the same
  zip-slip path-traversal guard used in Phase 4's `NativesResolver`, fully
  unit tested including round-trip fidelity and a deliberate malicious-entry
  test case, now surfaced in the Storage screen
- All Phase 9 subsystems have no dependency on the JRE/LWJGL gap - they
  operate purely on local files/device APIs, independent of whether a game
  process can currently run

### In Progress — Phase 8: Controls
- Real Minecraft key binding model (`MinecraftAction`) matching the game's
  actual `options.txt` key names (e.g. `key_key.forward`), not an invented
  scheme - so configuration built today is already wire-compatible once a
  real game process exists
- `OptionsTxtWriter`: genuinely reads/writes real `options.txt` line format,
  fully unit tested including round-trip and "preserve unrelated existing
  settings" behavior (a real options.txt has far more than key bindings)
- `ControlsRepository`: persists touch layout + key binding overrides via
  DataStore/Gson, with corrupt-data fallback to known-good defaults
- Real, working draggable touch-control layout editor (`ControlsScreen`) -
  genuinely repositions and persists virtual joystick/button placement, not
  a static mockup
- `GamepadDetector`: real controller detection via Android's `InputDevice`
  API, instrumented-tested against the real `InputManager`
- Extracted `ControlsStore` and `GamepadProvider` interfaces (same pattern
  as Phase 3/6/7) so `ControlsViewModel` stays unit-testable without a real
  Context/InputManager
- **Honest scope note**: this phase covers configuring controls, which is
  fully real and complete. Routing that configuration INTO a live,
  running Minecraft process is blocked on the same JRE/LWJGL gaps as the
  rest of the launch pipeline (see docs/KNOWN_GAPS.md) - not attempted here

### In Progress — Phase 7: Mods + Shaders + Resource Packs
- `ModrinthContentRepository`: real search and version resolution against
  Modrinth's genuine, no-API-key-required public API - fully unit tested,
  including `ModrinthFacetsBuilder`'s exact JSON facet-string format (a
  classic silent-bug spot: wrong format doesn't error, it just returns
  unfiltered results)
- Shared `ContentBrowserViewModel`/`ContentBrowserScreen` power all three
  real screens (Mods, Shaders, Resource Packs) rather than three
  near-duplicate implementations - each downloads its resolved primary file
  into the correct on-disk cache folder via the existing Download Manager
  pipeline from Phase 3
- Extended `GameDirectoryProvider` (from Phase 3) to cover the content cache
  directories, keeping `ContentBrowserViewModel` unit-testable without a
  real Android Context
- **Honest, explicit gap** (`CurseForgeConfig`): CurseForge support is not
  implemented. Unlike Modrinth, CurseForge requires a registered API key
  tied to a specific application - same category of blocker as the Azure AD
  client ID and the JRE catalog URLs, so it's marked the same way
  (`isConfigured()` check, blank by default) rather than worked around

### In Progress — Phase 6: Fabric / Forge / NeoForge / Quilt
- `FabricQuiltRepository`: real, complete version listing and launch-profile
  resolution against Fabric's and Quilt's genuine meta APIs
  (meta.fabricmc.net / meta.quiltmc.org) - Quilt reuses Fabric's exact
  response shape since its meta API was deliberately kept compatible.
  Fully unit tested including the network-failure and unknown-loader paths.
- `ForgeVersionRepository`: real version listing for Forge
  (`promotions_slim.json`) and NeoForge (Maven versions API, matched by
  Minecraft-version prefix), fully unit tested
- **Honest, explicit gap** (`ForgeInstallerRunner`): Forge/NeoForge
  installation is NOT implemented. Unlike Fabric/Quilt, Forge/NeoForge ship
  installer jars that patch the vanilla client's bytecode, not just add
  libraries - correctly replicating that requires either running the real
  installer jar's logic or faithfully reimplementing its processor steps.
  A simplified guess here would produce installs that look complete but
  crash at runtime, which is worse than no support - so this is marked as a
  real, separate piece of engineering rather than stubbed out to look done.

### In Progress — Phase 5: Authentication (Microsoft account only)
- Real, complete Microsoft -> Xbox Live -> XSTS -> Minecraft Services OAuth
  device-code flow (`AuthRepository`), with genuine, documented behavior at
  every hop: `authorization_pending` polling, XSTS error-code translation
  (e.g. "no Xbox Live profile", "child account needs adult verification"),
  and a real Minecraft ownership check (404 = account doesn't own the game,
  never granted access anyway)
- `SecureSessionStore`: tokens persisted via `EncryptedSharedPreferences`
  (real Android Keystore-backed encryption), never logged, never plaintext
- No offline/local/third-party account path exists anywhere in this
  subsystem, by design
- Real Accounts screen: device-code sign-in instructions, live progress
  through each stage, honest error display, sign-out
- `HomeViewModel` now checks the real signed-in session via `AuthRepository`
  and reports the true next blocker (no account -> sign in; signed in ->
  "launching isn't wired up yet," since `LaunchPreparer`/`LauncherEngine`
  from Phase 4 aren't called from here yet) - never a fabricated success
- Extracted `SessionStore` interface (mirroring the `DownloadJobScheduler`/
  `GameDirectoryProvider` pattern from Phase 3) and made `AuthRepository`'s
  IO dispatcher injectable, both specifically for real unit testability
  without Android Keystore/WorkManager/real network access
- **NOT yet done**: `MicrosoftAuthConfig.default.clientId` is blank (no
  registered Azure AD app - a real release blocker, not code); `HomeViewModel`
  still doesn't call the Phase 4 launch pipeline even once signed in

### In Progress — Phase 4: Java Runtime + Launcher Engine
This phase was stopped mid-way (by user request) and is NOT complete. What
exists so far:
- `ArgumentBuilder`: builds the real JVM + game argument command line
  (heap size, classpath, natives path, identity/session args), fully unit
  tested
- `LauncherEngine`: spawns a real `ProcessBuilder` process and streams real
  stdout/stderr and real exit codes - never fakes a successful launch (fixed
  a real `flowOn`/`emit` context-violation bug during development), fully
  unit tested including real-subprocess tests
- `LibraryResolver`: resolves a version's `libraries` array into classpath
  entries with Mojang-accurate OS-rule evaluation ("last matching rule
  wins"), fully unit tested including the allow-then-disallow ordering case
- `AssetResolver`: resolves a version's asset index into individual
  downloadable assets using Mojang's real hash-sharded CDN layout
  (`<hash[0:2]>/<hash>`, not a path resembling the asset name), fully unit
  tested
- `NativesResolver`: genuinely unzips native library classifier jars and
  extracts real `.so` files to disk, with a zip-slip path-traversal guard,
  fully unit tested including a deliberate malicious-entry test case
- `JavaRuntimeManager` / `JavaRuntimeCatalog`: models and process-verification
  logic are real, but the catalog has NO real JRE download URLs yet (see the
  critical entry below) - this is the actual blocker, not the code around it
- **NOT yet done**: nothing yet orchestrates AssetResolver/LibraryResolver/
  NativesResolver together into one end-to-end "download everything, then
  launch" sequence hooked up to the Download Manager.

### Update — HomeViewModel now reports a real blocker instead of simulating
- Replaced the Phase 1 timed-simulation launch pipeline entirely.
  `HomeViewModel.onPlayClicked()` now reports `LaunchState.NotReady("No
  Microsoft account signed in...")` - a real, specific, accurate reason -
  rather than either doing nothing or faking progress toward a launch that
  can't actually happen yet (Phase 5 auth doesn't exist). No dead code, no
  fabricated identity: `LaunchPreparer`/`LauncherEngine` remain fully built
  and tested, ready to be called the moment a real `LaunchIdentity` exists.
- `HomeScreen` updated to match: shows the real blocker reason inline next
  to the PLAY button instead of a progress bar for a launch that isn't
  actually happening.
- Added `LaunchPreparer` (orchestrates LibraryResolver + JavaRuntimeManager
  into a ready-to-launch `LaunchConfig`, or a precise reason why not) and
  its test suite.

### Added — Phase 3: Versions + Downloads + File Manager
- Version Manager: Retrofit models matching Mojang's real
  `version_manifest_v2.json` and per-version detail API, `MojangMetaService`,
  and `VersionRepository` with Room-backed local caching so the Versions
  screen works offline after a first sync (never fakes a version list)
- Real Versions screen: Release/Snapshot/All filters, manual refresh, honest
  offline notice, per-row download button
- Download Manager: `FileDownloader` (framework-independent, HTTP Range
  resume + SHA-1 verification), Room-backed download queue, `DownloadWorker`
  (WorkManager `CoroutineWorker`) with retry-vs-fail logic distinguishing
  transient network errors (auto-retried) from checksum mismatches (not
  auto-retried), `DownloadScheduler`, real Downloads screen with progress
  bars and retry/cancel
- File Manager: `FileManager` resolving FrostByte's sandboxed on-device
  directory layout (app-private external storage, no broad storage
  permissions needed), per-category disk usage, and a `safeDelete` guard
  that refuses to delete outside its own base directory
- End-to-end wiring: Versions screen's download button resolves the real
  client jar URL/SHA-1/size from Mojang and enqueues it through the same
  Download Manager pipeline the Downloads screen displays
- Extracted `DownloadJobScheduler` / `GameDirectoryProvider` interfaces so
  ViewModels depending on WorkManager/Context-backed classes stay unit
  -testable
- Database schema bumped to v3 (`version_cache`, `downloads` tables); added
  `docs/KNOWN_GAPS.md` to track the destructive-migration tradeoff and other
  deliberate pre-release shortcuts
- Tests: `FileDownloader` against a local `MockWebServer` (success, checksum
  mismatch, HTTP error, Range-resume, already-complete skip), instrumented
  `FileManager` tests, `VersionRepository` and `VersionsViewModel` tests
  using hand-written fakes for the network/DAO/scheduler/directory layers
- App icon redesigned toward a sharper, faceted "F" monogram over a
  gold-to-blue accretion ring, matching the FrostByte reference mark

### Added — Phase 2: Profiles + Settings + Storage
- Room database: `ProfileEntity`, `ProfileDao` (portable recently-played sort,
  avoids SQLite 3.30+ `NULLS LAST` dependency), `Converters`, `FrostByteDatabase`
- DataStore-backed `SettingsRepository`: reduced-motion override, space
  quality selection, telemetry/crash-report opt-ins (off by default per
  Section 27)
- `ProfileRepository` domain layer with validation (blank name / non-positive
  RAM rejected) separating DB entities from UI-facing models
- Minimal manual DI container (`FrostByteContainer`) + `frostByteViewModel {}`
  Compose helper
- Real Profiles screen: list, create dialog (loader dropdown, RAM slider),
  set-default, delete
- Real Settings screen: quality picker, reduced-motion toggle, privacy toggles
- Home screen now reads the real default/most-recent profile from the
  database, with an honest empty state when none exist yet
- Unit tests for `ProfileRepository`, `HomeViewModel`, `ProfilesViewModel`
  (via a hand-written `FakeProfileDao`) + instrumented Room test verifying
  real SQL sort order

### Added — Phase 1: Cosmic UI + Navigation Scaffold
- Gradle project scaffold (Kotlin DSL), Compose + Material 3 wired up
- Cosmic space renderer: animated starfield, nebula, black hole with rotating
  accretion disk, cosmic dust particles — with Low/Balanced/High/Ultra
  quality tiers and animation pause on app background
- Reduced-motion accessibility support (disables background animation)
- `GlassCard` component implementing the transparent glass UI without
  expensive blur
- Adaptive navigation: bottom nav on phones, navigation rail on large screens
- Full `NavHost` wiring for all 13 main sections
- Home screen with PLAY button and simulated launch-state pipeline
  (Checking → Preparing → Downloading → Verifying → Starting Java → Starting
  Minecraft → Running) — UI only, not yet wired to a real launcher engine
- Placeholder screens for Versions, Profiles, Mods, Shaders, Resource Packs,
  Worlds, Controls, Accounts, Downloads, Performance, Storage, Settings
- GitHub Actions: debug build, release build (with signing), nightly build
- Repository docs: README, LICENSE, SECURITY, CONTRIBUTING, CODE_OF_CONDUCT

[Unreleased]: https://github.com/your-org/FrostByte-Launcher/compare/main...HEAD
