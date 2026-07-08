# TODO

- When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Universal UI model and switch to corner-only white borders for better aesthetics
- Support arbitrary states which are independently toggleable instead of mutually exclusive
- Unit types additional semantic properties
    - Move limits
- Dialogs minimise, which remembers their position and size and adds them to a taskbar at top of screen
- Optional Olympiad Dawn game logic for friendly and enemy detection narration
- Main menu background is a generated, scrolling map ref
    - https://openprocessing.org/@u137792/668106
    - https://thingonitsown.blogspot.com/2019/02/finding-perlin-contours.html

## Bugs

- `java.lang.IllegalStateException: Epsilon X square index 100 is outside supported designators.`
    - Input boxes should clamp to min/max permitted values when user input would violate them.
- Escape menu should open regardless of whether dialogs are open or not
- Perf pass, moving dialogs around should not be so impactful