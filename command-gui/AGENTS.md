# Command GUI Notes

## Migration Contract and Current Architecture

The migration is intentionally incomplete. Keep these boundaries accurate while it proceeds:

- There are two selectable viewers. KorGE under `command-gui/.../korge/` is still the default compatibility backend. `command-gui-compose` is the modern Compose Desktop viewer,
  loaded through the `MapViewerBackendProvider` service. `MapViewerFrame` and `--viewer=korge|compose` remain the public compatibility seam.
- `command-gui` is a mixed facade/core/legacy module, not yet the final module split. Engine-neutral code belongs in the top-level `commandgui` or `commandgui.operation` packages;
  KorGE views, events and bitmap types stay below `korge`. Compose and Skia types stay in `command-gui-compose`.
- `operation/OperationCore.kt` is the source of truth for Compose operation content: templates, units, graphics, drawings, notes, change sets, timeline and document metadata.
  `operation/OperationTimelineAuthoring.kt` is the authoritative constructor and presentation model for timeline entries, turn markers/groups, canonical descriptions and affected
  editor targets. Compose and archive adapters must use it rather than assigning entry kinds or descriptions independently.
  `operation/OperationTransforms.kt`, `OperationTransformGestures.kt` and `OperationSelectionGeometry.kt` are authoritative for transforms, hit testing, selection frames and
  marquee geometry in both viewers. KorGE adapts these rules through snapshot/projection wrappers; do not recreate transform math below `korge` or in Compose.
- `ComposeOperationState` is the authoritative Compose session state. It owns the document/head replay, camera, tool, selection, historical preview, transform preview, save
  revisions and the neutral `OperationWorkspaceState`. Composables may own only active drag/resize deltas; persistent editor identity, typed drafts, dock placement and floating
  state belong in `commandgui.operation`.
- `MapViewerSaveRepository.loadOperation/createOperation/saveOperation` is the authoritative operation archive entry point for both launchpads. `OperationSaveLoader` reads an
  archive
  once into `OperationDocument` data, and the ledger codec reads/writes `OperationTimelineEntry`, `OperationChangeSet` and `OperationContent` directly. Save codecs must not import
  `korge` or `korlibs`; camera, bitmap and action-history conversion belongs in `korge.adapter` or the KorGE viewer boundary.
- KorGE now accepts a loaded `OperationDocument` and converts its neutral timeline once on viewer entry. Its own `LedgerEntry`, snapshot, template, note and drawing controllers
  remain
  temporary runtime models; convert them to a neutral timeline only when saving, and never reintroduce prefix replay or KorGE source-of-truth models into archive code.
- `OperationTimelineEntry.componentChangeSets` retains the sequential neutral members of a persisted batch. Keep this when adapting or editing timelines: an aggregate before/after
  change cannot represent batches whose actions cancel, while each top-level archive record must remain exactly one timeline entry.
- Preserve KorGE behavior while it is the default, but do not build new product UI in KorGE merely for symmetry. Extract reusable rules first, then give each renderer a thin
  drawing/input adapter. Do not change the default or remove the selector before the parity gates below are met.

## Current Compose Capability

Treat the following as implemented and protected behavior, not future work:

- The styled main/new/existing-operation launchpad opens both new and saved operations into the same `OperationDocument`/`ComposeOperationState` path.
- The map uses a tiled Compose Canvas with cursor-anchored zoom, right-drag panning, saved camera restoration, the shared fading/sticky grid layout and zoom-independent unit icons.
- Units, graphics and all persisted drawing shapes render from neutral content. Selection supports click/Shift-click, marquee, group frames, move, resize, rotation, deletion and
  immutable previews committed as one timeline entry.
- Draw creates Freeform, Line, Rectangle and Oval objects through the neutral reducers in `MapViewerToolGestures.kt`, including colour/mode controls, Shift constraints, preview,
  cancellation and one-entry commit. Measure is transient, grid-aware and remains available during historical replay.
- The live ledger supports current/history selection, read-only preview, undo/redo, branch truncation and selection pruning. Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z, Ctrl+S, Delete and Escape
  are wired.
- The notes browser, note/unit/graphic-template editors and ordinary unit/graphic editors share typed editor instances in a resizable right dock or floating windows. Notes are
  timeline-backed and support multiline drafts, ordering, map-picked object references, missing-reference preservation and object-context links. The Compose unit and
  graphic palettes create, edit and safely delete templates, and their click-then-map placement modes use immutable previews before recording one object entry. Graphic dimensions
  are template-owned and propagate to every live instance; archive and KorGE boundaries mirror them through the established normalized-size property keys. Unit edits cover
  template, grid-aware position, name, facing, destroyed state, affiliation and custom properties; graphic edits cover template, display label, grid-aware position and instance
  properties. Campaign-specific Olympiad Dawn panels remain deferred to Phase 3.
- Revisioned two-second autosave, manual save, save-gated return/close, in-flight mutation handling and Retry/Cancel Exit are implemented. New operations save without requiring a
  first edit.
