# Command GUI Agent Notes

## Current status

The latest completed migration work is **Phase 4, Slice 3 (focus, dialogs, feedback and look-and-feel parity)**. Treat Phase 4 Slice 4 and later as planned work even where scaffolding exists.

Use a strangler migration: locate tested KorGE behaviour, move reusable rules to neutral types, add renderer-independent fixtures, connect Compose through thin adapters, and remove compatibility code only after every consumer has crossed the boundary. KorGE is the compatibility backend, not the destination for new product UI.

## Architecture and ownership

- `MapViewerFrame` is the public viewer facade. KorGE remains the default compatibility backend; Compose Desktop loads from `command-gui-compose` through `MapViewerBackendProvider`. Keep `--viewer=korge|compose` through the Phase 5 fallback release.
- Engine-neutral state and rules belong in `commandgui` or `commandgui.operation`; KorGE/korlibs types stay below `korge`; Compose/Skia types stay in `command-gui-compose`. Save codecs import neither renderer.
- `OperationCore.kt` owns operation content. `OperationTimelineAuthoring.kt` owns timeline construction, turn grouping, descriptions and affected targets. A persisted batch remains one top-level entry, but do not collapse its sequential `componentChangeSets`.
- Neutral transforms, selection geometry, tool gestures, grid layout/references, measurements, command resolution, viewer-chrome geometry, menu geometry and fire-arc primitives are authoritative. Renderer code converts coordinates, measures text, paints and adapts input.
- `ComposeOperationState` owns document/head replay, camera, tool, selection, previews, save revisions, workflows and `OperationWorkspaceState`. Composables retain only ephemeral pointer, focus or popup state. Editor identity, drafts, bounds, dock/floating placement and validation inputs remain neutral and persistable.
- `MapViewerSaveRepository.loadOperation/createOperation/saveOperation` is the archive boundary. Preserve unknown `additionalData.json` fields additively. Disabling `OLYMPIAD_DAWN_EXPERIMENTAL` hides entry points without deleting saved payloads.
- `enabledLayerIds` remains persistence-only until a separately approved layer slice. Compose paints the supplied base map and grid directly.

## Protected behaviour through Phase 4 Slice 3

### Map, authoring and operational rules

- Both launch paths enter the same Compose operation state. The tiled map restores camera state and supports cursor-anchored zoom, right-drag pan, shared grid layout, zoom-independent unit icons and neutral selection/transform gestures.
- Units, graphics and all persisted drawings support click/Shift/marquee selection, group frames, move/resize/rotate/delete and immutable one-entry commits. Draw and transient Measure share neutral constraints, clamping and grid-aware results.
- The Game Turns editor is a compact KorGE-style navigator: equal Undo/Redo controls, an Add Turn/name row, a scrolling list of fixed-height turn buttons with thumbnail/name/four-digit hexadecimal index, and a fixed Current button at the bottom. Turn selection preserves revert/branch-truncation semantics; do not reintroduce expanded action-detail rows into this narrow panel. Save and save status remain available on the Game Settings tab.
- Compose can create/edit/delete unit and graphic types, place instances, edit ordinary instance fields, maintain timeline-backed notes/references and use flag-gated Olympiad Dawn status, report and tactical workflows. Graphic dimensions remain template-owned.
- Sensor, ECM, targeting, ammunition, damage and fire-arc rules remain neutral. SCIREP and NAVREP are distinct context commands. Persisted Olympiad Dawn fighter IDs remain random four-character `[A-NP-Z0-9_]` tokens.

### Workspace and persistence

- New operations start with an empty workspace. Stable menu-editor IDs are `game`, `operation-notes`, `graphic-types` and `unit-types`; summon Game/Notes to the left and Graphics/Units to the right.
- `OperationWorkspaceState` restores left/right/bottom docks, size/collapse, tab order/active tab, cross-region movement, keyboard cycling and floating bounds/minimize/pin/Z-order/map anchor. Typed drafts survive moves; opaque records remain losslessly unsupported.
- UI schema 7 round-trips the four menu panels, Game tab, Units collapsed affiliations, ordinary/operational editor drafts, bounds, docking and floating state. Legacy KorGE `ledger`, `notes`, `graphics` and `unit-types` records restore as functional editors and continue to be emitted. Preserve unknown draft fields and unrelated additional data.
- Global display preferences remain in existing `settings.json` without a format bump. `MapViewerDisplaySettings` is the aggregate boundary for destroyed-unit mode, icon size and public fire-arc mode.

