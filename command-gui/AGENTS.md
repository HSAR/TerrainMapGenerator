# Command GUI Agent Notes

## Current status

The latest completed migration work is **Phase 4, including its Compose acceptance closure**. Treat Phase 5 and later as planned work even where scaffolding exists.

Use a strangler migration: locate tested KorGE behaviour, move reusable rules to neutral types, add renderer-independent fixtures, connect Compose through thin adapters, and remove compatibility code only after every consumer has crossed the boundary. KorGE is the compatibility backend, not the destination for new product UI.

## Architecture and ownership

- `MapViewerFrame` is the public viewer facade. KorGE remains the default compatibility backend; Compose Desktop loads from `command-gui-compose` through `MapViewerBackendProvider`. Keep `--viewer=korge|compose` through the Phase 5 fallback release.
- Engine-neutral state and rules belong in `commandgui` or `commandgui.operation`; KorGE/korlibs types stay below `korge`; Compose/Skia types stay in `command-gui-compose`. Save codecs import neither renderer.
- `OperationCore.kt` owns operation content. `OperationTimelineAuthoring.kt` owns timeline construction, turn grouping, descriptions and affected targets. A persisted batch remains one top-level entry, but do not collapse its sequential `componentChangeSets`.
- Neutral transforms, selection geometry, tool gestures, grid layout/references, measurements, command resolution, viewer-chrome geometry, menu geometry and fire-arc primitives are authoritative. Renderer code converts coordinates, measures text, paints and adapts input.
- `ComposeOperationState` owns document/head replay, camera, tool, selection, previews, the current document revision, workflows and `OperationWorkspaceState`. `MapViewerSaveCoordinator` owns saved/in-flight revisions, debounce/retry timing, failure state and pending exits. Composables retain only ephemeral pointer, focus or popup state.
- `MapViewerSaveRepository.loadOperation/createOperation/saveOperation` is the archive boundary. Preserve unknown `additionalData.json` fields additively. Disabling `OLYMPIAD_DAWN_EXPERIMENTAL` hides entry points without deleting saved payloads.
- `enabledLayerIds` remains persistence-only until a separately approved layer slice. Compose paints the supplied base map and grid directly.

## Protected behaviour through Phase 4 Slice 6

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
- Game contains Turns and Settings. Workspace Mode updates the persisted workspace immediately; Destroyed Units, Unit Icon Size and flag-gated Fire Arcs write through `MapViewerGlobalSettings`. Notes uses the compact KorGE browser: one 140×40 Create Note control followed by 62 px title/subtitle rows with 10 px gaps; clicking anywhere on a row opens that note. Individual note editors are always 700×720 floating non-modal dialogs, even while the four menu panels use docked workspace mode, and use the universal dialog shell with persisted bounds. Keep their order as Title, collapsible References, Body, then Save/Delete/Cancel, and update `NOTE - <draft title>` live. Units and Graphics host their full authoring/placement bodies.
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
- Compose context menus intentionally retain the selected object name as a 44 px heading above the KorGE-equivalent action list. Action slots are 44 px with inset 40 px white-framed buttons, 12 px left label padding and case-preserving 24 px labels; widths fit their content from a 180 px minimum. Disabled actions use the legacy red boxed warning and hover explanation rather than shrinking the label with inline reason text, while submenus use the shared line chevron and safe corridor.
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
- `Phase4ClosureUiTest` exercises the production dock, tab-overflow, context-menu and note-editor composables. Keep physical dock resize, Ctrl+Tab/Ctrl+Shift+Tab focus, keyboard submenu navigation, multiline Unicode/IME commits and modern `LocalClipboard` copy/paste in that UI-level acceptance boundary.
- All Compose modal presentation goes through one typed `ComposeModalHostState`. Requests have stable keys, duplicate queued requests collapse, loss-prevention requests may pre-empt an ordinary user modal, and otherwise the queue is FIFO. Each modal defines an explicit default and cancel action; cancellation must complete its owning workflow exactly once.
- Slice 3 adds no confirmation to object, group, note, drawing, unit-type or graphic-type deletion. Those actions remain immediate unless a later independently approved confirmation contract says otherwise.
- Transient feedback uses `ComposeTransientMessageHostState`: at most five active lower-left notices, ten-second display and 420 ms fade/lift, with no pointer interception. `MapViewerTransientNoticeTokens` is shared with KorGE: 16 px viewport inset, 8 px stack gap, 12×8 px content padding, 24 px regular Nord text and a borderless black background at alpha 180. Do not reintroduce severity-coloured notice panels. Save success/failure and other non-blocking feedback belong here rather than in ad hoc overlays.
- `ComposeFilePicker` is the sole Compose file-selection boundary. Keep its AWT `FileDialog` parented to the Compose window, suspend conflicting viewer shortcuts while it is open, and decode/read selected content off the UI thread. Treat cancel, unreadable and unsupported selections as distinct typed results, and never collapse cancellation into an unreadable-file failure.

