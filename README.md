# ❄️ FrostByte Launcher

**Minecraft Java Edition. Beyond the Stars.**

A Minecraft Java Edition launcher for Android, built with Kotlin and Jetpack
Compose, with an original cosmic visual identity — a transparent glass UI
floating over an animated black hole and starfield.

FrostByte is an independent project and is **not affiliated with, endorsed
by, or associated with Mojang Studios or Microsoft**. Minecraft is a
trademark of Mojang Studios / Microsoft.

---

## Status

This repository is under active, phased development. See
[`docs/DEVELOPMENT_STRATEGY.md`](docs/DEVELOPMENT_STRATEGY.md) for the full
roadmap.

- ✅ **Phase 1 — Cosmic UI + Navigation Scaffold** (current)
- ⬜ Phase 2 — Profiles + Settings + Storage
- ⬜ Phase 3 — Versions + Downloads + File Manager
- ⬜ Phase 4 — Java Runtime + Launcher Engine
- ⬜ Phase 5 — Authentication
- ⬜ Phase 6 — Fabric / Forge / NeoForge / Quilt
- ⬜ Phase 7 — Mods + Shaders + Resource Packs
- ⬜ Phase 8 — Controls (touch / keyboard / mouse / gamepad)
- ⬜ Phase 9 — Performance + Diagnostics + Backups
- ⬜ Phase 10 — Testing + CI/CD + Release

Screens for features not yet implemented show an honest "Coming in Phase N"
placeholder rather than fake data — see Section 37 of the PRD ("no fake core
features").

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Build:** Gradle (Kotlin DSL) + GitHub Actions
- **Async:** Kotlin Coroutines + Flow
- **Persistence:** Room + DataStore (from Phase 2)
- **Background work:** WorkManager (from Phase 3)
- **Networking:** Retrofit + OkHttp (from Phase 3)

## Building

```bash
git clone <your-fork-url>
cd FrostByteLauncher
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

If this is a fresh clone and `./gradlew` fails immediately with a wrapper
error, see `gradle/wrapper/README_WRAPPER_JAR.txt` — open the project in
Android Studio once and it will self-heal.

### Requirements

- JDK 17
- Android SDK (API 34), min SDK 26
- Android Studio Koala (2024.1) or newer recommended

## Getting an APK via GitHub Actions (no local Android Studio needed)

1. Push this repository to GitHub.
2. Go to the **Actions** tab → **Debug Build** workflow → **Run workflow**
   (or just push a commit to `main`).
3. When the run finishes, open it and download the **FrostByteLauncher-debug**
   artifact — that's your installable APK (`app-debug.apk`).
4. Transfer it to your Android device and install it (you'll need to allow
   "install from unknown sources" for whichever app you use to open the file).

For a signed **release** build, see
[`docs/RELEASE_SIGNING.md`](docs/RELEASE_SIGNING.md).

## Project Structure

```
FrostByteLauncher/
├── app/                          # Single Android application module
│   └── src/main/java/com/frostbyte/launcher/
│       ├── ui/
│       │   ├── theme/            # Colors, typography, Material3 theme
│       │   ├── space/            # Black hole / starfield / nebula renderer
│       │   ├── components/       # GlassCard and other shared widgets
│       │   ├── navigation/       # NavHost, destinations, adaptive scaffold
│       │   └── screens/          # One package per main section
│       ├── MainActivity.kt
│       └── FrostByteApplication.kt
├── .github/workflows/            # build.yml, release.yml, nightly.yml
└── docs/
```

Deeper modularization (`core/`, `launcher/`, `features/`) lands starting
Phase 2–4 as those subsystems are actually implemented — Phase 1 keeps
everything in `app/` since introducing empty modules ahead of real content
would just be structural noise.

## License

See [LICENSE](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Security

See [SECURITY.md](SECURITY.md) for how to report vulnerabilities.
