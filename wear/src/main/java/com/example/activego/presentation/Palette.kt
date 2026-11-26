package com.example.activego.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.activego.R

/**
 * Design tokens taken 1:1 from the ActiveGo Figma file (page "Smartwatch").
 * The same values are used by the phone module, so both apps read as one product.
 */
object Palette {
    val Background = Color(0xFF090B09)
    val Surface = Color(0xFF121612)
    val Border = Color(0xFF232A23)
    val Track = Color(0xFF1C221C)
    val Lime = Color(0xFFCCFF00)
    val LimeTint = Color(0x1ACCFF00)      // rgba(204,255,0,0.1)
    val Red = Color(0xFFFF3B30)
    val RedTint = Color(0x1AFF3B30)       // rgba(255,59,48,0.1)
    val Green = Color(0xFF30D158)
    // Pause control. Not in the Figma file: system blue, the same palette family
    // as the design's red #FF3B30 and green #30D158.
    val Blue = Color(0xFF0A84FF)
    val BlueTint = Color(0x1A0A84FF)      // rgba(10,132,255,0.1)
    val Gray = Color(0xFF8B958B)
    val White = Color(0xFFFFFFFF)
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