- Additive UI schema version 6 stores typed note, unit and graphic drafts (including bounded embedded template images) while continuing to write version-2
  IDs/drafts/dock panels and KorGE `notes`, `note` and `unit-edit` dialog records. Version-1 through version-5 and recognized legacy dialogs migrate into functional typed editors; unknown
  additional-data and unsupported legacy dialog/draft fields remain preserved.

Do not overstate these partial implementations:

- `enabledLayerIds` is state/persistence only; Compose has no real layer controller and Canvas still paints the supplied base map and grid directly.
- The workspace implementation is deliberately a prototype: only the right dock is rendered, tabs cannot yet be interactively reordered or moved between regions, and only notes
  plus the ordinary unit/graphic and template editors have functional Compose bodies. The neutral model already retains generic regions, tab order and opaque restored editor
  records for later ports.
- Unit/graphic-template creation/edit/delete, unit/graphic placement, ordinary `Edit Unit`/`Edit Graphic`, and delete context-menu actions are functional. SITREP and the other
  legacy commands are still absent.
- The old `loadMap()`, `MapViewerSavedMap`, `MapViewerLedgerReplay`, replay Canvas and projection-only tests have been removed. Do not add a second map/replay loader; derive views
  from `OperationDocument.content` or its timeline.
- Layer management remains deliberately deferred. `enabledLayerIds` is persisted state only; do not treat it as authoritative rendering until a separately approved layer slice.

## Migration Plan

Use a strangler migration, not a second big-bang rewrite. For each workflow: locate the KorGE rule and tests, express the rule against neutral types, add neutral parity tests,
connect Compose to the rule, and leave only rendering/input adaptation in each backend. Remove a legacy model only after all its consumers have crossed the boundary.

### Phase 1 — Stabilise the neutral editing foundation

Completed: transform gestures and selection geometry now use neutral operation content and projection APIs in both viewers. The old KorGE resize/pivot implementations and unused
Compose replay renderer have been deleted; parity coverage includes every persisted drawing kind, mixed selections, graphics, modifiers, snapping, cancellation and handles.

Completed: string editor IDs and viewer-local drafts/windows have been replaced by `OperationWorkspaceState`. Notes and unit editors restore functionally in docked/floating modes;
schema-v3 persistence remains bidirectionally compatible with the KorGE unit-dialog record.

Completed: operation persistence is document-native. Archive loading is single-pass and incremental, applied timeline entries are encoded directly, redo branches are intentionally
discarded on restart, thumbnails and image loading consume neutral models, and KorGE conversion is confined to viewer/adaptor boundaries. The obsolete save projection and duplicate
KorGE catalogue are deleted. Legacy tactical additional-data helpers remain under `korge.persistence`, layered over the neutral additive UI document.

Completed: `MapViewerGridOverlayLayoutBuilder`, `GridReferenceFormatter` and `measureMapDrag` are authoritative for both viewers and thumbnails. The shared screen-space contract
owns
line placement, fades, sticky labels, major/fine switching, live references and measurements; KorGE and Compose retain only text measurement, coordinate conversion and drawing.

Authoritative layer controls and regeneration are a future feature, not current migration work. Do not start them incidentally while completing ordinary authoring, persistence,
grid parity or operational-rule extraction.

Phase 1 is complete: neutral transform fixtures cover every shape/group/modifier combination; restored editors are functional; both viewers consume the same grid layout and
reference/measurement services; no Compose production code references KorGE types, and save codecs remain operation-native.

### Phase 2 — Complete ordinary operation authoring

Completed: Compose has an operation-native unit-type palette, typed template creation/editing, guarded deletion, click-then-map placement, and ordinary unit editing for template,
grid-aware position, name, facing, destroyed state, affiliation and custom properties. `OperationUnitAuthoring` owns validation, stable instance naming, placement sizing and
change construction; do not recreate these rules in Compose. Drag placement, bulk catalogue import, template reordering and campaign-specific unit panels remain intentionally
deferred.
Completed: Compose has an operation-native graphic palette, typed template import/create/editing, guarded deletion, click-then-map placement, and ordinary graphic editing for type,
display label, grid-aware position and custom properties. `OperationGraphicAuthoring` owns validation, aspect-derived default sizing, placement, retargeting and shared-size
propagation. Typed dimensions round-trip through legacy `normalizedWidth`/`normalizedHeight` template properties at archive and KorGE boundaries. Drag placement, bulk import and
template reordering remain deferred.
Completed: Compose notes use neutral timeline-backed create/edit/delete/reorder actions. New and edited note drafts remain workspace-only until Save; references can target every
map object kind through a cancellable map-pick mode, retain order and missing IDs, and round-trip through legacy KorGE note dialogs. Pre-schema-6 scratchpad text is intentionally
discarded rather than converted into a ledger entry.
Completed: Draw creation now uses `MapViewerDrawingGesture` in both viewers, including padded-background start/clamping, physical Shift constraints, freeform spacing and no-op
suppression. Compose owns Draw/Measure pointer streams through a cancellable captured session, renders constant-screen-width drawing strokes and EF Nord drawing labels, and exposes
legacy-compatible optional name editing through the shared `displayName` custom property. Measurements remain transient, map-clamped, grid-aware and available during history.

