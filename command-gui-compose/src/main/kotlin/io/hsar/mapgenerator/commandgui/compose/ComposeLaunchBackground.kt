package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.hsar.mapgenerator.commandgui.MapViewerLaunchBackgroundBatchGenerator
import io.hsar.mapgenerator.commandgui.MapViewerLaunchBackgroundLayout
import io.hsar.mapgenerator.commandgui.MapViewerLaunchBackgroundSpec
import io.hsar.mapgenerator.commandgui.MapViewerLaunchTileCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLaunchTileQuality
import io.hsar.mapgenerator.commandgui.MapViewerLaunchTileReveal
import io.hsar.mapgenerator.commandgui.MapViewerLaunchTileUpdate
import io.hsar.mapgenerator.commandgui.MapViewerLaunchpadTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import kotlin.math.roundToInt

internal val LocalComposeLaunchBackgroundState = staticCompositionLocalOf<ComposeLaunchBackgroundState> {
    error("Compose launch background state is unavailable")
}

internal data class ComposeLaunchTileVisual(
    val coordinate: MapViewerLaunchTileCoordinate,
    val preview: ImageBitmap? = null,
    val previewArrivedAtNanos: Long? = null,
    val refinement: ImageBitmap? = null,
    val refinementArrivedAtNanos: Long? = null,
    val previewAlphaAtRefinement: Float = 0f,
)

internal class ComposeLaunchBackgroundState(
    initialSeed: Long,
    private val scope: CoroutineScope,
    private val generator: MapViewerLaunchBackgroundBatchGenerator = MapViewerLaunchBackgroundBatchGenerator(),
) {
    var seed by mutableLongStateOf(initialSeed)
        private set
    var spec by mutableStateOf<MapViewerLaunchBackgroundSpec?>(null)
        private set
    val tiles = mutableStateMapOf<MapViewerLaunchTileCoordinate, ComposeLaunchTileVisual>()
    var revision by mutableIntStateOf(0)
        private set
    private var generationStarted = false
    private var generationJob: Job? = null
    private var generation = 0
    private var viewportWidth = 0
    private var viewportHeight = 0

    val biome: String
        get() = spec?.biome ?: MapViewerLaunchBackgroundSpec.biome(seed)

    fun ensureStarted(viewportWidth: Int, viewportHeight: Int) {
        if (generationStarted) return
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        generationStarted = true
        val activeGeneration = generation
        val generatedSpec = MapViewerLaunchBackgroundSpec.forViewport(viewportWidth, viewportHeight, seed)
        spec = generatedSpec
        generationJob = scope.launch {
            generator.generate(
                spec = generatedSpec,
                onTile = { update -> if (activeGeneration == generation) accept(update) },
                onFailure = { job, failure, _ ->
                    if (activeGeneration == generation) {
                        LogManager.getLogger(ComposeLaunchBackgroundState::class.java)
                            .warn("Could not render launch background ${job.quality.name.lowercase()} tile ${job.coordinate}.", failure)
                    }
                },
            )
        }
    }

    fun regenerate(newSeed: Long) {
        generation++
        generationJob?.cancel()
        generationJob = null
        tiles.clear()
        revision++
        seed = newSeed
        spec = null
        generationStarted = false
        if (viewportWidth > 0 && viewportHeight > 0) ensureStarted(viewportWidth, viewportHeight)
    }

    fun isAnimating(nowNanos: Long): Boolean = tiles.values.any { tile ->
        tile.refinementArrivedAtNanos?.let { arrived ->
            elapsedMilliseconds(arrived, nowNanos) < MapViewerLaunchTileReveal.REFINEMENT_FADE_MILLISECONDS
        } ?: tile.previewArrivedAtNanos?.let { arrived ->
            elapsedMilliseconds(arrived, nowNanos) < MapViewerLaunchTileReveal.PREVIEW_FADE_MILLISECONDS
        } ?: false
    }

    private suspend fun accept(update: MapViewerLaunchTileUpdate) {
        val bitmap = withContext(Dispatchers.Default) { update.image.toComposeImageBitmap() }
        val arrivedAt = System.nanoTime()
        val current = tiles[update.coordinate] ?: ComposeLaunchTileVisual(update.coordinate)
        tiles[update.coordinate] = when (update.quality) {
            MapViewerLaunchTileQuality.PREVIEW -> if (current.refinement == null) {
                current.copy(preview = bitmap, previewArrivedAtNanos = arrivedAt)
            } else current
            MapViewerLaunchTileQuality.REFINEMENT -> current.copy(
                refinement = bitmap,
                refinementArrivedAtNanos = arrivedAt,
                previewAlphaAtRefinement = current.previewArrivedAtNanos?.let { previewArrival ->
                    MapViewerLaunchTileReveal.previewAlpha(elapsedMilliseconds(previewArrival, arrivedAt)).toFloat()
                } ?: 0f,
            )
        }
        revision++
    }
}

