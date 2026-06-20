package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.font.DefaultTtfFont
import korlibs.image.font.SystemFont
import korlibs.image.font.TtfFont
import korlibs.image.font.readFont
import korlibs.io.file.std.JvmClassLoaderResourcesVfs
import korlibs.io.file.std.jailedLocalVfs
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

internal enum class MapViewerFontRole {
    MAIN,
    STRONG,
    FLAVOUR,
}

internal object MapViewerKorgeFonts {
    private val resolver = MapViewerKorgeFontResolver(
        warn = { message -> LogManager.getLogger(MapViewerKorgeFonts::class.java).warn(message) },
    )

    fun font(role: MapViewerFontRole): TtfFont =
        resolver.font(role)
}

internal class MapViewerKorgeFontResolver(
    private val classLoader: ClassLoader = MapViewerKorgeFontResolver::class.java.classLoader,
    private val localFontDirectory: Path? = System.getenv("WINDIR")?.let { Path.of(it, "Fonts") },
    private val allowSystemFonts: Boolean = true,
    private val warn: (String) -> Unit = {},
) {
    private val cache = mutableMapOf<MapViewerFontRole, TtfFont>()
    private var warnedMissingFonts = false

    fun font(role: MapViewerFontRole): TtfFont =
        cache.getOrPut(role) {
            val spec = specs.getValue(role)
            loadClasspathFont(spec)
                ?: loadLocalFont(spec)
                ?: loadSystemFont(spec)
                ?: fallbackFont(spec)
        }

    private fun loadSystemFont(spec: FontSpec): TtfFont? =
        if (!allowSystemFonts) {
            null
        } else {
            runCatching {
                runBlocking { SystemFont(spec.displayName) }.ttf as? TtfFont
            }.getOrNull()
        }

    private fun loadClasspathFont(spec: FontSpec): TtfFont? =
        runCatching {
            spec.classpathPaths.asSequence()
                .mapNotNull { path ->
                    runCatching {
                        runBlocking { JvmClassLoaderResourcesVfs(classLoader)[path].readFont() as? TtfFont }
                    }.getOrNull()
                }
                .firstOrNull()
        }.getOrNull()

    private fun loadLocalFont(spec: FontSpec): TtfFont? =
        runCatching {
            val localDirectory = localFontDirectory ?: return null
            val fontFile = jailedLocalVfs(localDirectory.toString())[spec.fileName]
            runBlocking { fontFile.readFont() as? TtfFont }
        }.getOrNull()

    private fun fallbackFont(spec: FontSpec): TtfFont {
        if (!warnedMissingFonts) {
            warnedMissingFonts = true
            warn("TV Nord command GUI fonts were not found; falling back to KorGE DefaultTtfFont.")
        }
        return DefaultTtfFont
    }

    private data class FontSpec(
        val fileName: String,
        val classpathPaths: List<String>,
        val displayName: String,
    )

    companion object {
        private val specs = mapOf(
            MapViewerFontRole.MAIN to FontSpec(
                fileName = "TV NordEF Regular Con.ttf",
                classpathPaths = listOf("TV NordEF Regular Con.ttf", "fonts/TV NordEF Regular Con.ttf"),
                displayName = "TV Nord EF Cond Regular",
            ),
            MapViewerFontRole.STRONG to FontSpec(
                fileName = "TV NordEF Bold Con.ttf",
                classpathPaths = listOf("TV NordEF Bold Con.ttf", "fonts/TV NordEF Bold Con.ttf"),
                displayName = "TV Nord EF Cond Bold",
            ),
            MapViewerFontRole.FLAVOUR to FontSpec(
                fileName = "TV NordEF Regular Con.ttf",
                classpathPaths = listOf("TV NordEF Regular Con.ttf", "fonts/TV NordEF Regular Con.ttf"),
                displayName = "TV Nord EF Cond Regular",
            ),
        )
    }
}
