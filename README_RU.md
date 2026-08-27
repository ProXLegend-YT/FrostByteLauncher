<div align="center">
  <img src="./app_pojavlauncher/src/main/assets/frostbyte_logo.png" width="150" height="150" alt="FrostByte Launcher logo"><br>

  # FrostByte Launcher

  [![Android CI](https://github.com/ProXLegend-YT/FrostByteLauncher/workflows/Android%20CI/badge.svg)](https://github.com/ProXLegend-YT/FrostByteLauncher/actions)
  [![GitHub commit activity](https://img.shields.io/github/commit-activity/m/ProXLegend-YT/FrostByteLauncher)](https://github.com/ProXLegend-YT/FrostByteLauncher/commits)
  [![Discord](https://img.shields.io/discord/1234567890?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/PNQ4fG6MDK)
  [![License: LGPL v3](https://img.shields.io/badge/License-LGPLv3-blue.svg)](https://github.com/ProXLegend-YT/FrostByteLauncher/blob/main/LICENSE)

</div>

<p align="center">
  Лаунчер Minecraft: Java Edition для Android в космической тематике, форк <a href="https://github.com/MojoLauncher/MojoLauncher">MojoLauncher</a> / <a href="https://github.com/PojavLauncherTeam/PojavLauncher">PojavLauncher</a>.
</p>

---

## ✨ Что такое FrostByte Launcher?

FrostByte Launcher позволяет играть в **Minecraft: Java Edition** на устройстве Android — без ПК. Поддерживается почти каждая версия Minecraft, от старых сборок rd-132211 до последних снапшотов, а также моддинг через **Forge**, **Fabric** и такие моды, как **OptiFine**.

- 🌌 Полная космическая/галактическая визуальная тема с прозрачным интерфейсом поверх собственного фона
- ⚡ Настройки производительности, унаследованные от ядра MojoLauncher
- 🎮 Работает почти с любой версией Minecraft: Java Edition, включая модификации
- 🧩 Установка Forge / Fabric / OptiFine в один тап через `.jar`-установщики

## 📖 Навигация
- [Как получить FrostByte Launcher](#-как-получить-frostbyte-launcher)
- [Сборка из исходного кода](#️-сборка-из-исходного-кода)
- [Планы развития](#️-планы-развития)
- [Известные проблемы](#-известные-проблемы)
- [Лицензия](#-лицензия)
- [Участие в разработке](#-участие-в-разработке)
- [Благодарности и сторонние компоненты](#-благодарности-и-сторонние-компоненты)

## 📦 Как получить FrostByte Launcher

Получить FrostByte Launcher можно несколькими способами:

1. **[Раздел Releases](https://github.com/ProXLegend-YT/FrostByteLauncher/releases)** — готовые APK.
2. **[GitHub Actions](https://github.com/ProXLegend-YT/FrostByteLauncher/actions)** — ранние/ночные сборки из последних коммитов.
3. **[Сборка из исходного кода](#️-сборка-из-исходного-кода)** — для полностью самостоятельной сборки.

## 🛠️ Сборка из исходного кода

Соберите лаунчер (все необходимые компоненты будут загружены автоматически):

```bash
./gradlew :app_pojavlauncher:assembleFullDebug
```

(На Windows используйте `.\gradlew.bat` вместо `./gradlew`.)

## 🗺️ Планы развития
- [x] Система инстансов вместо профилей
- [x] Поддержка 1.21.5 "из коробки"
- [x] Импорт mrpack/CurseForge zip
- [ ] LTW: решить проблемы с Create
- [ ] LTW: включить compute shader/расширения изображений
- [ ] LTW: перейти на цветовой формат, поддерживающий рендеринг, для фреймбуферов
- [ ] Инструмент управления модпаками/модами
- [ ] Импорт инстансов, совместимых с MMC
- [ ] Реализовать единый стандарт нативных библиотек

## 🐞 Известные проблемы
- На некоторых физических мышах скорость курсора может быть очень низкой
- На Holy GL4ES большие текстурные атласы могут искажаться (растянутые/блочные текстуры в модпаках)
- Наверняка есть и другие — для этого у нас есть баг-трекер 😉

## 📜 Лицензия
FrostByte Launcher распространяется под лицензией [GNU LGPLv3](https://github.com/ProXLegend-YT/FrostByteLauncher/blob/main/LICENSE) — той же, что и у оригинала, MojoLauncher.

## 🤝 Участие в разработке
Вклад в проект приветствуется! Код, документация, переводы, отчёты об ошибках — всё это помогает. Отправляйте изменения в виде pull request с чётким описанием того, что изменилось и как это проверить.

## 🙏 Благодарности и сторонние компоненты
FrostByte Launcher — это ребренд, построенный на основе [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), который, в свою очередь, основан на [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). Вся заслуга за базовую инженерную работу принадлежит этим проектам и компонентам ниже:

- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [лицензия GNU LGPLv3](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): неизвестная лицензия/[Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) или GNU GPLv2
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [Holy GL4ES](https://github.com/artdeell/gl4es_extra_extra/): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [лицензия GNU GPLv2](https://openjdk.java.net/legal/gplv2+ce.html)
- [GLFW](https://github.com/MojoLauncher/glfw): [лицензия zlib](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): лицензия 3-Clause BSD
- [LWJGL3](https://github.com/LWJGL/lwjgl3): [лицензия BSD-3](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)
- [pro-grade](https://github.com/pro-grade/pro-grade) (менеджер безопасности Java): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (перехват кодов завершения): [лицензия MIT](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (авторизация ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)
- [alsoft](https://github.com/kcat/openal-soft/) (библиотека вывода звука): [GNU LIBRARY GENERAL PUBLIC LICENSE](https://github.com/kcat/openal-soft/blob/master/COPYING) и [изменённый PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)
- Благодарность [Mineskin](https://mineskin.eu/) за предоставление аватаров Minecraft

---

<p align="center">💬 <a href="https://discord.gg/PNQ4fG6MDK">Присоединяйтесь к Discord FrostByte</a></p>