### Presentation resources and parity evidence

- `CommandDesignTokens` and neutral menu/chrome tokens are authoritative for button sizes, padding, gaps, borders, row geometry, colours and interaction states. New editor/menu presentation must use those tokens rather than introducing visually equivalent local constants.
- Compose inventory/navigation rows use `ComposeUniversalListRow`, derived from the compact Game turn and KorGE universal-list geometry. It owns the 2 px frame, selected fill, optional fitted preview, one/two-line ellipsized label, trailing label, optional single centred action and secondary-click actions. Turns use 56 px rows with 80 px previews, Notes and note references use 62 px rows, and unit/graphic types use compact 44 px rows. Keep Notes on the dark command surface. Unit/graphic Edit and immediate Delete remain secondary-click actions; Place is the sole normally visible row action.
- Floating Compose workspace editors use `MapViewerDialogChromeTokens`, matching KorGE non-modal windows: 2 px white shell, dark panel at alpha 220, inset 38 px title bar at alpha 235, 24 px uppercase left-aligned title, and three contiguous 42 px icon controls ordered Minimise, Pin, Close. Minimise uses the legacy up/down chevron, Pin uses the line pin and alpha-72 active fill, and Close uses the cross; do not substitute text labels or general-purpose `CommandButton` chrome.
- Draw Compose title-control frame strokes fully inside their 42 px canvas (2 px strokes centred at logical coordinates 1 and 41). Centring them on 0 and 42 clips half the stroke and makes the controls appear taller than the title separator.
- Every `CommandButton` uses a white focus frame; click focus and active tool/action state must never introduce the blue frame. The tool rail retains its dark selected versus main inactive fill distinction.
- Bundle both TV Nord EF font resources and inspect them at startup. If either cannot load, log the diagnostic once and use a readable sans-serif fallback; the application must remain usable. Keep the font attribution and the authorization assumption visible in third-party notices.
- The persistent launch shell exposes a compact bottom-left version button and top-left biome/background-information control on every launch screen, including New Operation and Ongoing Operations. They retain state across launch-screen navigation and disappear only after an operation opens. The version button opens About through the shared modal host and shows build version, runtime/font diagnostics, project licence and third-party notices, with copyable diagnostics.
- Ordinary Compose content inherits white through the theme's `LocalContentColor`; translucent custom surfaces must not fall back to black. Use explicit colours only for meaningful disabled, status, warning or destructive presentation.
- Compose selection overlays follow KorGE layering: affiliation-coloured unit highlights and stored-colour drawing highlights, then a separate translucent whole-group frame using the common selected-unit affiliation or grid-colour fallback. A single unit exposes no corner brackets or rotation handle. Every multi-selection, including legacy all-unit saves, shows brackets on its whole-group frame; resize hit targets remain disabled unless at least one selected graphic or drawing supports resize.
- The Compose launch shell uses the same seeded topography generator, dark wash and `MapViewerLaunchpadTokens` geometry as KorGE. Preserve the centred 540×78 application header and 420×320 home panel, the 34 px header/panel title, 24 px body/button text, and the three 300×44 buttons at the legacy offsets.
- The second launchpad screen is **Ongoing Operations**, never Saved Operations. `MapViewerOngoingOperationsTokens` is shared by both renderers: preserve the centred 760 px legacy shell, internal Back button, 96 px bordered save rows on a 108 px step, 16:9 inset previews, name/timestamp-only labels, explicit 18 px scrollbar and centred 280×44 Browse For Save File button. Do not restore the generic toolbar, Refresh action, filename or Open-label card chrome.
- Launch topography is a persistent two-quality tile pipeline owned above individual launch screens. `MapViewerLaunchBackgroundBatchGenerator` schedules 360-unit tiles on at most four minimum-priority workers using the shared priority order and arrival jitter. Rendering continues independently of tile publication so the reveal animation does not throttle generation. Each tile first displays its 54 px (8×) preview with nearest-neighbour sampling and a 120 ms reveal, then crossfades to its bleed-aware 432 px refinement at one raster pixel per world unit over 280 ms. Keep the seed, source extent and completed tile cache stable across launch-screen navigation, resize and recomposition.
- The main launchpad keeps KorGE's `(20,20)` 220×42 biome box. Clicking it toggles a non-modal 360×330 information surface anchored to the same top-left point, so it expands only down and right and retains the biome-only heading. The expanded surface shows seed, texture, noise scale and contour count plus Regenerate and Close. Launchpad actions and chrome do not use the blue editor-focus frame, including during click-triggered screen changes. Regeneration advances the generation identity before cancelling old work, clears the tile cache and refreshes the still-open box; stale tile completions must be ignored.
- Size the topography world extent from logical viewport dimensions and apply the 1.5× source scale exactly once. Refinement tiles match KorGE at one raster pixel per world unit and use linear filtering when the world canvas is scaled. Do not multiply the world extent by display density.
- Required build metadata, project licence, third-party notices and 64/128/256/512 application icons are packaging-tested. Missing required resources fail tests; optional presentation failures degrade with an explicit diagnostic.
- Slice 3 parity evidence is manual and measurement-led rather than checked-in golden PNGs. Follow `command-gui/docs/phase4-slice3-parity.md`; keep temporary captures under ignored `build/reports/parity`, verify the stated window/DPI matrix, and distinguish deliberate Compose differences from known KorGE defects.

