package io.hsar.mapgenerator.commandgui.korge

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.geometryFor
import io.hsar.mapgenerator.commandgui.korge.input.PanPanner
import io.hsar.mapgenerator.commandgui.korge.input.PanState
import io.hsar.mapgenerator.commandgui.korge.input.PanTrigger
import io.hsar.mapgenerator.grid.GraphicsGridRenderSurface
import io.hsar.mapgenerator.grid.GridLayerPainter
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicReference
import korlibs.event.Key
import korlibs.event.MouseButton
import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.input.cursor
import korlibs.korge.input.keys
import korlibs.korge.input.mouse
import korlibs.korge.input.onDown
import korlibs.korge.input.onUpAnywhere
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.math.geom.Vector2D
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

internal class MapViewerScene(
    private val renderMap: (MapViewerSettings) -> MapViewerRenderData,
    private val initialSettings: MapViewerSettings,
) {

    fun showWindow() {
        thread(name = "command-gui-korge", isDaemon = false) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val renderData = renderMap(initialSettings)
            val renderedImage = renderMapImage(renderData)
            val renderedBitmap: Bitmap = renderedImage.toBMP32()
            val geometry = geometryFor(renderedImage)
            val lastPointer = AtomicReference(Vector2D.ZERO)

            runBlocking {
                Korge(
                    windowWidth = screenSize.width,
                    windowHeight = screenSize.height,
                    title = initialSettings.windowTitle(),
                    displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP,
                ) {
                    val canvasX = (gameWindow.width - geometry.backgroundWidth) / 2.0
                    val canvasY = (gameWindow.height - geometry.backgroundHeight) / 2.0

                    container panRoot@{
                        x = canvasX
                        y = canvasY
                        solidRect(geometry.backgroundWidth, geometry.backgroundHeight, Colors.BLACK)

                        val map = image(renderedBitmap) {
                            x = geometry.mapOffsetX.toDouble()
                            y = geometry.mapOffsetY.toDouble()
                        }

                        val panPanner = PanPanner(
                            view = map,
                            backgroundWidth = geometry.backgroundWidth,
                            backgroundHeight = geometry.backgroundHeight,
                        )
                        val panState = PanState(
                            panner = panPanner,
                            updateCursor = { this@panRoot.cursor = it },
                        )

                        fun clampPosition(candidate: Double, scaledSize: Double, backgroundSize: Int): Double {
                            val max = if (scaledSize <= backgroundSize) backgroundSize - scaledSize else 0.0
                            val min = if (scaledSize <= backgroundSize) 0.0 else backgroundSize - scaledSize
                            return candidate.coerceIn(min, max)
                        }

                        fun zoomAt(anchor: Vector2D, factor: Double) {
                            val canvasAnchor = anchor - Vector2D(canvasX, canvasY)
                            val oldScale = map.scaleX
                            val newScale = (oldScale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
                            val anchorMapX = (canvasAnchor.x - map.x) / oldScale
                            val anchorMapY = (canvasAnchor.y - map.y) / oldScale
                            val scaledWidth = map.unscaledWidth * newScale
                            val scaledHeight = map.unscaledHeight * newScale

                            map.scale = newScale
                            map.x = clampPosition(canvasAnchor.x - anchorMapX * newScale, scaledWidth, geometry.backgroundWidth)
                            map.y = clampPosition(canvasAnchor.y - anchorMapY * newScale, scaledHeight, geometry.backgroundHeight)
                        }

                        addUpdater {
                            panState.refreshCursor()
                        }

                        mouse {
                            onDown {
                                lastPointer.set(currentPosStage)
                                panState.onMouseDown(button, currentPosStage, buttons)
                            }
                            onMoveAnywhere {
                                lastPointer.set(currentPosStage)
                                panState.onMouseMove(currentPosStage, buttons)
                            }
                            onUpAnywhere {
                                panState.onMouseUp(button, buttons)
                            }
                            onScrollAnywhere {
                                lastPointer.set(currentPosStage)
                                zoomAt(currentPosStage, if (scrollDeltaYLines < 0) ZOOM_STEP_IN else ZOOM_STEP_OUT)
                            }
                        }

                        keys {
                            down(Key.SPACE) {
                                panState.setHeld(PanTrigger.SPACE, true, lastPointer.get())
                            }
                            up(Key.SPACE) {
                                panState.setHeld(PanTrigger.SPACE, false, lastPointer.get())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderMapImage(renderData: MapViewerRenderData): BufferedImage =
        BufferedImage(
            renderData.baseImage.width,
            renderData.baseImage.height,
            BufferedImage.TYPE_INT_ARGB,
        ).also { image ->
            image.createGraphics().use { graphics ->
                graphics.drawImage(renderData.baseImage, 0, 0, null)
                renderData.gridLayer?.let { gridLayer ->
                    GridLayerPainter.draw(
                        GraphicsGridRenderSurface(graphics, image.width, image.height),
                        gridLayer,
                    )
                }
            }
        }

    private fun Graphics2D.use(block: (Graphics2D) -> Unit) {
        try {
            block(this)
        } finally {
            dispose()
        }
    }

    companion object {
        private const val MIN_SCALE = 0.5
        private const val MAX_SCALE = 4.0
        private const val ZOOM_STEP_IN = 1.1
        private const val ZOOM_STEP_OUT = 1 / 1.1
    }
}
