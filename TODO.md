# TODO

- Icons should scale independent of map size
    - When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Universal UI model and switch to corner-only white borders for better aesthetics
- Support arbitrary states which are independently toggleable instead of mutually exclusive
- Unit types additional semantic properties
    - Move limits
- Drawings can be resized with appropriate UI on bounding boxes
- Dialogs minimise, which remembers their position and size and adds them to a taskbar at top or bottom of screen

## Bugs

- Consistency pass, launchpad UI should behave the same as the map viewer UI (ref `MapViewerUiBoxes.kt`) and use shared handlers
- `java.lang.IllegalStateException: Epsilon X square index 100 is outside supported designators.`
    - Input boxes should clamp to min/max permitted values when user input would violate them.