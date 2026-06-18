# TODO

- Save and reload system
    - Includes any and all map layers
    - Includes any and all icon images for units
    - Units properties (name, properties)
        - Custom properties can be created, saved and reloaded as key-value pairs
- Units can be marked destroyed
    - Marking a unit as destroyed is a MapAction - it can be undone with ctrl-Z and is a turn-based change
    - Destroyed units can be hidden or shown with reduced opacity
- Icons should scale independent of map size
    - When zooming out, tokens should gather appropriately (eg. platoons to companies)
- Launchpad UI for selecting a save or starting a new file
- Measure tool should lock to the midpoint of a unit when starting and ending, unless shift key is held
- Universal UI model and switch to corner-only white borders for better aesthetics