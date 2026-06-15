package io.hsar.mapgenerator.commandgui

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.Timer
import javax.swing.SwingConstants
import javax.swing.SwingWorker
import kotlin.math.abs
import kotlin.math.roundToInt

class MapViewerFrame(
    private val renderMap: (MapViewerSettings) -> BufferedImage,
    private val initialSettings: MapViewerSettings = MapViewerSettings(),
) : JFrame(initialSettings.windowTitle()) {

    private val availableLayers = MapLayer.values().toList()
    private val imageLabel = JLabel("Loading map...", SwingConstants.CENTER)
    private val layerToggles = availableLayers.associateWith { layer ->
        JCheckBox(layer.displayName, layer in initialSettings.enabledLayers).also {
            it.addActionListener { requestRenderNow() }
        }
    }
    private val imageScrollPane = JScrollPane(imageLabel)

    private var currentSettings = initialSettings
    private var renderRevision = 0
    private var activeWorker: SwingWorker<BufferedImage, Unit>? = null
    private val renderDebounceTimer = Timer(RENDER_DEBOUNCE_MS) { requestRenderNow() }.apply {
        isRepeats = false
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        preferredSize = Dimension(1280, 900)

        imageLabel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        imageScrollPane.isWheelScrollingEnabled = false
        imageScrollPane.preferredSize = Dimension(1280, 840)

        val controls = JPanel()
        controls.add(JLabel("Layers:"))
        availableLayers.forEach { layer ->
            controls.add(layerToggles.getValue(layer))
        }

        add(controls, BorderLayout.NORTH)
        add(imageScrollPane, BorderLayout.CENTER)

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

        imageLabel.addMouseWheelListener(zoomWheelHandler)
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
            imageLabel.icon = null
            imageLabel.text = "Enable one or more layers to view the combined map."
            return
        }

        imageLabel.text = "Rendering map..."
        imageLabel.icon = null

        val renderSettings = currentSettings.copy(enabledLayers = selectedLayers)
        val revision = ++renderRevision
        activeWorker?.cancel(true)
        activeWorker = object : SwingWorker<BufferedImage, Unit>() {
            override fun doInBackground(): BufferedImage = renderMap(renderSettings)

            override fun done() {
                if (revision != renderRevision || isCancelled) {
                    return
                }

                try {
                    imageLabel.icon = ImageIcon(get())
                    imageLabel.text = null
                } finally {
                    activeWorker = null
                }
            }
        }.also { it.execute() }
    }

    companion object {
        private const val ZOOM_STEP_IN = 1.2
        private const val ZOOM_STEP_OUT = 1 / 1.2
        private const val RENDER_DEBOUNCE_MS = 120
    }
}
