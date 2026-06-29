# TODO

- Save and reload system
    - Unit custom properties can be created, saved and reloaded as key-value pairs
- Icons should scale independent of map size
    - When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Universal UI model and switch to corner-only white borders for better aesthetics
- Destroyed units can be hidden or shown with reduced opacity
- Support arbitrary states which are independently toggleable instead of mutually exclusive
- Grid supports changing colour to maximise contrast
- Unit types additional semantic properties
    - Move limits
- Randomly generated operation names

## Bugs

- Too many callbacks warning in game log.
- Opening the Edit Unit Type dialog is still inconsistent.
- Cannot yet open multiple non-modal dialogs.
- Consistency pass, launchpad UI should behave the same as the map viewer UI (ref `MapViewerUiBoxes.kt`) and use shared handlers