package com.podtekst.decoder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CyberBg = Color(0xFF000A14)
val CyberBlue = Color(0xFF00E5FF)
val CyberBlueDeep = Color(0xFF0080FF)
val CyberBlueDim = Color(0xFF004060)
val CyberRed = Color(0xFFFF0040)
val CyberWhite = Color(0xFFCDF6FF)

private val PodtekstColors = darkColorScheme(
    primary = CyberBlue,
    onPrimary = CyberBg,
    secondary = CyberBlueDeep,
    onSecondary = CyberBg,
    background = CyberBg,
    onBackground = CyberWhite,
    surface = CyberBg,
    onSurface = CyberWhite,
    error = CyberRed,
    onError = CyberBg,
)

private val Mono = FontFamily.Monospace

val PodtekstTypography = Typography(
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = CyberBlue),
    titleMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CyberBlue),
    bodyLarge = TextStyle(fontFamily = Mono, fontSize = 16.sp, color = CyberWhite),
    bodyMedium = TextStyle(fontFamily = Mono, fontSize = 14.sp, color = CyberWhite),
    bodySmall = TextStyle(fontFamily = Mono, fontSize = 12.sp, color = CyberBlueDeep),
    labelMedium = TextStyle(fontFamily = Mono, fontSize = 12.sp, color = CyberBlue, fontWeight = FontWeight.Bold),
)

@Composable
fun PodtekstTheme(content: @Composable () -> Unit) {
    // Игнорируем системную тему — у нас всегда кибер-палитра.
    @Suppress("UNUSED_VARIABLE") val ignored = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = PodtekstColors,
        typography = PodtekstTypography,
        content = content,
    )
}
