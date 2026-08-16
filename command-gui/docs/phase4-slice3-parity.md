# Phase 4 Slice 3 parity record

This slice uses manual visual evidence rather than checked-in golden PNGs. Captures belong under the ignored `build/reports/parity/` directory and must not be committed.

## Capture matrix

Capture KorGE and Compose with the same representative operation at 1280×720 and 1920×1080 logical window sizes, at Windows scaling of 100%, 125% and 150%. Record the backend, logical window size, physical image size and scaling beside each pair.

Required states:

- empty viewer chrome and narrow-window tool-rail fallback;
- Game Turns and Settings tabs;
- normal, hovered, keyboard-focused, pressed and disabled buttons;
- root context menu, left-flipped Notes submenu, disabled historical row and destructive row;
- dock tabs, floating title bar and resize corner;
- Escape, drawing-name, save-failure and About modals;
- one and five transient messages.

## Authoritative measurements

- Viewer outer inset: 16 logical px.
- Viewer button: 120×40 logical px; inter-button gap 12 px.
- Narrow tool rail y: 68 px.
- Menu row: 44 px; panel padding 4 px; minimum width 180 px; border 2 px.
- Submenu corridor: 8 px; close delay 180 ms.
- Floating title bar: 42 px.
- Notice inset/gap: 16/8 px; maximum five; lifetime/fade 10,000/420 ms.

Exact geometry remains covered by neutral and Compose tests. Manual review is for rasterisation, font metrics, state colour, alignment and DPI artefacts.

## Deliberate differences from KorGE

- Compose uses the accessible keyboard focus ring even where KorGE focus is pointer-only.
- Unsupported, unreadable and cancelled file selections have distinct outcomes; cancellation is silent.
- Modal and transient feedback is centrally queued instead of allowing overlapping windows.
- Known KorGE clipping, one-character wrapping and unparented chooser behaviour must not be reproduced.
