# Agentic Agent Summary

This repo contains a Kotlin-based program built using multiple Maven modules.

- `library/src/main/kotlin/io/hsar/mapgenerator/...` contains lower-level functions such as image generation, GUI tools and utility code.
- `contour-generator/src/main/kotlin/io/hsar/mapgenerator/contourgenerator/...` contains code for rendering large areas of contoured survey-style imagery.
- `facility-generator/src/main/kotlin/io/hsar/mapgenerator/facilitygenerator/...` contains code for rendering blocky factory-style buildings.
- `ship-generator/src/main/kotlin/io/hsar/mapgenerator/shipgenerator/...` contains code for rendering science-fiction starships.
- `render/src/main/kotlin/io/hsar/mapgenerator/commandline/Main.kt` is the executable entry point for the CLI and should own command registration.
- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/MapViewerFrame.kt` is the active KorGE viewer entry point for generated map images and should not depend on `render/`.
  - Use the JVM-specific KorGE Maven artifact; KorGE 5.4 currently works better for Maven-launched Windows GUI runs than KorGE 6.0's JNI OpenGL path.
  - When working in KorGE, consult the entrypoint docs first: https://docs.korge.org/entrypoint/
- `command-gui-swing-legacy/src/main/kotlin/io/hsar/mapgenerator/commandgui/MapViewerFrame.kt` preserves the previous Swing implementation as a reference while the KorGE refactor is evaluated.
  - The map viewer uses scroll-wheel zoom; keep the active seed stable across rerenders so graph and contour layers stay consistent for a single session.
  - CLI layer selection is opt-in via repeated `--layer` flags; keep the GUI's initial layer state and any render calls aligned with the chosen CLI layers so disabled layers are neither shown nor generated.
  - Terrain-based layers should be sampled from stable world-space coordinates derived from `metresPerPixel`, not raw pixel indices, so zoomed rerenders keep the same scale and only gain resolution.
  - Static image layers are loaded from repeated `--image` flags and normalized to the largest image size before being exposed to the viewer, so layer toggles and zooming stay aligned.

# Testing

- Run `mvn test` in the most specific module possible, ideally specifying single files of concern.
- Fix any failing tests.
- Add or update tests for any code changed even if not specifically asked to, using an arrange/act/assert style of test writing.

# Code Style

- Always prefer highly functional Kotlin, with single-lambda declarations, implied return and function chains to using braces.
