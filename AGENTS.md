# Agentic Agent Summary

This repo contains a Kotlin-based program built using multiple Maven modules.

- `library/` contains lower-level functions such as image generation, GUI tools and utility code.
- `contour-generator/` contains code for rendering large areas of contoured survey-style imagery.
- `facility-generator/` contains code for rendering blocky factory-style buildings.
- `ship-generator` contaings code for rendering science-fiction starships.

# Testing

- Run `mvn test` in the most specific module possible, ideally specifying single files of concern.
- Fix any failing tests.
- Add or update tests for any code changed even if not specifically asked to, using an arrange/act/assert style of test writing.
