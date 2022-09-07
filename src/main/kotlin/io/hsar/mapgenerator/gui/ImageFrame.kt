package io.hsar.mapgenerator.gui

import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.SwingUtilities


object ImageFrame {

    val frame = JFrame()
        .also {
            it.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        }

    fun showImage(image: BufferedImage) = SwingUtilities.invokeLater {
        frame.isVisible = true
        frame.setBounds(0, 0, image.width, image.height)
        frame.graphics.drawImage(image, 0, 0, frame)
    }

}