# Command GUI Notes

- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/MapViewerFrame.kt` is the public viewer entry point for generated map images and should not depend on `render/`.
- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/korge/scene/MapViewerScene.kt` owns KorGE window setup, camera world wiring and scene-level composition.
- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/korge/overlay/GridOverlayLayout.kt` is the source of truth for sticky axis labels, fade behaviour, major/fine grid
  switching and live reference overlays.
    - Use the JVM-specific KorGE Maven artifact; KorGE 5.4 currently works better for Maven-launched Windows GUI runs than KorGE 6.0's JNI OpenGL path.
    - When working in KorGE, consult the entrypoint docs first: https://docs.korge.org/entrypoint/
    - All `command-gui` bitmaps handed to KorGE views should be premultiplied before upload; call `premultipliedIfRequired()` after loading, trimming or composing bitmap content.
    - The map viewer uses scroll-wheel zoom; keep the active seed stable across rerenders so graph and contour layers stay consistent for a single session.
    - CLI layer selection is opt-in via repeated `--layer` flags; keep the GUI's initial layer state and any render calls aligned with the chosen CLI layers so disabled layers are
      neither shown nor generated.
    - Terrain-based layers should be sampled from stable world-space coordinates derived from `metresPerPixel`, not raw pixel indices, so zoomed rerenders keep the same scale and
      only gain resolution.
    - Static image layers are loaded from repeated `--image` flags and normalized to the largest image size before being exposed to the viewer, so layer toggles and zooming stay
      aligned.
- Save files may carry an `additionalData.json` payload; preserve it on a best-efforts basis and write it back additively when present, without making it required for normal
  save/load flows.
