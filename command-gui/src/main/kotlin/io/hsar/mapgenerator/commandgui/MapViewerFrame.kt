package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.GraphicsGridRenderSurface
import io.hsar.mapgenerator.grid.GridLayerPainter
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.Timer
import javax.swing.SwingWorker
import javax.swing.event.ChangeEvent
import kotlin.math.abs
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

            g.drawImage(renderData.baseImage, 0, 0, null)
            renderData.gridLayer?.let { gridLayer ->
                GridLayerPainter.draw(
                    GraphicsGridRenderSurface(g as Graphics2D, width, height),
                    gridLayer,
                    visibleRect.toGraphRectangle()
                )
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

    private var currentSettings = initialSettings
    private var renderRevision = 0
    private var activeWorker: SwingWorker<MapViewerRenderData, Unit>? = null
    private val renderDebounceTimer = Timer(RENDER_DEBOUNCE_MS) { requestRenderNow() }.apply {
        isRepeats = false
    }
    private var currentRenderData: MapViewerRenderData? = null
    private var renderStatusText: String? = "Loading map..."

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        preferredSize = Dimension(1280, 900)

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

        val zoomWheelHandler = java.awt.event.MouseWheelListener { event ->
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
                    canvas.preferredSize = Dimension(currentRenderData!!.baseImage.width, currentRenderData!!.baseImage.height)
                    canvas.revalidate()
                    canvas.repaint()
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

    private fun java.awt.Rectangle.toGraphRectangle() = Rectangle(
        Point(x.toDouble(), y.toDouble()),
        Point((x + width).toDouble(), (y + height).toDouble()),
    )

    companion object {
        private const val ZOOM_STEP_IN = 1.2
        private const val ZOOM_STEP_OUT = 1 / 1.2
        private const val RENDER_DEBOUNCE_MS = 120
    }
}
