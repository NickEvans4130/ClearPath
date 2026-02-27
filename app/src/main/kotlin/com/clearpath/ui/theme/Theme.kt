package com.clearpath.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary            = Amber,
    onPrimary          = Background,
    primaryContainer   = AmberDim,
    onPrimaryContainer = OnSurface,
    secondary          = ExposureLow,
    onSecondary        = Background,
    tertiary           = ExposureHigh,
    onTertiary         = OnSurface,
    background         = Background,
    onBackground       = OnSurface,
    surface            = Surface,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceVariant,
    onSurfaceVariant   = OnSurfaceMuted,
    error              = ExposureHigh,
    onError            = OnSurface,
    outline            = SheetHandle,
)

@Composable
fun ClearPathTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = ClearPathTypography,
        content     = content,
    )
}
