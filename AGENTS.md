# Agentic Agent Summary

This repo contains a Kotlin-based program built using multiple Maven modules.

- `library/src/main/kotlin/io/hsar/mapgenerator/...` contains lower-level functions such as image generation, GUI tools and utility code.
- `contour-generator/src/main/kotlin/io/hsar/mapgenerator/contourgenerator/...` contains code for rendering large areas of contoured survey-style imagery.
- `facility-generator/src/main/kotlin/io/hsar/mapgenerator/facilitygenerator/...` contains code for rendering blocky factory-style buildings.
- `ship-generator/src/main/kotlin/io/hsar/mapgenerator/shipgenerator/...` contains code for rendering science-fiction starships.
- `render/src/main/kotlin/io/hsar/mapgenerator/commandline/Main.kt` is the executable entry point for the CLI and should own command registration.
- [`command-gui/AGENTS.md`](command-gui/AGENTS.md) carries command-gui-specific soft-contract notes close to the code.

# Testing

- Run `mvn test` in the most specific module possible, ideally specifying single files of concern.
    - As far as you are aware, when changing code in an underlying layer check the tests of dependent modules in case signature changes cause breakage.
  - In PowerShell, always quote Maven selector arguments that contain commas, such as `"-Dtest=MapViewerDialogWindowTest,MapViewerDialogLayoutBuilderTest"`, so PowerShell does not
    split or misparse them.
- Fix any failing tests.
- Add or update tests for any code changed even if not specifically asked to, using an arrange/act/assert style of test writing.

# Code Style

- Always prefer highly functional Kotlin, with single-lambda declarations, implied return and function chains to using braces.
- Always make the smallest change possible to fulfil a given request, unless explicitly told to carry out a wider refactor.
