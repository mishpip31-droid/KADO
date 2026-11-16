package com.kado.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KadoPurple = Color(0xFF7141CB)
val Ink = Color(0xFF1D1933)
val Muted = Color(0xFF77718B)
val Pale = Color(0xFFF6F3FF)

private val light = lightColorScheme(
    primary = KadoPurple,
    onPrimary = Color.White,
    secondary = Color(0xFF8061B3),
    background = Color(0xFFFAF9FF),
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFEBF8),
    outline = Color(0xFFD6CFE5),
)
private val dark = darkColorScheme(
    primary = Color(0xFFC8ABFF),
    onPrimary = Color(0xFF37215D),
    secondary = Color(0xFFC5B5E8),
    background = Color(0xFF15131C),
    surface = Color(0xFF23202C),
    onSurface = Color(0xFFF0EBF6),
    surfaceVariant = Color(0xFF383243),
    outline = Color(0xFF625A72),
)

@Composable
fun KadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) dark else light,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
