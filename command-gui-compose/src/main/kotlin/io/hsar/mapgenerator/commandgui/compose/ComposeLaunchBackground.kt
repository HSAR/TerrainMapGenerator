package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.hsar.mapgenerator.commandgui.MapViewerLaunchBackground
import io.hsar.mapgenerator.commandgui.MapViewerLaunchBackgroundRenderer
import io.hsar.mapgenerator.commandgui.MapViewerLaunchpadTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal val LocalComposeLaunchBackgroundSeed = staticCompositionLocalOf { 0L }

@Composable
internal fun ComposeLaunchBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = BoxWithConstraints(modifier.background(Color.Black)) {
    if (!enabled) return@BoxWithConstraints
    val viewportWidth = maxWidth.value.roundToInt().coerceAtLeast(1)
    val viewportHeight = maxHeight.value.roundToInt().coerceAtLeast(1)
    val seed = LocalComposeLaunchBackgroundSeed.current
    var background by remember(seed) { mutableStateOf<MapViewerLaunchBackground?>(null) }
    LaunchedEffect(seed) {
        background = withContext(Dispatchers.Default) {
            MapViewerLaunchBackgroundRenderer.render(viewportWidth, viewportHeight, seed)
        }
    }
    background?.let { generated ->
        val bitmap = remember(generated.image) { generated.image.toComposeImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
    }
    Box(
        Modifier.fillMaxSize().background(
            Color.Black.copy(alpha = MapViewerLaunchpadTokens.BACKGROUND_WASH_ALPHA / 255f),
        ),
    )
}