### Viewer chrome contract

- Viewer screens have no Compose/AWT desktop `MenuBar`, permanent bottom ledger, always-visible palette or `MENU` button. Launch screens have no menu surface.
- Four floating buttons are exactly 120×40 logical pixels: Game `(16,16)`, Notes 12 px to its right, Units 16 px from the right, and Graphics 12 px left of Units. `MapViewerChromeLayoutBuilder` is authoritative.
- Keep Select/Draw/Measure centred at the top. If its bounds intersect either button group, move it to y=68 (16 px padding + 40 px row + 12 px gap).
- Clicking a closed menu editor opens and activates it in its legacy-side dock; clicking any already-open instance closes it even when inactive. Restored editors open where persisted. Placement initiated from Units or Graphics does not close its editor.
- Game contains Turns and Settings. Workspace Mode updates the persisted workspace immediately; Destroyed Units, Unit Icon Size and flag-gated Fire Arcs write through `MapViewerGlobalSettings`. Notes reuses the notes browser; Units and Graphics host their full authoring/placement bodies.
- Escape contains Return to Launchpad, Exit and Close only. Save remains in Game and Ctrl+S. All exit requests continue through save gating.

### Commands, shortcuts and context menus

- `MapViewerCommand`, `MapViewerCommandItem`, `MapViewerCommandContext` and `MapViewerCommandResolver` are the neutral command boundary. Presentation carries label, enabled state, disabled reason, destructive classification and children; it never stores renderer callbacks.
- Floating buttons, Game controls, Escape actions, context rows and global shortcuts dispatch typed commands against current Compose state. Do not reintroduce flat `MapViewerContextMenuCommand`, string-prefix deletion detection or captured document snapshots. KorGE may retain its callback compatibility adapter until removal.
- Global shortcuts run in bubble phase. Focused text controls own Ctrl+A/C/X/V/Z/Y and Delete; Ctrl+S remains a document command. Outside text editing, Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z, Delete and dock Ctrl+Tab keep document/workspace meanings. Activate on key-down, suppress non-repeatable repeats, and dispatch one event once.
- Escape unwinds one state only: context-menu chain; highest active preview/gesture/placement/reference workflow; open Escape menu; otherwise open Escape menu.
- Secondary-click cancellation of drawing, placement and reference workflows takes priority over menu opening. Empty-map secondary click dismisses the menu and preserves selection.
- Right-clicking an unselected object selects it. Right-clicking a selected member of a multi-selection preserves the group and exposes only immediate Delete Selected. Single-object rows are:
  - Unit: Edit Unit, Delete Unit, separate SCIREP and NAVREP, feature-gated Tactical and Status, then referenced Notes submenu.
  - Graphic: Edit Graphic, Delete Graphic, then referenced Notes submenu.
  - Drawing: Edit Name, Delete Drawing, then referenced Notes submenu.
- History preview permits safe inspection editors but disables content mutation, state-changing reports, firing and deletion with an explicit reason. Object and group deletion remain immediate; dependent template deletion remains blocked.

### Custom menu-chain implementation

- Context menus live in one Compose host at viewer-root coordinates and consume pointer input across the active chain; never create an AWT popup over the map.
- `MapViewerMenuTokens` is authoritative: 44 px rows, 4 px padding, 180 px minimum width, 2 px border, 8 px submenu corridor and 180 ms close delay. Keep EF Nord and shared command colours for normal, hover/focus, disabled, destructive/warning, separator and arrow states.
- `MapViewerMenuLayoutBuilder` clamps roots to the viewer, aligns children to source rows, prefers right, flips left and vertically clamps tall menus. Recompute from current viewer dimensions after resize, DPI or dock changes.
- Pointer movement into the safe corridor must retain the submenu during the close delay. Keyboard operation covers Up/Down/Home/End, Right/Left, Enter/Space and Escape. Dismiss the full chain on outside action and restore focus to the invoking control or map.
- Disabled reasons must be presented through semantics/text, not colour alone. Do not permit pointer events to pass through the active menu chain to selection or map gestures.

