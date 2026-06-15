# Agentic Agent Summary

This repo contains a Kotlin-based program built using multiple Maven modules.

- `library/` contains lower-level functions such as image generation, GUI tools and utility code.
- `contour-generator/` contains code for rendering large areas of contoured survey-style imagery.
- `facility-generator/` contains code for rendering blocky factory-style buildings.
- `ship-generator` contaings code for rendering science-fiction starships.
- `render/` is the executable entry point for the CLI and should own command registration.
- `command-gui/` is a Swing GUI library for viewing generated map images and should not depend on `render/`.
  - The map viewer uses scroll-wheel zoom; keep the active seed stable across rerenders so graph and contour layers stay consistent for a single session.
  - CLI layer selection is opt-in via repeated `--layer` flags; keep the GUI's initial layer state and any render calls aligned with the chosen CLI layers so disabled layers are neither shown nor generated.

# Testing

- Run `mvn test` in the most specific module possible, ideally specifying single files of concern.
- Fix any failing tests.
- Add or update tests for any code changed even if not specifically asked to, using an arrange/act/assert style of test writing.

# Code Style

- Always prefer highly functional Kotlin, with single-lambda declarations, implied return and function chains to using braces.