@Composable
private fun ComposeLaunchBackgroundButton(
    state: ComposeLaunchBackgroundState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = CommandButton(onClick = onClick, modifier = modifier) {
    Text(state.biome, fontSize = MapViewerLaunchpadTokens.BODY_FONT_SIZE.sp)
}

@Composable
internal fun ComposeLaunchBackgroundPanel(
    state: ComposeLaunchBackgroundState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRegenerate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!expanded) {
        ComposeLaunchBackgroundButton(
            state = state,
            onClick = onToggle,
            modifier = modifier.width(220.dp).height(42.dp),
        )
        return
    }
    Surface(
        modifier = modifier.width(360.dp).height(330.dp),
        shape = RectangleShape,
        color = CommandColours.Dark.copy(alpha = MapViewerLaunchpadTokens.PANEL_ALPHA / 255f),
        contentColor = CommandColours.White,
        border = BorderStroke(CommandDesignTokens.borderWidth, CommandColours.White),
    ) {
        Box {
            ComposeLaunchBackgroundButton(
                state = state,
                onClick = onToggle,
                modifier = Modifier.width(220.dp).height(42.dp),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 24.dp, top = 58.dp, end = 24.dp, bottom = 24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    state.spec?.information.orEmpty().forEach { (label, value) ->
                        Text(
                            "$label: $value",
                            color = CommandColours.White,
                            fontSize = MapViewerLaunchpadTokens.BODY_FONT_SIZE.sp,
                            lineHeight = 28.sp,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommandButton(
                        onClick = onRegenerate,
                        modifier = Modifier.weight(1f),
                    ) { Text("REGENERATE") }
                    CommandButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("CLOSE") }
                }
            }
        }
    }
}

@Composable
internal fun ComposeLaunchBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = BoxWithConstraints(modifier.background(Color.Black)) {
    if (!enabled) return@BoxWithConstraints
    val viewportWidth = launchBackgroundWorldPixels(maxWidth.value)
    val viewportHeight = launchBackgroundWorldPixels(maxHeight.value)
    val state = LocalComposeLaunchBackgroundState.current
    LaunchedEffect(state, viewportWidth, viewportHeight) {
        state.ensureStarted(viewportWidth, viewportHeight)
    }
    var frameNanos by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(state.revision) {
        do {
            withFrameNanos { frameNanos = System.nanoTime() }
        } while (state.isAnimating(frameNanos))
    }
    val spec = state.spec
    if (spec != null) {
        val tiles = state.tiles.values.sortedWith(compareBy({ it.coordinate.row }, { it.coordinate.column }))
        Canvas(Modifier.fillMaxSize()) {
            val placement = MapViewerLaunchBackgroundLayout.cover(
                spec.textureWidth,
                spec.textureHeight,
                size.width.roundToInt().coerceAtLeast(1),
                size.height.roundToInt().coerceAtLeast(1),
            )
            withTransform({
                translate(placement.left.toFloat(), placement.top.toFloat())
                scale(placement.scale.toFloat(), placement.scale.toFloat(), Offset.Zero)
            }) {
                tiles.forEach { tile -> drawTile(tile, frameNanos) }
            }
        }
    }
    Box(
        Modifier.fillMaxSize().background(
            Color.Black.copy(alpha = MapViewerLaunchpadTokens.BACKGROUND_WASH_ALPHA / 255f),
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTile(tile: ComposeLaunchTileVisual, nowNanos: Long) {
    val coordinate = tile.coordinate
    clipRect(
        left = coordinate.left.toFloat(),
        top = coordinate.top.toFloat(),
        right = (coordinate.left + MapViewerLaunchTileCoordinate.TILE_CLIP_SIZE).toFloat(),
        bottom = (coordinate.top + MapViewerLaunchTileCoordinate.TILE_CLIP_SIZE).toFloat(),
    ) {
        val destination = IntOffset(
            coordinate.left - MapViewerLaunchTileCoordinate.TILE_BLEED,
            coordinate.top - MapViewerLaunchTileCoordinate.TILE_BLEED,
        )
        val destinationSize = IntSize(
            MapViewerLaunchTileCoordinate.PADDED_TILE_SIZE,
            MapViewerLaunchTileCoordinate.PADDED_TILE_SIZE,
        )
        val refinementAlpha = tile.refinementArrivedAtNanos?.let { arrival ->
            MapViewerLaunchTileReveal.refinementAlpha(elapsedMilliseconds(arrival, nowNanos)).toFloat()
        } ?: 0f
        val previewAlpha = tile.refinementArrivedAtNanos?.let {
            tile.previewAlphaAtRefinement * (1f - refinementAlpha)
        } ?: tile.previewArrivedAtNanos?.let { arrival ->
            MapViewerLaunchTileReveal.previewAlpha(elapsedMilliseconds(arrival, nowNanos)).toFloat()
        } ?: 0f
        tile.preview?.let { preview ->
            drawImage(preview, dstOffset = destination, dstSize = destinationSize, alpha = previewAlpha, filterQuality = FilterQuality.None)
        }
        tile.refinement?.let { refinement ->
            drawImage(refinement, dstOffset = destination, dstSize = destinationSize, alpha = refinementAlpha, filterQuality = FilterQuality.High)
        }
    }
}

private fun elapsedMilliseconds(startedAtNanos: Long, nowNanos: Long) =
    (nowNanos - startedAtNanos).coerceAtLeast(0L) / 1_000_000.0

internal fun launchBackgroundWorldPixels(logicalDp: Float) = logicalDp.roundToInt().coerceAtLeast(1)
