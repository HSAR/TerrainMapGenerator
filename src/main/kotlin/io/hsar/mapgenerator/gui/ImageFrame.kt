package io.hsar.mapgenerator.gui

import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities


object ImageFrame {

    val frame = JFrame()
        .also {
            it.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            it.isVisible = true
        }

    fun showImage(image: BufferedImage) = SwingUtilities.invokeLater {
        with(frame) {
            add(JLabel(ImageIcon(image)))
            setBounds(0, 0, image.width, image.height)
        }
    }

}