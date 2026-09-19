package com.campussync.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme: androidx.compose.material3.ColorScheme
    @Composable get() = lightColorScheme(
        primary = BrandPrimary,
        primaryContainer = BrandPrimaryLight,
        background = OffWhiteBackground,
        surface = SurfaceWhite,
        onPrimary = SurfaceWhite,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        error = StatusError,
        errorContainer = StatusErrorBackground,
        outline = BorderLight
    )


@Composable
fun CampusSyncTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