### Display settings and overlays

- Full destroyed mode retains normal rendering, hit testing, menus and transforms.
- Partial renders icon and destruction cross at 50%, permits selection/context menus, and blocks transforms. A group containing any non-movable destroyed unit cannot begin a group transform.
- Hide omits destroyed units from rendering/hit testing, prunes their selection, and closes invalid object menus/editors without changing document content.
- `OperationFireArcOverlayBuilder` returns renderer-independent circle, sector-arc and radial primitives from shared weapons, facing and range bands. Compose and KorGE consume the same primitives. Compose uses constant screen-space strokes.
- Fire-arc filtering remains All Units, Friendlies of Selected, Hostiles of Selected, Selected Only or None. Hide both the setting and overlays when the feature flag is disabled.

### Focus, modal and feedback contract

- When an operation opens with a persisted active editor, focus that editor; otherwise focus the map. Activating a dock tab or floating editor moves focus to it. Dismissing a menu or modal restores focus to its invoking control, falling back to the map when the invoker is no longer available.
- Editable text retains clipboard, selection, undo/redo, multiline and IME ownership. Do not remount a typed editor draft merely to change validation or focus state, and do not let viewer shortcuts observe text-owned events.
- All Compose modal presentation goes through one typed `ComposeModalHostState`. Requests have stable keys, duplicate queued requests collapse, loss-prevention requests may pre-empt an ordinary user modal, and otherwise the queue is FIFO. Each modal defines an explicit default and cancel action; cancellation must complete its owning workflow exactly once.
- Slice 3 adds no confirmation to object, group, note, drawing, unit-type or graphic-type deletion. Those actions remain immediate unless a later independently approved confirmation contract says otherwise.
- Transient feedback uses `ComposeTransientMessageHostState`: at most five lower-left notices, ten-second display, 420 ms fade, and no map-input interception outside the painted notice bounds. Save success/failure and other non-blocking feedback belong here rather than in ad hoc overlays.
- `ComposeFilePicker` is the sole Compose file-selection boundary. Keep its AWT `FileDialog` parented to the Compose window, suspend conflicting viewer shortcuts while it is open, and decode/read selected content off the UI thread. Treat cancel, unreadable and unsupported selections as distinct typed results. Slice 3 intentionally imposes no file-size limit; loading limits belong to Slice 5.

### Presentation resources and parity evidence

- `CommandDesignTokens` and neutral menu/chrome tokens are authoritative for button sizes, padding, gaps, borders, row geometry, colours and interaction states. New editor/menu presentation must use those tokens rather than introducing visually equivalent local constants.
- Bundle both TV Nord EF font resources and inspect them at startup. If either cannot load, log the diagnostic once and use a readable sans-serif fallback; the application must remain usable. Keep the font attribution and the authorization assumption visible in third-party notices.
- The launchpad exposes a compact bottom-left version button. It opens About through the shared modal host and shows build version, runtime/font diagnostics, project licence and third-party notices, with copyable diagnostics.
- Required build metadata, project licence, third-party notices and 64/128/256/512 application icons are packaging-tested. Missing required resources fail tests; optional presentation failures degrade with an explicit diagnostic.
- Slice 3 parity evidence is manual and measurement-led rather than checked-in golden PNGs. Follow `command-gui/docs/phase4-slice3-parity.md`; keep temporary captures under ignored `build/reports/parity`, verify the stated window/DPI matrix, and distinguish deliberate Compose differences from known KorGE defects.

## Completed migration summary