### Save and exit contract

- `MapViewerSaveCoordinator` is the renderer-neutral authority. Feed it immutable document/camera/UI snapshots and inject asynchronous saving, time and completion dispatch; do not restore Compose-local save callbacks or timers.
- Permit one write at a time. Coalesce newer manual requests, preserve dirty revisions during older writes and ignore duplicate, stale or post-disposal completions. Ordinary autosave uses a two-second trailing debounce and 2/4/8/16/32/60-second capped retry delays; notify only once per failure series.
- Return to Launchpad, Escape Exit and native window close use the same request path. Clean exits proceed immediately; dirty and in-flight exits wait for the newest revision. The latest destination wins. Exit failure pauses retry behind the loss-prevention Retry/Cancel Exit modal; cancellation keeps the viewer dirty and resumes ordinary retry.
- `OperationSaveWriter` stages a complete sibling archive before replacement. Prefer atomic replace; the unsupported-atomic fallback must preserve and restore the previous archive and clean temporary/backup files. Keep archive format 6, UI schema 7 and unknown additional data unchanged.

### Loading and desktop distribution contract

- `MapViewerAsyncWork` is the renderer-neutral state machine for idle/running/succeeded/failed/cancelled work. Request IDs are monotonic; cancelled, superseded and stale completions must not mutate current UI or open an operation. Compose owns only the coroutine job and cancellation signal through `ComposeAsyncWorkController`.
- Run blocking image, archive and render work through interruptible background dispatch. Pass `MapViewerCancellationSignal` through archive streaming, ledger replay, assets and thumbnails, and check it between bounded work units. Back, Cancel, replacement and composable disposal cancel the owning request.
- `MapViewerLoadLimits` is authoritative: 512 MiB archive, 10,000 entries, 256 MiB per archive entry/encoded image, 1 GiB expanded archive, 100:1 compression ratio, 64 MiB JSON, 16,384 px maximum image axis and 100 million decoded pixels. Validate metadata before image allocation and use bounded streaming when entry size is absent or can change.
- Preserve the current valid launch form, catalogue or editor draft after a load failure. Present typed, actionable failures inline; cancellation is not an error. Startup `--map` decoding for Compose belongs to the cancellable launch workflow, while KorGE retains its compatibility preload.
- The portable Windows launcher enters Compose when invoked without arguments and retains explicit CLI commands and `--viewer=korge|compose`. `:render:portableZip` must include the runtime, executable, icon, fonts, project licence and third-party notices. `:render:portableSmoke` loads Compose from the packaged runtime, creates a dirty persisted workspace under extracted/save paths containing spaces and non-ASCII characters, proves a failed exit save keeps the viewer open, retries, closes and reopens the exact Compose workspace.

