# Release Checklist

This is the real, concrete list of what must be true before FrostByte
Launcher can ship a public release. It exists so "are we ready to release"
has a checkable answer instead of a vibe.

## Hard blockers (release CI enforces some of these automatically)

- [ ] **Azure AD app registered.** `MicrosoftAuthConfig.default.clientId` is
      a real, non-blank value. `release.yml` fails the build automatically
      if this is still blank - see `docs/KNOWN_GAPS.md`.
- [ ] **`JavaRuntimeCatalog` has real, verified JRE download URLs** for
      every Java major version FrostByte needs to support, each one
      actually tested to execute (`java -version` succeeds) on a real
      Android device via `JavaRuntimeManager.verifyRuntimeRuns`. Not
      verifiable in this development environment - see
      `docs/KNOWN_GAPS.md`.
- [ ] **LWJGL/rendering compatibility layer exists.** Without this,
      Minecraft cannot render even with a working JRE. This is the largest
      remaining piece of engineering in the project - see
      `docs/KNOWN_GAPS.md`.
- [ ] **`HomeViewModel` is wired to the real launch pipeline**
      (`LaunchPreparer`/`LauncherEngine`, both already built in Phase 4)
      instead of reporting "launching isn't wired up yet." This is a
      self-contained follow-up, not new subsystem work, but it hasn't been
      done - see `docs/KNOWN_GAPS.md`.
- [ ] **`fallbackToDestructiveMigration()` replaced with a real
      `Migration`** in `FrostByteDatabase.kt` before any version with real
      user data ships - once this is live, an upgrade must never silently
      wipe profiles/settings.
- [ ] **Signing keystore generated and repository secrets set** - see
      `docs/RELEASE_SIGNING.md`.

## Should-fix before a 1.0 (not hard blockers, but real gaps)

- [ ] CurseForge support (`CurseForgeConfig`) - needs a registered API key.
      Modrinth alone is a complete, working content source, so this is
      additive.
- [ ] Forge/NeoForge installer execution (`ForgeInstallerRunner`) - version
      listing works today; actual installation does not.
- [ ] Gamepad hotplug live-updates (currently recomputed only on
      screen recomposition, not via a real `InputDeviceListener`).

## Process checks

- [ ] `./gradlew testDebugUnitTest` passes locally and in CI
      (`build.yml`).
- [ ] `./gradlew connectedDebugAndroidTest` passes in CI
      (`instrumented-tests.yml`) - covers Room, `FileManager`, and
      `GamepadDetector` against real Android APIs.
- [ ] `./gradlew lintDebug` / `lintRelease` pass (see the `lint {}` block
      in `app/build.gradle.kts` for what's intentionally suppressed and
      why - only noise categories, not correctness/security checks).
- [ ] Every entry in `docs/KNOWN_GAPS.md` has either been resolved (and
      removed from that file, with a note in `CHANGELOG.md`) or is
      explicitly accepted as a known limitation for this release, stated
      in the release notes.
- [ ] Version name/code bumped in `app/build.gradle.kts` and a matching
      `CHANGELOG.md` entry added under a real version header (not
      `[Unreleased]`).
- [ ] Tag pushed matching `v*.*.*` to trigger `release.yml`.

## What "ready to build" vs. "ready to release" means here

The project can be built and run through every screen today - navigation,
profiles, settings, real Mojang/Modrinth API integration, real downloads,
real auth flow UI, real controls editor, real diagnostics - all of it is
genuine, tested code. "Ready to release" specifically means a real person
can sign in with their own Microsoft account and actually launch and play
Minecraft through the app, which needs every item in "Hard blockers" above
to be true. Conflating "the code compiles and the UI works" with "this is
ready to ship" is exactly the kind of overstatement this checklist exists
to prevent.
