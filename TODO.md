# TODO

# Features

- FINISH: Optional Olympiad Dawn game logic for friendly and enemy detection narration
    - Deployables from minelayers
- Improve autosave performance
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Dialogs minimise, which remembers their position and size and adds them to a taskbar at top of screen
- Main menu background improvements
    - Scrolling
    - Generated as tiles using threads, pixelating in when finished
- Non-standard named save files are re-serialised using the non-standard save name
- Universal UI model and switch to corner-only white borders for better aesthetics
- When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Support arbitrary states which are independently toggleable instead of mutually exclusive
- Other unit type additional semantic properties?

## Bugs/Minor

- `java.lang.IllegalStateException: Epsilon X square index 100 is outside supported designators.`
    - Input boxes should clamp to min/max permitted values when user input would violate them.
- Generic perf pass on all dynamically sized panels, these should not rebuild trees or force redraw per frame when scrolling
- Unit type list in map viewer should match unit type list in new operation unit type mode
- Dialog shadows should not draw over other dialogs
- Cannot select, resize or move graphics