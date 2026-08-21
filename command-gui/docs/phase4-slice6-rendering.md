# Phase 4 Slice 6 rendering verification

Keep captures and notes under the ignored `build/reports/parity/slice6/` directory. Do not commit generated evidence.

## Backends

Build the portable application with `./gradlew :render:portableZip`. Extract it to a path containing spaces and non-ASCII characters, then launch the executable twice from PowerShell:

```powershell
$env:SKIKO_RENDER_API = "DIRECT3D"
.\TerrainMapGenerator.exe

$env:SKIKO_RENDER_API = "SOFTWARE"
.\TerrainMapGenerator.exe
```

Confirm the About diagnostics report the requested renderer and the 512M GPU cache default. An explicit `-Dskiko.gpu.resourceCacheLimit` must remain visible and must not be replaced by the launcher default.

## Matrix

Use small (1×1), normal (1920×1080) and large (at least 8192×4096) map fixtures. For each renderer, exercise 1280×720, 1920×1080 and a 4K physical viewport at Windows scaling of 100%, 125% and 150%. Where two monitors are available, drag the live window between monitors with different scaling.

At each size verify zoom 0.5×, 1× and 4×; cursor-anchored wheel zoom; right-drag pan to all world edges; dock open/collapse/resize; floating editor movement; rapid pan reversal; and replacement with a different operation while tiles are pending.

Record that:

- map tiles have no gaps or seams after loading and obsolete tiles never flash after pan or replacement;
- the camera centre and saved zoom survive dock, window and DPI changes;
- map, tactical, ECM, drawing, preview, graphic, grid, fire-arc, unit, label and selection layers retain their order;
- off-screen objects become visible at the correct edge and remain selectable/context-targetable wherever their full geometry is hit-testable;
- software rendering remains usable and produces the same geometry as Direct3D.

The neutral metadata tests cover the 16,384-pixel axis and 100-million-pixel limits without allocating boundary-sized rasters. Do not create a 400 MB image merely to repeat those assertions manually.
