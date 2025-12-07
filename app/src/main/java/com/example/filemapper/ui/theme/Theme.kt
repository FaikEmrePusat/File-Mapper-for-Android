package com.example.filemapper.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Custom dark color scheme for Spatial File Manager.
 * Features navy/black background with neon blue accents.
 */
private val SpatialDarkColorScheme = darkColorScheme(
    // Primary colors - Neon Blue
    primary = NeonBlue,
    onPrimary = DarkNavy,
    primaryContainer = NeonBlueDark,
    onPrimaryContainer = NeonBlueLight,
    
    // Secondary colors - Electric Purple
    secondary = ElectricPurple,
    onSecondary = DarkNavy,
    secondaryContainer = ElectricPurpleDark,
    onSecondaryContainer = ElectricPurpleLight,
    
    // Tertiary colors - Neon Pink
    tertiary = NeonPink,
    onTertiary = DarkNavy,
    tertiaryContainer = NeonPinkDark,
    onTertiaryContainer = NeonPinkLight,
    
    // Background colors - Navy/Black
    background = Navy,
    onBackground = TextPrimary,
    
    // Surface colors
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonBlue,
    
    // Container colors
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkNavy,
    inversePrimary = NeonBlueDark,
    
    // Outline colors
    outline = TextTertiary,
    outlineVariant = SurfaceLight,
    
    // Status colors
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,
    
    // Scrim
    scrim = DarkNavy.copy(alpha = 0.5f)
)

/**
 * Spatial File Manager Theme.
 * Always uses dark mode with neon blue accents for a futuristic look.
 */
@Composable
fun FileMapperTheme(
    // Always use dark theme for this app
    darkTheme: Boolean = true,
    // Disable dynamic color to maintain consistent neon aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = SpatialDarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar and navigation bar to match theme
            window.statusBarColor = DarkNavy.toArgb()
            window.navigationBarColor = DarkNavy.toArgb()
            
            // Use light icons on dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}