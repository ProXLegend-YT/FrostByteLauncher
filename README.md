<div align="center">
  <img src="./app_pojavlauncher/src/main/assets/frostbyte_logo.png" width="150" height="150" alt="FrostByte Launcher logo"><br>

  # FrostByte Launcher

  [![Android CI](https://github.com/ProXLegend-YT/FrostByteLauncher/workflows/Android%20CI/badge.svg)](https://github.com/ProXLegend-YT/FrostByteLauncher/actions)
  [![GitHub commit activity](https://img.shields.io/github/commit-activity/m/ProXLegend-YT/FrostByteLauncher)](https://github.com/ProXLegend-YT/FrostByteLauncher/commits)
  [![Discord](https://img.shields.io/discord/1234567890?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/PNQ4fG6MDK)
  [![License: LGPL v3](https://img.shields.io/badge/License-LGPLv3-blue.svg)](https://github.com/ProXLegend-YT/FrostByteLauncher/blob/main/LICENSE)

</div>

<p align="center">
  A space-themed Minecraft: Java Edition launcher for Android, forked from <a href="https://github.com/MojoLauncher/MojoLauncher">MojoLauncher</a> / <a href="https://github.com/PojavLauncherTeam/PojavLauncher">PojavLauncher</a>.
</p>

---

## ✨ What is FrostByte Launcher?

FrostByte Launcher lets you play **Minecraft: Java Edition** on your Android device — no PC required. It runs almost every version of Minecraft, from ancient rd-132211 builds to the latest snapshots, and supports modding through **Forge**, **Fabric**, and mods like **OptiFine**.

- 🌌 Full galaxy/space visual theme with a transparent UI over a custom background
- ⚡ Peak performance tuning inherited from the MojoLauncher core
- 🎮 Runs almost every Minecraft: Java Edition version, including modded setups
- 🧩 One-tap Forge / Fabric / OptiFine installation via `.jar` installers

## 📖 Navigation
- [Getting FrostByte Launcher](#-getting-frostbyte-launcher)
- [Building from source](#-building-from-source)
- [Roadmap](#-roadmap)
- [Known Issues](#-known-issues)
- [License](#-license)
- [Contributing](#-contributing)
- [Credits & Third-Party Components](#-credits--third-party-components)

## 📦 Getting FrostByte Launcher

You can get FrostByte Launcher via:

1. **[Releases section](https://github.com/ProXLegend-YT/FrostByteLauncher/releases)** — prebuilt APKs.
2. **[GitHub Actions](https://github.com/ProXLegend-YT/FrostByteLauncher/actions)** — early/nightly builds from the latest commits.
3. **[Build from source](#-building-from-source)** — for the fully hands-on route.

## 🛠️ Building from source

Build the launcher (it will automatically download all required components):

```bash
./gradlew :app_pojavlauncher:assembleFullDebug
```

(Replace `./gradlew` with `.\gradlew.bat` on Windows.)

## 🗺️ Roadmap
- [x] Instance system in favor of profiles
- [x] Out-of-the-box 1.21.5 support
- [x] mrpack/CurseForge zip import
- [ ] LTW: resolve issues with Create
- [ ] LTW: enable compute shader/image extensions
- [ ] LTW: switch to a color-renderable format for framebuffers
- [ ] Modpack/mod management tool
- [ ] MMC-compatible instance import
- [ ] Implement common native library standard

## 🐞 Known Issues
- Some physical mice may have very slow mouse speed
- On Holy GL4ES, large texture atlases may be distorted (resulting in stretched/blocky textures in modpacks)
- Probably more — that's why we have a bug tracker 😉

## 📜 License
FrostByte Launcher is licensed under the [GNU LGPLv3](https://github.com/ProXLegend-YT/FrostByteLauncher/blob/main/LICENSE), the same license as its upstream, MojoLauncher.

## 🤝 Contributing
Contributions are welcome! Code, documentation, translations, bug reports — all of it helps. Submit changes as a pull request with a clear description of what changed and how to test it.

## 🙏 Credits & Third-Party Components
FrostByte Launcher is a rebrand built on top of [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), itself based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). All credit for the underlying engineering goes to those projects and the components below:

- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License/[Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) or GNU GPLv2
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [Holy GL4ES](https://github.com/artdeell/gl4es_extra_extra/): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html)
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license
- [LWJGL3](https://github.com/LWJGL/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (exit code trapping): [MIT license](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (ely.by authorisation): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)
- [alsoft](https://github.com/kcat/openal-soft/) (Audio output library): [GNU LIBRARY GENERAL PUBLIC LICENSE](https://github.com/kcat/openal-soft/blob/master/COPYING) and [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)
- Thanks to [Mineskin](https://mineskin.eu/) for providing Minecraft avatars

---

<p align="center">💬 <a href="https://discord.gg/PNQ4fG6MDK">Join the FrostByte Discord</a></p>
