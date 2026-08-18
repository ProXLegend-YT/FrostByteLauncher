# Contributing to FrostByte Launcher

Thanks for your interest in contributing!

## Development Setup

1. Install Android Studio Koala (2024.1) or newer.
2. Clone the repository and open it in Android Studio — it will resolve the
   Gradle wrapper and SDK automatically.
3. Run the `app` configuration on an emulator or device (min SDK 26).

## Ground Rules (from the project spec)

These are non-negotiable for any PR:

- **No proprietary Minecraft files.** Never commit or bundle Mojang/Microsoft
  copyrighted game assets.
- **No authentication bypasses.** Never add cracked/offline-account features.
- **No credential logging.** Tokens and passwords must never be logged or
  written to plaintext storage.
- **No fake features.** If a feature isn't really implemented, it should show
  an honest "not yet available" state — not mocked/hardcoded success data.
- **No main-thread blocking work.** Network, disk, and Java-process operations
  must run on background dispatchers/coroutines/WorkManager.
- **Respect quality tiers.** Any new visual effect added to the space renderer
  must respect the existing Low/Balanced/High/Ultra tiers and the
  reduced-motion accessibility setting.

## Code Style

- Kotlin, official code style (`kotlin.code.style=official`, already set in
  `gradle.properties`).
- Compose: prefer stateless composables that take state + callbacks as
  parameters; keep `ViewModel`s as the single source of truth for a screen.
- Run `./gradlew lintDebug` and `./gradlew testDebugUnitTest` before opening
  a PR — CI will run both anyway, but it's faster to catch locally.

## Commit / PR Process

1. Fork the repo and create a feature branch.
2. Keep PRs scoped to one phase/feature where possible — see
   `docs/DEVELOPMENT_STRATEGY.md` for the phase breakdown.
3. Ensure `./gradlew assembleDebug` succeeds and CI is green.
4. Open a PR describing what changed and which PRD section it maps to.

## Reporting Bugs / Requesting Features

Use GitHub Issues. For security-sensitive reports, see `SECURITY.md` instead
of opening a public issue.
