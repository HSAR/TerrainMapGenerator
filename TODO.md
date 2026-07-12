# TODO

# Features

- FINISH: Optional Olympiad Dawn game logic for friendly and enemy detection narration
    - Ships on active sensors are detected by anything they detect
- Improve autosave performance
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Dialogs minimise, which remembers their position and size and adds them to a taskbar at top of screen
- Main menu background is a generated, scrolling map ref
    - https://openprocessing.org/@u137792/668106
    - https://thingonitsown.blogspot.com/2019/02/finding-perlin-contours.html
        - Avoid using Perlin noise https://noiseposti.ng/posts/2022-01-16-The-Perlin-Problem-Breaking-The-Cycle.html
- Unit types additional semantic properties
    - Move limits
- Non-standard named save files are re-serialised using the non-standard save name
- Universal UI model and switch to corner-only white borders for better aesthetics
- When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Support arbitrary states which are independently toggleable instead of mutually exclusive

## Bugs/Minor

- `java.lang.IllegalStateException: Epsilon X square index 100 is outside supported designators.`
    - Input boxes should clamp to min/max permitted values when user input would violate them.
- Move-induced measure tool isn't coloured by affiliation
- When editing a unit name, the dialog title only updates for the first character
- Generic perf pass on all dynamically sized panels, these should not rebuild trees or force redraw per frame when scrolling