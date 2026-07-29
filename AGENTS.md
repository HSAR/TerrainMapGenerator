# Agentic Agent Summary

This repo contains a Kotlin-based program built as a Gradle Kotlin DSL multi-project.

- `library/src/main/kotlin/io/hsar/mapgenerator/...` contains lower-level functions such as image generation, GUI tools and utility code.
- `contour-generator/src/main/kotlin/io/hsar/mapgenerator/contourgenerator/...` contains code for rendering large areas of contoured survey-style imagery.
- `facility-generator/src/main/kotlin/io/hsar/mapgenerator/facilitygenerator/...` contains code for rendering blocky factory-style buildings.
- `ship-generator/src/main/kotlin/io/hsar/mapgenerator/shipgenerator/...` contains code for rendering science-fiction starships.
- `render/src/main/kotlin/io/hsar/mapgenerator/commandline/Main.kt` is the executable entry point for the CLI and should own command registration.
- [`command-gui/AGENTS.md`](command-gui/AGENTS.md) carries command-gui-specific soft-contract notes close to the code.

# Testing

- Run `.\gradlew.bat :module:test` in the most specific module possible, ideally specifying single test classes with `--tests`.
    - As far as you are aware, when changing code in an underlying layer check the tests of dependent modules in case signature changes cause breakage.
- Run `.\gradlew.bat testAll` for the complete repository suite.
- Gradle/Kotlin compilation can exceed the interactive command window. For builds or tests expected to run longer than 60 seconds, launch Gradle as a hidden background process with
  redirected logs, then poll it from separate short PowerShell commands instead of waiting in one foreground command:
  ```powershell
  $log = Join-Path $env:TEMP "terrain-map-generator-gradle.log"
  $errors = Join-Path $env:TEMP "terrain-map-generator-gradle.err.log"
  $process = Start-Process ".\gradlew.bat" -ArgumentList @(":command-gui:test", "--tests", "io.hsar.mapgenerator.commandgui.MapViewportTest", "--no-daemon") -WorkingDirectory (Get-Location) -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $errors -PassThru
  $process.Id
  ```
  Poll with `Get-Process -Id <id> -ErrorAction SilentlyContinue` and `Get-Content $log -Tail 40`; when the process exits, inspect both log files and report the Gradle exit code.
  Keep each polling command short and do not block the interactive command window with `Start-Sleep` for more than 60 seconds.
- Fix any failing tests.
- Add or update tests for any code changed even if not specifically asked to, using an arrange/act/assert style of test writing.

# Code Style

- Always prefer highly functional Kotlin, with single-lambda declarations, implied return and function chains to using braces.
- Always make the smallest change possible to fulfil a given request, unless explicitly told to carry out a wider refactor.
- Persisted Olympiad Dawn fighter IDs are randomly generated four-character `[A-NP-Z0-9_]` tokens; never derive or display them from placement UUIDs.
