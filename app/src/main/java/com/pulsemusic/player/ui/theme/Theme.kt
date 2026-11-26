package com.pulsemusic.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PulseCyan = Color(0xFF25F4EE)
val PulsePink = Color(0xFFFE2C55)
val PulseBg = Color(0xFF080B14)
val PulseSurface = Color(0xFF111726)
val PulseSurfaceAlt = Color(0xFF1A2236)

private val PulseDarkColors = darkColorScheme(
    primary = PulseCyan,
    onPrimary = Color(0xFF041C1E),
    secondary = PulsePink,
    onSecondary = Color.White,
    background = PulseBg,
    onBackground = Color(0xFFF8FAFC),
    surface = PulseSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = PulseSurfaceAlt,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun PulseMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PulseDarkColors,
        content = content
    )
}
