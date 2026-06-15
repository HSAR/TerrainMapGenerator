package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.GraphicsGridRenderSurface
import io.hsar.mapgenerator.grid.GridLayerPainter
import java.awt.Color
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point as AwtPoint
import java.awt.image.BufferedImage
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.Timer
import javax.swing.SwingWorker
import javax.swing.SwingUtilities
import javax.swing.KeyStroke
import javax.swing.event.ChangeEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MapViewerFrame(
    private val renderMap: (MapViewerSettings) -> MapViewerRenderData,
    private val initialSettings: MapViewerSettings = MapViewerSettings(),
) : JFrame(initialSettings.windowTitle()) {

    private val availableLayers = initialSettings.availableLayers
    private val canvas = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val renderData = currentRenderData
            if (renderData == null) {
                drawMessage(g, renderStatusText ?: "Loading map...")
                return
            }

            val geometry = geometryFor(renderData.baseImage)
            val mapOffset = java.awt.Point(geometry.mapOffsetX, geometry.mapOffsetY)
            g.color = Color.BLACK
            g.fillRect(0, 0, width, height)
            g.drawImage(renderData.baseImage, mapOffset.x, mapOffset.y, null)
            renderData.gridLayer?.let { gridLayer ->
                val mapBounds = java.awt.Rectangle(mapOffset.x, mapOffset.y, renderData.baseImage.width, renderData.baseImage.height)
                val visibleMapBounds = visibleRect.intersection(mapBounds)

                if (visibleMapBounds.width > 0 && visibleMapBounds.height > 0) {
                    val graphics = (g as Graphics2D).create(mapOffset.x, mapOffset.y, renderData.baseImage.width, renderData.baseImage.height) as Graphics2D

                    try {
                        GridLayerPainter.draw(
                            GraphicsGridRenderSurface(graphics, renderData.baseImage.width, renderData.baseImage.height),
                            gridLayer,
                            visibleMapBounds.toLayerViewport(mapOffset.x, mapOffset.y),
                        )
                    } finally {
                        graphics.dispose()
                    }
                }
            }

            renderStatusText?.let { status ->
                drawMessage(g, status)
            }
        }
    }
    private val layerToggles = availableLayers.associateWith { layer ->
        JCheckBox(layer.displayName, layer.id in initialSettings.enabledLayerIds).also {
            it.addActionListener { requestRenderNow() }
        }
    }
    private val imageScrollPane = JScrollPane(canvas)
    private var spaceHeld = false
    private val panController = PanController()
    private var pendingZoomAnchor: ZoomAnchor? = null

    private var currentSettings = initialSettings
    private var renderRevision = 0
    private var activeWorker: SwingWorker<MapViewerRenderData, Unit>? = null
    private val renderDebounceTimer = Timer(RENDER_DEBOUNCE_MS) { requestRenderNow() }.apply {
        isRepeats = false
    }
    private var currentRenderData: MapViewerRenderData? = null
    private var renderStatusText: String? = "Loading map..."
    private var hasCenteredInitialViewport = false

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        preferredSize = Dimension(1280, 900)
        canvas.background = Color.BLACK

        imageScrollPane.isWheelScrollingEnabled = false
        imageScrollPane.preferredSize = Dimension(1280, 840)

        val controls = JPanel()
        controls.add(JLabel("Layers:"))
        availableLayers.forEach { layer ->
            controls.add(layerToggles.getValue(layer))
        }

        add(controls, BorderLayout.NORTH)
        add(imageScrollPane, BorderLayout.CENTER)
        imageScrollPane.viewport.addChangeListener { _: ChangeEvent ->
            canvas.repaint()
        }
        installPanBindings()

        val zoomWheelHandler = java.awt.event.MouseWheelListener { event ->
            pendingZoomAnchor = captureZoomAnchor()
            val steps = abs(event.preciseWheelRotation).roundToInt().coerceAtLeast(1)
            repeat(steps) {
                currentSettings = when {
                    event.preciseWheelRotation < 0 -> currentSettings.withZoom(ZOOM_STEP_IN)
                    else -> currentSettings.withZoom(ZOOM_STEP_OUT)
                }
            }
            scheduleRender()
            event.consume()
        }

        canvas.addMouseWheelListener(zoomWheelHandler)
        controls.addMouseWheelListener(zoomWheelHandler)
        layerToggles.values.forEach { it.addMouseWheelListener(zoomWheelHandler) }

        pack()
        setLocationRelativeTo(null)
    }

    fun showWindow() {
        isVisible = true
        requestRenderNow()
    }

    private fun scheduleRender() {
        activeWorker?.cancel(true)
        renderDebounceTimer.restart()
    }

    private fun requestRenderNow() {
        renderDebounceTimer.stop()
        val selectedLayers = availableLayers
            .filter { layerToggles.getValue(it).isSelected }
            .toSet()
        title = currentSettings.windowTitle()

        if (selectedLayers.isEmpty()) {
            activeWorker?.cancel(true)
            activeWorker = null
            currentRenderData = null
            renderStatusText = "Enable one or more layers to view the combined map."
            canvas.repaint()
            return
        }

        renderStatusText = "Rendering map..."
        canvas.repaint()

        val renderSettings = currentSettings.copy(enabledLayerIds = selectedLayers.map { it.id }.toSet())
        val revision = ++renderRevision
        activeWorker?.cancel(true)
        activeWorker = object : SwingWorker<MapViewerRenderData, Unit>() {
            override fun doInBackground(): MapViewerRenderData = renderMap(renderSettings)

            override fun done() {
                if (revision != renderRevision || isCancelled) {
                    return
                }

                try {
                    currentRenderData = get()
                    renderStatusText = null
                    val geometry = geometryFor(currentRenderData!!.baseImage)
                    canvas.preferredSize = Dimension(geometry.backgroundWidth, geometry.backgroundHeight)
                    canvas.revalidate()
                    val shouldCenter = !hasCenteredInitialViewport
                    val zoomAnchor = pendingZoomAnchor
                    pendingZoomAnchor = null
                    SwingUtilities.invokeLater {
                        if (zoomAnchor != null) {
                            imageScrollPane.viewport.viewPosition = viewportForZoomAnchor(zoomAnchor, geometry)
                            hasCenteredInitialViewport = true
                        } else if (shouldCenter) {
                            imageScrollPane.viewport.viewPosition = AwtPoint(geometry.mapOffsetX, geometry.mapOffsetY)
                            hasCenteredInitialViewport = true
                        }
                        clampViewportToBackground()
                        canvas.repaint()
                    }
                } finally {
                    activeWorker = null
                }
            }
        }.also { it.execute() }
    }

    private fun drawMessage(g: Graphics, message: String) {
        val metrics = g.fontMetrics
        val x = (canvas.width - metrics.stringWidth(message)) / 2
        val y = (canvas.height - metrics.height) / 2 + metrics.ascent
        g.drawString(message, x.coerceAtLeast(16), y.coerceAtLeast(16))
    }

    private fun captureZoomAnchor(): ZoomAnchor? {
        val renderData = currentRenderData ?: return null
        val pointer = MouseInfo.getPointerInfo()?.location ?: return null
        val canvasPoint = AwtPoint(pointer)
        SwingUtilities.convertPointFromScreen(canvasPoint, canvas)

        if (!canvas.contains(canvasPoint)) {
            return null
        }

        val geometry = geometryFor(renderData.baseImage)
        if (geometry.backgroundWidth <= 0 || geometry.backgroundHeight <= 0) {
            return null
        }

        val viewportPoint = AwtPoint(
            canvasPoint.x - imageScrollPane.viewport.viewPosition.x,
            canvasPoint.y - imageScrollPane.viewport.viewPosition.y,
        )

        return ZoomAnchor(
            screenPoint = viewportPoint,
            normalisedX = canvasPoint.x.toDouble() / geometry.backgroundWidth,
            normalisedY = canvasPoint.y.toDouble() / geometry.backgroundHeight,
        )
    }

    private fun viewportForZoomAnchor(anchor: ZoomAnchor, geometry: ViewerGeometry): AwtPoint {
        val contentX = anchor.normalisedX * geometry.backgroundWidth
        val contentY = anchor.normalisedY * geometry.backgroundHeight

        return clampViewport(
            AwtPoint(
                (contentX - anchor.screenPoint.x).roundToInt(),
                (contentY - anchor.screenPoint.y).roundToInt(),
            ),
        )
    }

    private fun installPanBindings() {
        canvas.isFocusable = true
        canvas.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3 || spaceHeld) {
                    panController.begin(e.locationOnScreen, if (spaceHeld) PanSource.Space else PanSource.RightClick)
                    e.consume()
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (panController.isActive) {
                    panController.end()
                    e.consume()
                }
            }
        })
        canvas.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                if (spaceHeld) {
                    panController.beginIfNeeded(e.locationOnScreen, PanSource.Space)
                }

                if (panController.isActive) {
                    panController.update(e.locationOnScreen)
                    e.consume()
                }
            }

            override fun mouseDragged(e: MouseEvent) {
                if (spaceHeld) {
                    panController.beginIfNeeded(e.locationOnScreen, PanSource.Space)
                }

                if (panController.isActive) {
                    panController.update(e.locationOnScreen)
                    e.consume()
                }
            }
        })
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).apply {
            put(KeyStroke.getKeyStroke("pressed SPACE"), "pan-start")
            put(KeyStroke.getKeyStroke("released SPACE"), "pan-stop")
        }
        rootPane.actionMap.apply {
            put("pan-start", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    spaceHeld = true
                    panController.beginFromPointerIfAvailable(PanSource.Space)
                    updatePanCursor()
                }
            })
            put("pan-stop", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    spaceHeld = false
                    if (panController.isSpacePanActive) {
                        panController.end()
                    } else {
                        updatePanCursor()
                    }
                }
            })
        }
    }

    private fun clampViewportToBackground() {
        imageScrollPane.viewport.viewPosition = clampViewport(imageScrollPane.viewport.viewPosition)
    }

    private fun clampViewport(point: AwtPoint): AwtPoint {
        val viewport = imageScrollPane.viewport
        val viewWidth = viewport.width.coerceAtLeast(1)
        val viewHeight = viewport.height.coerceAtLeast(1)
        val maxX = max(0, canvas.preferredSize.width - viewWidth)
        val maxY = max(0, canvas.preferredSize.height - viewHeight)

        return AwtPoint(
            point.x.coerceIn(0, maxX),
            point.y.coerceIn(0, maxY),
        )
    }

    private fun updatePanCursor() {
        canvas.cursor = when {
            panController.isActive || spaceHeld -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
            else -> Cursor.getDefaultCursor()
        }
    }

    private inner class PanController {
        private var activeSource: PanSource? = null
        private var panAnchorScreen: AwtPoint? = null
        private var panViewportStart: AwtPoint? = null
        private val panTimer = Timer(PAN_POLL_MS) {
            if (isActive) {
                MouseInfo.getPointerInfo()?.location?.let(::update)
            }
        }.apply {
            isRepeats = true
        }

        val isActive: Boolean
            get() = activeSource != null

        val isSpacePanActive: Boolean
            get() = activeSource == PanSource.Space

        fun begin(screenPoint: AwtPoint, source: PanSource) {
            activeSource = source
            panAnchorScreen = AwtPoint(screenPoint)
            panViewportStart = imageScrollPane.viewport.viewPosition
            panTimer.start()
            canvas.requestFocusInWindow()
            updatePanCursor()
        }

        fun beginIfNeeded(screenPoint: AwtPoint, source: PanSource) {
            if (!isActive) {
                begin(screenPoint, source)
            }
        }

        fun beginFromPointerIfAvailable(source: PanSource) {
            val pointer = MouseInfo.getPointerInfo()?.location ?: return
            beginIfNeeded(pointer, source)
        }

        fun update(screenPoint: AwtPoint) {
            val anchor = panAnchorScreen ?: return
            val start = panViewportStart ?: return
            val deltaX = anchor.x - screenPoint.x
            val deltaY = anchor.y - screenPoint.y
            imageScrollPane.viewport.viewPosition = clampViewport(
                AwtPoint(start.x + deltaX, start.y + deltaY),
            )
            canvas.repaint()
        }

        fun end() {
            activeSource = null
            panAnchorScreen = null
            panViewportStart = null
            panTimer.stop()
            updatePanCursor()
        }
    }

    private enum class PanSource {
        RightClick,
        Space,
    }

    private data class ZoomAnchor(
        val screenPoint: AwtPoint,
        val normalisedX: Double,
        val normalisedY: Double,
    )

    companion object {
        private const val ZOOM_STEP_IN = 1.1
        private const val ZOOM_STEP_OUT = 1 / 1.1
        private const val RENDER_DEBOUNCE_MS = 120
        private const val PAN_POLL_MS = 16
    }
}