Completed: Compose now presents the ledger as expandable turn groups with neutral canonical action descriptions, generated turn thumbnails, affected-editor links and explicit
future/current states. New operations begin with `Operation Start`; adding a turn creates one marker entry. Selecting a turn moves the applied head and remains read-only until
Current reapplies the retained branch, while selecting an individual action remains a non-mutating history preview. Applied saves omit redo-only thumbnail assets.

Phase 2 is complete: a user can create and maintain templates, units, graphics, drawings, notes and turns entirely in Compose. All Compose authoring records through the neutral
timeline-entry factory, so batches, descriptions and affected targets retain their meaning across archive and KorGE boundaries. Representative Compose→KorGE→Compose fixtures
must continue retaining content, IDs, images, applied head, turn thumbnails and descriptions.

### Phase 3 — Extract and port operational rules

Move rules out of `korge/interaction` before building their Compose UI. Use neutral `OperationUnit`/content queries rather than adapting new code to `UnitSnapshot`.

1. Extract unit query, Olympiad Dawn definitions/damage/fighter state and shared report formatting.
2. Extract sensor visibility, contact/Sierra lifecycle, ECM geometry and SITREP/SCIREP/NAVREP generation, preserving the contracts documented under **SITREP Contacts** below.
3. Extract fire arcs, tactical targeting, ammunition/fire-control usage, combat resolution and damage application.
4. Build Compose SITREP/SCIREP, GUNREP, tactical and Olympiad Dawn editors/reports on those neutral services, including map overlays and persisted per-turn state.

Phase 3 gate: existing sensor, tactical and Olympiad Dawn test cases run outside `korge` against neutral models, and the corresponding Compose workflows produce equivalent
persisted
results from the same operation fixture.

### Phase 4 — Finish the Compose workspace and desktop product

1. Expand the typed workspace beyond its current right-dock prototype: add left/bottom regions, tab reordering and cross-region moves, stable keyboard/focus activation,
   editor-specific
   validation state and the remaining editor bodies. Preserve the implemented floating drag/resize/minimize/pin/Z-order/map-anchor behavior.
2. Finish native desktop behavior: menus and shortcuts, focus traversal, context menus, confirmations, snackbars/errors, native AWT file dialogs, app icons/licenses/resources and
   robust loading/progress/cancellation states. Keep one Compose rendering surface and use Material/Compose multiline text controls.
3. Make save/exit orchestration independently testable by injecting the saver as well as the existing autosave clock. Cover failed autosave, retry, mutation during save, forced
   exit save and cancelled exit without requiring a live window.
4. Harden rendering with visible-tile/object culling, cache lifetime checks, large-map limits, DPI/window resizing and hardware/software Skia verification. Use EF Nord consistently
   without replacing accessible control semantics.

Phase 4 gate: workspace state restores exactly, multiline/focus/clipboard/IME behavior has Compose UI coverage, native close cannot lose changes, and the portable Windows image
passes an extract-and-run smoke test.

### Phase 5 — Prove parity and change the default

Build explicit evidence rather than relying on visual inspection:

- Version-1/version-2 and unknown-field persistence fixtures, undone-head saves, generated assets and Compose→KorGE→Compose round trips.
- Tolerant renderer snapshots at minimum/default/maximum zoom plus pure assertions for layer order, hit targets, sticky grid labels, pan/zoom anchoring and large-map tiling.
- Compose UI tests for launchpad, new/open, tools, every editor/report, multiline input, workspace switching and save-failure exit gating.
- `testAll`, dependent `:render:test`, portable ZIP smoke tests and Windows hardware/software-rendering checks.

Only after these gates pass: make Compose the default for one release while retaining explicit `--viewer=korge` fallback and conversion compatibility.

### Phase 6 — Remove KorGE in a dedicated release slice

After the fallback release, move any final shared rules out of `korge`, delete the KorGE scene/views/models/dependencies and `LegacyOperationAdapter`, remove the backend selector
and
service seam, simplify `MapViewerFrame`, and split/rename modules only where the resulting dependency graph warrants it. Do not mix this cleanup with feature porting.

- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/MapViewerFrame.kt` is the public viewer entry point for generated map images and should not depend on `render/`.
- `MapViewerFrame` is also the backend compatibility facade. The Compose implementation is loaded from `command-gui-compose` through
  `MapViewerBackendProvider`; keep Compose/Skia types out of the facade and shared save models.
- `command-gui-compose` owns the modern Compose Desktop window, Material controls and tiled Canvas renderer. It must remain a single Compose
  rendering surface; AWT interop is reserved for native operating-system dialogs.
- `command-gui/src/main/kotlin/io/hsar/mapgenerator/commandgui/korge/scene/MapViewerScene.kt` owns KorGE window setup, camera world wiring and scene-level composition.
- KorGE is the compatibility backend while workflows move to Compose. New reusable behavior belongs in engine-neutral packages rather than
  under `korge/`.
- Grid layout, grid-reference formatting and measurement calculations are neutral top-level services. Keep KorGE grid code limited to projecting shared screen-space primitives into
  its world-attached line layer and screen-attached label/arrow layer; do not recreate layout rules below `korge`.
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
