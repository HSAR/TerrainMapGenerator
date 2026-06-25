package io.hsar.mapgenerator.image

import java.awt.Color
import java.awt.Font

object Palette {
    object Colours {
        val BLUE = Color.decode("#276A88")

        val RED = Color.decode("#C23E3C")

        val GREEN = Color.decode("#3D7652")

        val YELLOW = Color.decode("#806E29")

        val DARK = Color.decode("#120909")

        val MAIN = Color.decode("#403A3A")

        val LIGHT = Color.decode("#706B6B")

        val BACKING = Color.decode("#9F9D9C")

        val WHITE = Color.decode("#FFFFFF")
    }

    object Fonts {
        val BASE = Font("TVNordEF-RegularCon", Font.PLAIN, 20) to Colours.MAIN
        val MAIN = BASE
        val STRONG = Font("TVNordEF-BoldCon", Font.PLAIN, 20) to Colours.DARK
        val FLAVOUR = Font("TVNordEF-RegularCon", Font.PLAIN, 20) to Colours.LIGHT
    }
}
