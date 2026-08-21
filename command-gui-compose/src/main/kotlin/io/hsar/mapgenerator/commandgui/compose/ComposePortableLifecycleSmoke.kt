package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.hsar.mapgenerator.commandgui.MapViewerExitTarget
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSaveCompletion
import io.hsar.mapgenerator.commandgui.MapViewerSaveCoordinator
import io.hsar.mapgenerator.commandgui.MapViewerSaveExecutor
import io.hsar.mapgenerator.commandgui.MapViewerSaveRepository
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** Packaged smoke entry point loaded reflectively by render, where Compose is a runtime dependency. */
object ComposePortableLifecycleSmoke {
    @JvmStatic
    fun run(root: Path) {
        val repository = MapViewerSaveRepository(root.resolve("saves"))
        val image = BufferedImage(48, 32, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().let { graphics ->
                try {
                    graphics.color = Color(22, 48, 80)
                    graphics.fillRect(0, 0, width, height)
                } finally {
                    graphics.dispose()
                }
            }
        }
        val operationId = UUID.fromString("00000000-0000-0000-0000-000000000515")
        val archive = root.resolve("saved operations 測試").resolve("Opération smoke.zip")
        Files.createDirectories(archive.parent)
        val created = repository.createOperation(
            settings = MapViewerSettings(operationName = "Portable 測試"),
            renderData = MapViewerRenderData(image),
            operationId = operationId,
        ).copy(sourcePath = archive)
        val initial = ComposeOperationState(created)
        val dirty = initial.openNotes()
        var saveAttempts = 0
        val exits = mutableListOf<MapViewerExitTarget>()
        val coordinator = MapViewerSaveCoordinator(
            initialSnapshot = initial.saveSnapshot(),
            saver = MapViewerSaveExecutor { attempt, complete ->
                saveAttempts++
                if (saveAttempts == 1) {
                    complete(MapViewerSaveCompletion.Failure("portable smoke retry"))
                } else {
                    complete(MapViewerSaveCompletion.Success(repository.saveOperation(
                        attempt.snapshot.document,
                        attempt.snapshot.camera,
                        attempt.snapshot.uiState,
                    )))
                }
            },
            onExitReady = exits::add,
        )

        coordinator.observe(dirty.saveSnapshot())
        coordinator.requestExit(MapViewerExitTarget.DESKTOP)
        require(coordinator.state.awaitingExitRetry) { "Failed save did not keep the Compose viewer open" }
        require(exits.isEmpty()) { "Compose exited before the dirty revision was saved" }
        coordinator.retryExit()
        require(exits == listOf(MapViewerExitTarget.DESKTOP)) { "Compose close did not resume after retry" }

        val reopened = repository.loadOperation(archive)
        val restored = ComposeOperationState(reopened)
        require(reopened.operationId == operationId) { "Reopened operation identity did not match" }
        require(reopened.operationName == "Portable 測試") { "Reopened operation name did not match" }
        require(reopened.renderData.baseImage.width == 48 && reopened.renderData.baseImage.height == 32) {
            "Reopened map dimensions did not match"
        }
        require(restored.workspace == dirty.workspace) { "Reopened Compose workspace did not match" }
        require(saveAttempts == 2) { "Portable close retry count did not match" }
    }

    @JvmStatic
    fun runRendererProbe(report: Path) {
        Files.createDirectories(report.toAbsolutePath().normalize().parent)
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Terrain Map Generator renderer smoke",
                state = rememberWindowState(size = DpSize(640.dp, 480.dp)),
            ) {
                val density = LocalDensity.current.density
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(ComposeColor(0xFF102030))
                    drawCircle(ComposeColor(0xFFE6E6E6), radius = size.minDimension / 5f, center = center)
                }
                LaunchedEffect(Unit) {
                    val rows = mutableListOf(
                        "requestedRenderer=${System.getenv("SKIKO_RENDER_API") ?: "DEFAULT"}",
                        "gpuCacheLimit=${System.getProperty("skiko.gpu.resourceCacheLimit") ?: "Skiko default"}",
                        "initialComposeDensity=$density",
                    )
                    var framesPresented = 0
                    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.forEachIndexed { monitor, device ->
                        val bounds = device.defaultConfiguration.bounds
                        listOf(1280 to 720, 1920 to 1080, 3840 to 2160).forEach { (width, height) ->
                            window.setLocation(bounds.x + 16, bounds.y + 16)
                            window.setSize(width, height)
                            withFrameNanos { }
                            withFrameNanos { }
                            framesPresented += 2
                            val configuration = window.graphicsConfiguration
                            rows += "monitor${monitor + 1}=${device.iDstring};bounds=$bounds;viewport=${width}x$height;scaleX=${configuration.defaultTransform.scaleX};scaleY=${configuration.defaultTransform.scaleY}"
                        }
                    }
                    rows += "screenDpi=${java.awt.Toolkit.getDefaultToolkit().screenResolution}"
                    rows += "framesPresented=$framesPresented"
                    Files.writeString(
                        report,
                        rows.joinToString(System.lineSeparator()),
                    )
                    exitApplication()
                }
            }
        }
    }
}
