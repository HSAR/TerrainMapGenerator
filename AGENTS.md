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
- Maven/Kotlin compilation can exceed the interactive command window. For builds or tests expected to run longer than 60 seconds, launch Maven as a hidden background process with
  redirected logs, then poll it from separate short PowerShell commands instead of waiting in one foreground command:
  ```powershell
  $log = Join-Path $env:TEMP "terrain-map-generator-maven.log"
  $errors = Join-Path $env:TEMP "terrain-map-generator-maven.err.log"
  $process = Start-Process mvn -ArgumentList @("-pl", "command-gui", "-am", "test", "-Dsurefire.failIfNoSpecifiedTests=false") -WorkingDirectory (Get-Location) -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $errors -PassThru
  $process.Id
  ```
  Poll with `Get-Process -Id <id> -ErrorAction SilentlyContinue` and `Get-Content $log -Tail 40`; when the process exits, inspect both log files and report the Maven exit code.
  Keep each polling command short and do not block the interactive command window with `Start-Sleep` for more than 60 seconds.
- Fix any failing tests.
- Add or update tests for any code changed even if not specifically asked to, using an arrange/act/assert style of test writing.

# Code Style

- Always prefer highly functional Kotlin, with single-lambda declarations, implied return and function chains to using braces.
- Always make the smallest change possible to fulfil a given request, unless explicitly told to carry out a wider refactor.
