package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.TerrainMapGenerator
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingWorker

class MapViewerFrame(
    private val initialSettings: MapViewerSettings = MapViewerSettings(),
) : JFrame(initialSettings.windowTitle()) {

    private val imageLabel = JLabel("Generate a map to begin viewing.", SwingConstants.CENTER)
    private val layerSelector = JComboBox(MapLayer.values())
    private val refreshButton = JButton("Render map")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        preferredSize = Dimension(1280, 900)

        imageLabel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)

        val controls = JPanel()
        controls.add(JLabel("Layer:"))
        controls.add(layerSelector)
        controls.add(refreshButton)

        add(controls, BorderLayout.NORTH)
        add(JScrollPane(imageLabel), BorderLayout.CENTER)

        layerSelector.selectedItem = initialSettings.layer
        refreshButton.addActionListener { renderSelectedMap() }

        pack()
        setLocationRelativeTo(null)
    }

    fun showWindow() {
        isVisible = true
        renderSelectedMap()
    }

    private fun renderSelectedMap() {
        val selectedLayer = layerSelector.selectedItem as MapLayer
        refreshButton.isEnabled = false
        imageLabel.text = "Rendering ${selectedLayer.displayName.lowercase()} map..."

        object : SwingWorker<BufferedImage, Unit>() {
            override fun doInBackground(): BufferedImage =
                MapViewer(initialSettings.copy(layer = selectedLayer))
                    .render()

            override fun done() {
                try {
                    imageLabel.icon = ImageIcon(get())
                    imageLabel.text = null
                } finally {
                    refreshButton.isEnabled = true
                }
            }
        }.execute()
    }
}

private class MapViewer(
    private val settings: MapViewerSettings,
) {
    fun render(): BufferedImage = TerrainMapGenerator(
        metresPerPixel = settings.metresPerPixel,
        metresPerContour = settings.metresPerContour,
        width = settings.width,
        height = settings.height,
    )
        .let { generator ->
            when (settings.layer) {
                MapLayer.GRAPH -> generator.generateGraphImage()
                MapLayer.HEIGHT -> generator.generateHeightImage()
                MapLayer.CONTOUR -> generator.generateContourImage()
                MapLayer.GRID -> generator.generateGridImage()
            }
        }
}
