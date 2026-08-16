package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.command_gui_compose.generated.resources.Res
import io.hsar.mapgenerator.command_gui_compose.generated.resources.tv_nord_ef_bold_con
import io.hsar.mapgenerator.command_gui_compose.generated.resources.tv_nord_ef_regular_con
import org.jetbrains.compose.resources.Font
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager

@Composable
internal fun NordFontFamily(available: Boolean = true) =
    if (available) FontFamily(
        Font(Res.font.tv_nord_ef_regular_con, FontWeight.Normal),
        Font(Res.font.tv_nord_ef_bold_con, FontWeight.Bold),
    ) else FontFamily.SansSerif

internal data class ComposeFontDiagnostic(val available: Boolean) {
    val label get() = if (available) "TV Nord EF Condensed (packaged)" else "Sans-serif fallback (TV Nord unavailable)"
}

internal fun inspectComposeFonts() = runCatching {
    runBlocking {
        require(Res.readBytes("font/tv_nord_ef_regular_con.ttf").isNotEmpty())
        require(Res.readBytes("font/tv_nord_ef_bold_con.ttf").isNotEmpty())
    }
}.fold(
    onSuccess = { ComposeFontDiagnostic(true) },
    onFailure = {
        LogManager.getLogger("ComposeFontDiagnostic").warn("TV Nord command GUI fonts could not be loaded; using the sans-serif fallback.", it)
        ComposeFontDiagnostic(false)
    },
)

@Composable
internal fun NordTheme(fontAvailable: Boolean = true, content: @Composable () -> Unit) {
    val family = NordFontFamily(fontAvailable)
    val typography = Typography().withFontFamily(family)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CommandColours.Backing,
            onPrimary = CommandColours.Dark,
            secondary = CommandColours.Light,
            onSecondary = CommandColours.White,
            background = CommandColours.Dark,
            onBackground = CommandColours.White,
            surface = CommandColours.Dark,
            onSurface = CommandColours.White,
            surfaceVariant = CommandColours.Main,
            onSurfaceVariant = CommandColours.Backing,
            outline = CommandColours.Light,
            outlineVariant = CommandColours.Main,
            error = CommandColours.Red,
        ),
        typography = typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(0.dp),
            small = RoundedCornerShape(0.dp),
            medium = RoundedCornerShape(0.dp),
            large = RoundedCornerShape(0.dp),
            extraLarge = RoundedCornerShape(0.dp),
        ),
        content = content,
    )
}

internal object CommandColours {
    val Blue = Color(0xFF276A88)
    val Red = Color(0xFFC23E3C)
    val Green = Color(0xFF3D7652)
    val Yellow = Color(0xFF806E29)
    val Dark = Color(0xFF120909)
    val Main = Color(0xFF403A3A)
    val Light = Color(0xFF706B6B)
    val Backing = Color(0xFF9F9D9C)
    val White = Color(0xFFFFFFFF)
}

private fun Typography.withFontFamily(family: FontFamily) =
    copy(
        displayLarge = displayLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        displaySmall = displaySmall.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
