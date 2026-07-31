# Command GUI Notes

## Current Legacy/Refactored Split

- The migration is intentionally incomplete. There are two selectable map-viewer renderers:
    - `command-gui/.../korge/` is the working legacy KorGE renderer and UI. It is still compiled directly into `command-gui` and remains the default backend.
    - `command-gui-compose` is the modern Compose Desktop renderer and UI. It draws the map with Compose Canvas/Skia and is discovered at runtime through the
      `MapViewerBackendProvider` service entry.
- `command-gui` is not yet a pure core module and there is currently no separate `command-gui-korge` module. Its top-level `commandgui` package contains the public facade plus
  shared, engine-neutral settings, save loading, camera/grid geometry, ledger replay and render-data types; its `korge` subpackages contain legacy-only UI and drawing code.
- `MapViewerFrame` is the compatibility boundary. `MapViewerBackend.KORGE` starts `MapViewerScene` directly; `MapViewerBackend.COMPOSE` loads the provider from
  `command-gui-compose`. The CLI exposes the same choice as `--viewer=korge|compose`, and `render` places Compose on the runtime classpath.
- Both viewers consume the same `MapViewerSettings`, `MapViewerSetupInputs`, `MapViewerRenderData` and `renderMap` callback, but they paint and handle UI independently. Do not pass
  KorGE, Compose or Skia types through those shared interfaces or persisted state.
- `command-gui/.../operation/` is now the source of truth for the new engine-neutral operation document used by Compose and by complete save round trips. It contains packed-ARGB
  drawings, templates backed by `BufferedImage`, placed units/graphics/drawings, notes, immutable change sets, the applied-head timeline, hit testing and transforms. Do not add a
  second Compose-only object or ledger model. `MapViewerSaveRepository.loadOperation/createOperation/saveOperation` is the document persistence boundary.
- `command-gui/.../korge/adapter/LegacyOperationAdapter.kt` is the temporary compatibility bridge between the neutral document and the still-existing KorGE ledger/snapshot types.
  Bitmap conversion is cached there. The legacy scene has not yet been converted to operate directly on `OperationDocument`; therefore its old snapshots/actions remain in use
  behind this adapter until a later extraction slice. Keep all new rules neutral and adapt them at this boundary.
- `command-gui-compose/.../ComposeOperationState.kt` is the authoritative Compose application state. It owns document replay, camera, tool, selection, layers, workspace mode,
  editors, drafts, historical preview, transform preview and revisioned save status. Viewer composables may retain transient pointer/menu layout state, but must not create a
  competing operation source of truth.
- Route changes as follows:
    1. Put reusable rules, state transformations, persistence and geometry in the top-level engine-neutral `commandgui` package and cover them with `command-gui` tests.
    2. Put new product UI and Compose Canvas work in `command-gui-compose`; do not add new KorGE UI merely to keep source files visually parallel.
    3. For required fallback parity, adapt the shared behavior in both renderers while leaving renderer-specific drawing, focus and input code in its owning backend.
    4. For a legacy-only bug, make the smallest fix under `korge/` and avoid copying its scene/view abstractions into Compose.
    5. Do not delete KorGE, change the default backend or remove the selector until an explicit parity/removal task requests it.

## Future Compose Implementation Plan

The Compose viewer is an editable vertical slice, not yet a replacement for KorGE. New and loaded operations now enter the same `OperationDocument`/`ComposeOperationState` path.
It has the styled three-screen launchpad, tiled map display, camera pan/zoom, shared grid layout, live ledger/history controls, revisioned two-second autosave, save-gated window
and
launchpad exit, and rendering/hit testing/move/resize/rotation/deletion for loaded units, graphics and all four drawing shapes. Docked/floating notes and editor records are still
prototype UI; Draw/Measure creation workflows and most operational editors remain to be ported.

### Completed Compose slices

- Complete saves now load into an engine-neutral `OperationDocument` with stable identity/timestamps, settings/render data, camera, asset provenance, generated assets, preserved
  additional JSON and an applied-head `OperationTimeline`. New operations use the same document path and save to the normal generated path; loaded operations overwrite their
  source path. The old `loadMap()` remains only as a compatibility projection.
- `ComposeOperationState` replays the live head or a read-only historical point, prunes selection after replay, truncates redo on a branched edit, keeps transform previews out of
  the ledger until release, and records one change set per gesture. It also tracks dirty/saving/saved revisions so mutations made during an in-flight save schedule a later save.
- Compose Canvas renders saved freeform, line, rectangle and oval drawings with colour/rotation plus graphic and unit assets. Select mode supports real-geometry hit testing,
  marquee selection, group move, corner resize, rotation with Shift snapping, Delete/context deletion, Escape cancellation and Ctrl+Z/Ctrl+Y/Ctrl+Shift+Z/Ctrl+S.
- Additive UI persistence now includes tool, selected IDs, workspace, enabled layers, open editor IDs and drafts while retaining legacy dialogs, dock panels and unrelated
  `additionalData.json` fields. Native close and return-to-launchpad force the latest save and keep the viewer open with Retry/Cancel Exit after failure.

- The Compose workspace now owns its current tool, selected operation objects, ledger cursor and note draft in `ComposeOperationState`. Select hit-tests units with a
  zoom-independent screen target and uses real graphic/drawing geometry; right-button dragging owns panning. Draw and Measure remain explicit
  selected modes awaiting their respective gesture/reducer slices.
- Normalized map coordinates, coordinate deltas, bounds/clamping and map-to-world conversion now live in engine-neutral `commandgui`. KorGE retains only its `Vector2D` adapters,
  while Compose uses the same conversion for replay-marker hit testing.