- **Phases 1–3:** neutral document editing, transforms/selection, authoring, persistence, sensors/reports/ECM and Olympiad Dawn tactical rules moved behind renderer-independent APIs; Compose gained equivalent authoring and workflow bodies.
- **Phase 4 Slice 1:** typed dock/floating workspace shell, restoration, validation and navigation completed.
- **Phase 4 Slice 2:** viewer floating menus, typed menu editors, schema-7 migration, unified command/context dispatch, bubble-phase shortcuts, Escape precedence, custom menu-chain geometry/timing, global destroyed-unit modes and shared fire-arc primitives completed.
- **Phase 4 Slice 3:** shared design tokens and scale fixtures, deterministic editor/map focus, typed priority/FIFO modal and transient-message hosts, parented asynchronous file selection, launchpad About/diagnostics/licences, font fallback diagnostics and manual parity evidence completed.

## Remaining migration plan

### Phase 4 Slice 4 — Save and exit orchestration

This is the immediate next slice. Preserve Slice 3 interaction and presentation behaviour while making every save/exit path lossless under concurrency and failure.

- Extract a testable save coordinator with injected saver, debounce clock and completion dispatcher. At most one save is in flight; mutations during save stay dirty; stale completion cannot mark a newer revision saved; requests coalesce; autosave recovers after failure.
- Route window close, Escape Exit and Return to Launchpad through one request path. Clean exits proceed; dirty exits save; in-flight exits wait; failures offer Retry and Cancel Exit without losing the operation.

### Phase 4 Slice 5 — Loading, packaging and desktop polish

- Give create/open/render/decode/archive operations explicit idle/running/succeeded/failed/cancelled state. Run expensive work off the UI thread, make cancellation cooperative and ignore stale completions.
- Define image/archive limits before allocation and present actionable failures. Build a portable Windows distribution with icons, fonts and licences, then smoke-test create/save/close/reopen from paths containing spaces and non-ASCII characters.

### Phase 4 Slice 6 — Rendering hardening

- Add conservative visible-tile/object culling, bounded caches and obsolete-work cancellation without changing layer order or hit testing.
- Verify supported map dimensions, minimum/default/maximum zoom, cursor anchoring, dock/window resize and DPI/monitor transitions on hardware and software Skia paths.

Phase 4 is complete only when workspace restoration is exact; menus, focus, multiline editing, clipboard and IME have Compose UI coverage; every close path is lossless under failure/concurrency; loading cancels safely; and the portable Windows image passes its smoke matrix.

### Phase 5 — Prove parity and change the default

- Protect schemas 1–7, unknown fields, undone heads, generated assets and Compose→KorGE→Compose round trips.
- Add renderer snapshots across zoom/DPI and Compose UI journeys for every launch path, tool, editor/report, menu/shortcut/context action, workspace restore and failed-save exit.
- Run `:command-gui:test`, `:command-gui-compose:test`, dependent `:render:test`, `testAll`, portable ZIP smoke tests and Windows hardware/software/DPI checks.
- Then make Compose default for one release while retaining explicit `--viewer=korge` fallback and conversion compatibility.

### Phase 6 — Remove KorGE

After the fallback release, move final shared rules out of `korge`, delete KorGE views/models/dependencies and `LegacyOperationAdapter`, remove the backend selector/service seam, simplify `MapViewerFrame`, and split modules only where the resulting dependency graph warrants it. Do not mix cleanup with feature porting.

## Compatibility details that must survive

- Keep the JVM-specific KorGE 5.4 dependency until removal. Premultiply every bitmap after load, trim or composition before uploading it to KorGE.
- Keep a session's render seed stable. Terrain layers sample stable world coordinates derived from `metresPerPixel`; repeated layer/image inputs remain aligned.
- KorGE dialog scrolling is transform-only: move clipped content and thumb, rebuilding only when content/dimensions/state changes.
- KorGE launch layout uses `MapViewerViewportResolver.actualVirtualWidth/actualVirtualHeight`, with window dimensions only as zero-value startup fallback. Each generation owns its clipped canvas; advance generation before cancelling old work.
- Sensor contacts remain additive `sitrepContacts` records keyed by turn and observer. Detection/loss/re-identification, Sierra numbering, retained last-seen data, 3U low-confidence inheritance, wording and supported class labels must preserve existing neutral tests.
