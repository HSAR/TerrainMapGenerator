# ADR 0001: Compose Desktop owns both UI and map rendering

Status: accepted

## Context

The KorGE viewer implements desktop controls—including multiline text editing, scrolling, focus,
selection, window chrome and context menus—inside a game-oriented scene graph. Recent fixes show
that this makes ordinary desktop behavior expensive to maintain. The map renderer itself needs
raster images, lines, paths, labels, transforms and pointer hit testing, but not physics, 3D,
audio or an independent game loop.

The target is Windows 10/11 with a single modern Kotlin UI and rendering surface.

## Decision

Use Compose Multiplatform Desktop for the window and Material 3 controls. Render the map through
Compose `Canvas`, backed by the same Skia/Skiko surface as the rest of the application. Keep the
camera and hit-testing state engine-neutral, tile large raster images before GPU upload, and use
AWT only for native operating-system dialogs.

KorGE remains selectable during migration through `MapViewerBackendProvider`. It is a temporary
compatibility backend, not an interop surface inside the Compose window.

## Alternatives

- **libGDX / Scene2D:** the best alternative game engine considered. It includes `TextArea`,
  `ScrollPane`, tables, trees and dialogs, but has no native Compose integration. Embedding it
  would retain separate focus, input, accessibility, layout and rendering systems.
- **JavaFX / FXGL:** mature desktop controls, but using them with Compose would create another UI
  toolkit boundary and FXGL does not add capabilities needed by this 2D viewer.
- **OpenRNDR:** Kotlin-friendly graphics, but deliberately lower-level than a desktop application
  toolkit and would still require interop.
- **Direct Skiko:** suitable for isolated low-level draw operations, but Compose already owns its
  Skiko surface; creating another layer would add lifecycle and input complexity.
- **Dear ImGui bindings:** useful for debug tooling, but weak for native text behavior,
  accessibility and long-form application workflows.
- **Image-only pan/zoom libraries:** unsuitable as viewport owners because map selection, drawing,
  sticky grid labels and pinned editors require the application camera to remain authoritative.

## Consequences

- Basic controls and multiline editing use supported Compose implementations instead of custom
  scene-graph controls.
- The map and UI share one compositor, input pipeline and accessibility tree.
- Rendering code must remain deterministic and tested independently from Composables.
- KorGE-specific types must not enter new shared or persisted state.
- Kotlin/Compose/Gradle versions are upgraded as one compatibility set.

## References

- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Compose compatibility and versions](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [Compose native distributions](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html)
- [Skiko](https://github.com/JetBrains/skiko)
- [libGDX Scene2D UI](https://libgdx.com/wiki/graphics/2d/scene2d/scene2d-ui)
