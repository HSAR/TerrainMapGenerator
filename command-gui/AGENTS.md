# Command GUI Notes

- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/MapViewerFrame.kt` is the public viewer entry point for generated map images and should not depend on `render/`.
- `MapViewerFrame` is also the backend compatibility facade. The Compose implementation is loaded from `command-gui-compose` through
  `MapViewerBackendProvider`; keep Compose/Skia types out of the facade and shared save models.
- `command-gui-compose` owns the modern Compose Desktop window, Material controls and tiled Canvas renderer. It must remain a single Compose
  rendering surface; AWT interop is reserved for native operating-system dialogs.
- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/korge/scene/MapViewerScene.kt` owns KorGE window setup, camera world wiring and scene-level composition.
- KorGE is the compatibility backend while workflows move to Compose. New reusable behavior belongs in engine-neutral packages rather than
  under `korge/`.
- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/korge/overlay/GridOverlayLayout.kt` is the source of truth for sticky axis labels, fade behaviour, major/fine grid
  switching and live reference overlays.
    - Keep the JVM-specific KorGE 5.4 dependency for the compatibility backend until that backend is removed; do not upgrade it as part of Compose work.
    - When working in KorGE, consult the entrypoint docs first: https://docs.korge.org/entrypoint/
    - All `command-gui` bitmaps handed to KorGE views should be premultiplied before upload; call `premultipliedIfRequired()` after loading, trimming or composing bitmap content.
    - The map viewer uses scroll-wheel zoom; keep the active seed stable across rerenders so graph and contour layers stay consistent for a single session.
    - CLI layer selection is opt-in via repeated `--layer` flags; keep the GUI's initial layer state and any render calls aligned with the chosen CLI layers so disabled layers are
      neither shown nor generated.
    - Terrain-based layers should be sampled from stable world-space coordinates derived from `metresPerPixel`, not raw pixel indices, so zoomed rerenders keep the same scale and
      only gain resolution.
    - Static image layers are loaded from repeated `--image` flags and normalized to the largest image size before being exposed to the viewer, so layer toggles and zooming stay
      aligned.
- Dialog performance: keep wheel scrolling as a transform-only update. Do not call the dialog's full `render()` for each scroll event; that removes and rebuilds the entire content
  tree, including text inputs and custom panels. Retain the clipped content container and update only its Y offset, rebuilding the scrollbar thumb separately. Use a full render
  only when content, dimensions, or dialog state actually changes.
- Save files may carry an `additionalData.json` payload; preserve it on a best-efforts basis and write it back additively when present, without making it required for normal
  save/load flows.
- Launchpad menu centring, the persistent launch shell, and launch-background regeneration must use `korge/launch/MapViewerViewport.kt`'s `MapViewerViewportResolver`. Its
  `actualVirtualWidth`/`actualVirtualHeight` values are authoritative, with the KorGE window dimensions used only as the startup fallback when either actual value is zero. A fixed
  raster canvas sized to an earlier background creates first-regeneration grid judder and a right-edge black band; each generation must retain its own clipped canvas and the
  outgoing/current canvases must be laid out independently during the transition. Repeated regeneration must advance `korge/launch/LaunchBackgroundGenerationSegments.kt` before
  cancelling the old job, so stale queued tile publications cannot update the current canvas.

## SITREP Contacts

- Sensor contact state is persisted additively in `additionalData.json` under `sitrepContacts`. Each record is keyed by numeric turn and observer unit ID, and records are
  serialized sorted by turn and then observer ID. Existing unrelated additional-data fields must be preserved.
- A unit becomes an active contact when it is within the outer sensor detection range. Its Sierra number is assigned at that point and remains attached to the contact while it
  remains detected. Losing detection removes the active contact and allows a later detection to receive a new number.
- Identification is separate from detection. A contact inside the identification range is identified and can appear with its hull class; a detected contact outside that inner range
  remains an unknown contact. An identified contact stays identified while it remains detected, and is cleared when it becomes hidden.
- Persisted contact records include the active unit ID, Sierra number, and identification state. Loading restores those values before sensor checks are refreshed; refresh then
  removes contacts that are no longer physically detected.
- SCIREP compares the current turn's identified contact IDs with the previous turn's record for the same observer. A contact newly identified on the current turn gets an additional
  Sierra identification sentence; reopening the report on that turn does not change its persisted state.
- The supported class-code display names are FF frigate, F fighter, DD destroyer, CL light cruiser, CA heavy cruiser, CVE escort aircraft carrier, CVL light aircraft carrier, and
  BC battlecruiser. The naval names follow the hull-classification reference at https://en.wikipedia.org/wiki/Hull_classification_symbol where the project codes are represented
  there.
- SCIREP contact segments must be sorted by ascending Sierra number and combined by contact ID before rendering, so each contact has one sentence even when multiple clauses apply.
  When an active contact disappears, retain its last-seen position, range, Sierra number, and identification metadata in the turn record and emit `Contact Sierra N lost.` on that
  turn. A newly detected contact within 3U of a retained lost position inherits that Sierra number as a low-confidence track and reports the last-known position; known
  identification is expressed as `was identified as` in that sentence.
