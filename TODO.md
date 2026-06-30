# TODO

- Icons should scale independent of map size
    - When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Universal UI model and switch to corner-only white borders for better aesthetics
- Destroyed units can be hidden or shown with reduced opacity
- Support arbitrary states which are independently toggleable instead of mutually exclusive
- Unit types additional semantic properties
    - Move limits
- Randomly generated operation names
- Drawings can be resized with appropriate UI on bounding boxes

## Bugs

- Too many callbacks warning in game log.
- Consistency pass, launchpad UI should behave the same as the map viewer UI (ref `MapViewerUiBoxes.kt`) and use shared handlers