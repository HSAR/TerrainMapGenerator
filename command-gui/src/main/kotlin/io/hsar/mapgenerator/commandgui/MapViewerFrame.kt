package io.hsar.mapgenerator.commandgui

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

class MapViewerFrame(
    private val imagesByLayer: Map<MapLayer, BufferedImage>,
    private val initialSettings: MapViewerSettings = MapViewerSettings(),
) : JFrame(initialSettings.windowTitle()) {

    private val availableLayers = imagesByLayer.keys.sortedBy { it.ordinal }
    private val imageLabel = JLabel("Select a map layer to view.", SwingConstants.CENTER)
    private val layerSelector = JComboBox(availableLayers.toTypedArray())
    private val refreshButton = JButton("Show layer")

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

        if (availableLayers.isNotEmpty()) {
            layerSelector.selectedItem = initialSettings.layer.takeIf(imagesByLayer::containsKey)
                ?: availableLayers.first()
        } else {
            refreshButton.isEnabled = false
            imageLabel.text = "No map layers were provided."
        }

        refreshButton.addActionListener { showSelectedLayer() }

        pack()
        setLocationRelativeTo(null)
    }

    fun showWindow() {
        isVisible = true
        if (availableLayers.isNotEmpty()) {
            showSelectedLayer()
        }
    }

    private fun showSelectedLayer() {
        val selectedLayer = layerSelector.selectedItem as? MapLayer ?: return
        val image = imagesByLayer[selectedLayer] ?: return

        imageLabel.icon = ImageIcon(image)
        imageLabel.text = null
    }
}