- `UnitIconDisplaySize` and persisted global icon-size settings now live in engine-neutral `commandgui`. Compose renders decoded unit-template images from replayed saves at that
  zoom-independent screen size, supports right-drag panning under every tool, and mirrors KorGE's `Alt` + wheel 12% icon-size adjustment.
- Compose now places its tool controls in a transparent horizontal map overlay and exposes the legacy Escape menu workflow through `Escape` and a Menu button; its modal provides
  return-to-launchpad, exit and close actions without retaining the obsolete map-generation/open toolbar.
- Context-menu action/target contracts, feature filtering and replay-object option resolution now live in engine-neutral `commandgui`. Compose right-clicks an object to show its
  object-specific menu and otherwise right-drags to pan; KorGE retains only rendering and callback adapters for its existing menus.
- Replayed graphics now carry their save-loaded icon and normalized dimensions through the shared model. Compose renders those icons at map-relative size, with `displayName`
  labels above them and the same display name in ledger and context-menu headers.
- Renderer-neutral selection chrome now lives in `commandgui`: screen selection bounds/frames, grabbable corner brackets, rotation-handle placement, marquee overlap and
  additive-selection reducers. Compose selection is now a stable set of IDs and supports click-to-replace, Shift-click toggling, Shift-marquee toggling, ordinary drag-marquee
  replacement, per-object bounding boxes, group frames, resize brackets and a rotate dot. Transform previews now come from neutral content and commit through the authoritative
  timeline rather than mutating renderer state.

Implement the remaining migration as small vertical slices in this order:

1. **Finish replacing the legacy domain, without another rewrite.** Move the remaining KorGE `LedgerEntry`, snapshot/template/note/drawing and selection-transform behavior onto
   the types in `operation/`, then make `MapViewerScene` consume the neutral document through a renderer adapter. The current legacy adapter is a persistence bridge, not the final
   architecture. Preserve legacy action descriptions and save compatibility with fixtures before deleting each old model.
2. **Close transform parity gaps.** Port the exact rotated rectangle/oval resize math, line endpoint behavior, Alt center-anchor inversion for groups, Shift shape constraints,
   bounds handling and no-op/cancellation rules from `MapSelectionResize`/`MapSelectionPivot` into neutral services. Expand tests to every object/shape and mixed group before
   simplifying or deleting the legacy implementations.
3. **Complete authoritative state plumbing.** Make layer controls, operation notes, editor drafts/validation, editor activation and regeneration mutate `ComposeOperationState`.
   Replace string-only restored editor placeholders with engine-neutral editor identities and persisted bounds/tab metadata. The debounce scheduler already has an injectable clock;
   next inject the saver/exit coordinator so retry and blocked-exit behavior can be tested without a Compose window.
4. **Reach remaining map interaction and rendering parity.** Add Draw and Measure creation gestures, tactical overlays, object labels/properties, layer regeneration/visibility and
   off-screen object culling. Remove `LegacyReplayMapCanvas` and `LegacyReplaySupport.kt` once no tests/callers need the former projection. Add Compose input tests and tolerant
   snapshots for every drawing path and zoom level.
5. **Port operational workflows, not KorGE widgets.** Replace the inert tool rail and prototypes with Compose controls for layer/grid settings, unit and graphic creation/editing,
   custom properties, notes and references, ledger navigation/replay, templates, SITREP/SCIREP, GUNREP, tactical targeting/fire and Olympiad Dawn status. Reuse extracted domain
   behaviour, but redesign the UI with the shared Compose command style and standard multi-line text fields instead of translating KorGE view classes one-for-one.
6. **Complete docked and floating workspaces.** Support multiple simultaneous editor instances with shared drafts and validation. Docked mode needs resizable regions, tabs,
   activation and collapse state. Floating mode needs drag, resize, minimize, overlap ordering, map pinning and viewport clamping. Switching mode must retain the same editor
   objects and drafts rather than close/recreate them; the current notes overlay is only a gesture/layout spike.
7. **Complete persistence depth.** Add dock/tab dimensions and order, active/collapsed panels, typed editor identities, floating bounds, pin/minimize state and map anchors to the
   neutral UI state. Continue reading unversioned saves as version 1, preserving unknown fields and writing legacy dialog records while KorGE remains selectable. Add explicit
   version-1/version-2 and Compose→KorGE→Compose fixtures, including an undone applied head and generated assets.
8. **Harden desktop behaviour and presentation.** Add the native menu/shortcuts, focus traversal, tooltips, context menus, confirmations, snackbars, error/loading states and native
   AWT file-dialog adapters. Apply the EF Nord design consistently without replacing accessible Material semantics. Verify large maps, DPI scaling, resizing, Windows accessibility,
   clipboard/IME/undo and hardware/software rendering. Keep one Compose rendering surface; do not embed Swing or another engine.
9. **Build parity evidence before changing defaults.** Add Compose UI tests for multiline editing and the complete launch/editor workflows; pure tests for transforms, hit targets,
   layer order, sticky grid labels and persistence; and tolerant renderer snapshots at minimum/default/maximum zoom. Run `testAll`, the portable ZIP smoke test and side-by-side
   saved operation fixtures. Only then make Compose the default for one release with explicit KorGE fallback.
10. **Remove the legacy backend only in a dedicated final task.** After the fallback release and confirmed parity, move any last shared code out of `korge/`, delete KorGE views and
   dependencies, remove conversion adapters and the backend selector/provider seam, and simplify `MapViewerFrame`. Do not combine this cleanup with feature-porting work.

For each slice, first locate the equivalent KorGE workflow and its tests, extract or test its behaviour in `command-gui`, implement the Compose state/UI/Canvas adapter, then run
the
most specific tests for both modules. Treat visual similarity as secondary to equivalent operational state and saved content; use tolerant snapshots rather than pixel-identical
font rendering.

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
