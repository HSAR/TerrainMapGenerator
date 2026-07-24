# TODO

# Features

- FINISH: Optional Olympiad Dawn game logic for friendly and enemy detection narration
    - Deployables from minelayers
- Improve autosave performance
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Taskbar for minimised windows
- Non-standard named save files are re-serialised using the non-standard save name
- Universal UI model and switch to corner-only white borders for better aesthetics
- When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Other unit type additional semantic properties?

## Bugs/Minor

- `java.lang.IllegalStateException: Epsilon X square index 100 is outside supported designators.`
    - Input boxes should clamp to min/max permitted values when user input would violate them.
- Generic perf pass on all dynamically sized panels, these should not rebuild trees or force redraw per frame when scrolling
- Dialog shadows should not draw over other dialogs
- Can't delete unit types in the new operation screen
- Importing unit types with the same name, even if different affiliations, to the new operation screen doesn't work
- Collapsed unit types list is not a universal list
- Text box cursor drifts away from correct characters especially over multiple lines