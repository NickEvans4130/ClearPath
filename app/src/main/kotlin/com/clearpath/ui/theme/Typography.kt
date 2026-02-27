package com.clearpath.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clearpath.R

// JetBrains Mono is the primary typeface for data / numeric readouts.
// Place JetBrainsMono-Regular.ttf (and Bold variant) in assets/fonts/.
// The font is loaded from res/font/ — copy it there during project setup.
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

val ClearPathTypography = Typography(
    // Primary display — region names, large stats
    displayLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = OnSurface,
    ),
    // Section headers
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = OnSurface,
    ),
    // Card titles
    titleMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = OnSurface,
    ),
    // Body text / descriptions
    bodyMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = OnSurface,
    ),
    // Small labels, metadata
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = OnSurfaceMuted,
    ),
    // Numeric readouts (distance, score, time)
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = OnSurface,
    ),
)
