package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.font.DefaultTtfFont
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.sameInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Files.copy
import java.nio.file.Path

class MapViewerKorgeFontResolverTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loads classpath font resources when present`() {
        val resolver = MapViewerKorgeFontResolver(
            classLoader = MapViewerKorgeFontResolver::class.java.classLoader,
            localFontDirectory = null,
            allowSystemFonts = false,
        )

        assertThat(MapViewerKorgeFontResolver::class.java.classLoader.getResource("TV NordEF Regular Con.ttf") != null, equalTo(true))
        assertThat(resolver.font(MapViewerFontRole.MAIN), not(sameInstance(DefaultTtfFont)))
        assertThat(resolver.font(MapViewerFontRole.MAIN).name, containsString("TV NordEF Regular Con.ttf"))
    }

    @Test
    fun `loads local font file when classpath resource is absent`() {
        val sourceFont = MapViewerKorgeFontResolver::class.java.classLoader.getResourceAsStream("TV NordEF Regular Con.ttf")
            ?: error("Packaged TV Nord regular font is not present on the classpath.")
        val localFont = Files.createDirectories(tempDir.resolve("fonts")).resolve("TV NordEF Regular Con.ttf")
        sourceFont.use { input ->
            copy(input, localFont)
        }

        val resolver = MapViewerKorgeFontResolver(
            classLoader = URLClassLoader(emptyArray(), null),
            localFontDirectory = localFont.parent,
            allowSystemFonts = false,
        )

        assertThat(resolver.font(MapViewerFontRole.MAIN), not(sameInstance(DefaultTtfFont)))
        assertThat(resolver.font(MapViewerFontRole.MAIN).name, containsString("TV NordEF Regular Con.ttf"))
    }

    @Test
    fun `falls back to KorGE default font and emits one warning when TV Nord is missing`() {
        val warnings = mutableListOf<String>()
        val resolver = MapViewerKorgeFontResolver(
            classLoader = URLClassLoader(emptyArray(), null),
            localFontDirectory = tempDir,
            allowSystemFonts = false,
            warn = warnings::add,
        )

        assertThat(resolver.font(MapViewerFontRole.MAIN), sameInstance(DefaultTtfFont))
        assertThat(resolver.font(MapViewerFontRole.STRONG), sameInstance(DefaultTtfFont))
        assertThat(warnings.size, equalTo(1))
    }

    @Test
    fun `maps palette AWT fonts to command GUI font roles`() {
        assertThat(MapViewerKorgeFonts.font(MapViewerFontRole.MAIN), not(sameInstance(DefaultTtfFont)))
        assertThat(MapViewerKorgeFonts.font(MapViewerFontRole.STRONG), not(sameInstance(DefaultTtfFont)))
        assertThat(MapViewerKorgeFonts.font(MapViewerFontRole.FLAVOUR), not(sameInstance(DefaultTtfFont)))
    }

    @Test
    fun `text helper creates native autosized KorGE labels and explicit boxed labels`() {
        val text = MapViewerTextViews.label(
            parent = korlibs.korge.view.Container(),
            text = "Operational Command Tool",
            color = Palette.Colours.WHITE,
        )
        val boxedText = MapViewerTextViews.box(
            parent = korlibs.korge.view.Container(),
            text = "New Operation",
            width = 160.0,
            height = 42.0,
            color = Palette.Colours.WHITE,
        )

        assertThat(text.text, equalTo("Operational Command Tool"))
        assertThat(text.autoScaling, equalTo(true))
        assertThat(text.autoSize, equalTo(true))
        assertThat(text.textBounds.width, greaterThan(0.0))
        assertThat(text.textBounds.height, greaterThan(0.0))
        assertThat(boxedText.autoScaling, equalTo(true))
        assertThat(boxedText.autoSize, equalTo(false))
        assertThat(boxedText.textBounds.width, equalTo(160.0))
        assertThat(boxedText.textBounds.height, equalTo(42.0))
    }
}
