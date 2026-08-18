# Development Strategy

FrostByte Launcher is built in 10 phases. Each phase follows the same loop:

```
IMPLEMENT → BUILD → TEST → LINT → FIX → OPTIMIZE → DOCUMENT
```

A phase is never left in a broken-build state before moving to the next one.

| Phase | Scope |
|-------|-------|
| 1 | Project setup + Compose + cosmic UI + black hole renderer + navigation |
| 2 | Profiles + settings + local storage (DataStore/Room) |
| 3 | Version manager + download manager + file manager |
| 4 | Java runtime manager + launcher engine |
| 5 | Microsoft/Minecraft authentication |
| 6 | Mod loader support: Fabric, Forge, NeoForge, Quilt |
| 7 | Mods + shaders + resource packs (discovery, install, management) |
| 8 | Controls: touch editor, keyboard, mouse, gamepad |
| 9 | Performance center, diagnostics, crash reporting, world backups |
| 10 | Testing, GitHub Actions hardening, release process |

## Phase 1 (current) — what's real vs. what's scaffolded

**Real and working:**
- Full Compose UI tree, theming, navigation graph
- Space renderer with actual quality-tier logic and lifecycle-aware pausing
- Home screen state machine (drives real UI transitions)

**Intentionally scaffolded, not yet real:**
- The Home screen's launch sequence is a timed simulation, not a call into a
  real `LauncherEngine` (that class doesn't exist until Phase 4)
- All other screens (Versions, Mods, Shaders, etc.) are honest placeholders
  that state which phase will implement them — no mocked lists or fake
  success states, per Section 37 of the PRD ("No fake core features remain")

## Definition of Done (project-level)

See Section 37 of the original PRD for the full list. Summarized: the APK
builds and installs, every management screen (versions/mods/shaders/etc.) is
functionally real (not mocked), authentication is legitimate, GitHub Actions
produces both debug and signed release artifacts, and documentation is
complete.