### Rendering hardening contract

- `MapViewport` remains a physical-pixel camera with zoom clamped to 0.5–4.0. Cursor zoom anchors the source point under the pointer; dock, window and DPI resize preserve centre and zoom while updating physical viewport dimensions.
- Compose map painting uses order-preserving `OperationRenderVisibility` candidates, but selection, marquee, context targeting and transforms always query complete operation content. Preserve the established map/tactical/ECM/drawing/preview/graphic/grid/fire-arc/unit/label/selection layer order.
- Base-map rasters are descriptor-only 1024 px tiles with one source-pixel bleed to prevent filtered seams. Request visible tiles plus one tile of overscan, visible-first and centre-out, through two cancellable workers. Ignore results from obsolete viewports, replaced maps and disposed canvases.
- Retain no more than 256 MiB of inactive RGBA tiles and protect the current active set only up to the 400 MiB Slice 5 decoded-map envelope. The Compose launcher defaults Skiko's GPU resource cache to 512M without overriding an explicit system property. Template textures exist only while referenced by visible paint candidates; unit uploads may be reduced to the shared 128 px display maximum.
- Follow `docs/phase4-slice6-rendering.md` for Direct3D/software, zoom, large-map, resize and DPI verification. Generated evidence belongs under ignored build reports.
- `:render:portableRendererSmoke` serially starts the packaged Direct3D and software renderers, presents 1280×720, 1920×1080 and 3840×2160 windows on every attached monitor, and records requested backend, cache limit, monitor transform and presented-frame count under ignored `render/build/reports/parity/slice6`. Keep the probes serialized: concurrent packaged processes race Gradle's shared problems report on Windows.

## Completed migration summary

- **Phases 1–3:** neutral document editing, transforms/selection, authoring, persistence, sensors/reports/ECM and Olympiad Dawn tactical rules moved behind renderer-independent APIs; Compose gained equivalent authoring and workflow bodies.
- **Phase 4 Slice 1:** typed dock/floating workspace shell, restoration, validation and navigation completed.
- **Phase 4 Slice 2:** viewer floating menus, typed menu editors, schema-7 migration, unified command/context dispatch, bubble-phase shortcuts, Escape precedence, custom menu-chain geometry/timing, global destroyed-unit modes and shared fire-arc primitives completed.
- **Phase 4 Slice 3:** shared design tokens and scale fixtures, deterministic editor/map focus, typed priority/FIFO modal and transient-message hosts, parented asynchronous file selection, launchpad About/diagnostics/licences, font fallback diagnostics and manual parity evidence completed.
- **Phase 4 Slice 4:** renderer-neutral serialized save coordination, coalesced revision snapshots, capped failure recovery, unified loss-protected exit routing and staged atomic archive replacement completed.
- **Phase 4 Slice 5:** explicit cancellable loading state, bounded image/archive decoding, stale-completion rejection, Compose loading presentation, Compose-first portable launcher and Unicode-path packaged smoke coverage completed.
- **Phase 4 Slice 6:** conservative render visibility, lazy map tiles, bounded retained texture caches, obsolete-work cancellation and physical-pixel resize/DPI verification completed.
- **Phase 4 acceptance closure:** production Compose UI coverage for dock movement/resize/cycling/focus, keyboard menus, multiline Unicode/IME text and clipboard ownership; packaged failed-save exit/retry/reopen; and repeatable packaged Direct3D/software multi-monitor frame probes completed.

## Remaining migration plan

Phase 4 is complete. Its closure boundary is exact workspace restoration; production Compose UI coverage for menus, focus, multiline editing, clipboard and IME commits; lossless close routing under failure/concurrency; safe loading cancellation; and the packaged lifecycle/backend smoke tasks above. The 21 August 2026 renderer run exercised two attached monitors at 100% scaling with both backends and all three viewport sizes. Physical 125%/150% configurations remain part of the Phase 5 release matrix when such displays are available; neutral scale fixtures continue to cover 1.0/1.25/1.5 deterministically.

### Phase 5 — Prove parity and change the default

This is the immediate next phase.

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
