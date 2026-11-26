package com.example.activego.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.activego.R

/**
 * Design tokens taken 1:1 from the ActiveGo Figma file (page "Mobile Phone",
 * second iteration). The watch module uses the same values.
 */
object ActiveGoColors {
    val Background = Color(0xFF090B09)
    val Surface = Color(0xFF121612)
    val Border = Color(0xFF232A23)
    val Track = Color(0xFF1C221C)
    val Lime = Color(0xFFCCFF00)
    val LimeTint = Color(0x1ACCFF00)      // rgba(204,255,0,0.1)
    val Green = Color(0xFF30D158)
    // Pause control. Not in the Figma file: system blue, the same palette family as
    // the design's red #FF3B30 and green #30D158. Shared with the watch.
    val Blue = Color(0xFF0A84FF)
    val BlueTint = Color(0x1A0A84FF)      // rgba(10,132,255,0.1)
    val Red = Color(0xFFFF3B30)
    val RedTint = Color(0x1AFF3B30)       // rgba(255,59,48,0.1)
    val Gray = Color(0xFF8B958B)
    val Dim = Color(0xFF556055)
    val White = Color(0xFFFFFFFF)
    val Ink = Color(0xFF090B09)           // text on lime surfaces
}

/** Outfit: numbers, titles and buttons in the Figma file. */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold)
)

/** Geist: labels and secondary text in the Figma file. */
val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold)
)

private val ActiveGoDarkScheme = darkColorScheme(
    primary = ActiveGoColors.Lime,
    onPrimary = ActiveGoColors.Ink,
    secondary = ActiveGoColors.Green,
    background = ActiveGoColors.Background,
    onBackground = ActiveGoColors.White,
    surface = ActiveGoColors.Surface,
    onSurface = ActiveGoColors.White,
    surfaceVariant = ActiveGoColors.Track,
    onSurfaceVariant = ActiveGoColors.Gray,
    outline = ActiveGoColors.Border,
    error = ActiveGoColors.Red
)

/**
 * Base text style matching the Figma text layers: no letter spacing and the font's
 * natural ("normal") line height. Material 3 would otherwise apply bodyLarge to
 * every Text (0.5 sp tracking and a fixed 24 sp line height), which squeezes the
 * large numbers and spreads the labels compared with the design.
 */
val FigmaBaseText = TextStyle(fontFamily = Geist, letterSpacing = 0.sp)

@Composable
fun ActiveGoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ActiveGoDarkScheme) {
        CompositionLocalProvider(LocalTextStyle provides FigmaBaseText, content = content)
    }
}
