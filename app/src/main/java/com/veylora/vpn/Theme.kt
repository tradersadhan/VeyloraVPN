package com.veylora.vpn

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF19D3C5)
val Navy = Color(0xFF0B1220)
val Card = Color(0xFF131C31)
val CardHigh = Color(0xFF1B2640)
val Danger = Color(0xFFFF5C7A)
val Muted = Color(0xFF8FA0BF)

@Composable
fun VeyloraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Teal,
            onPrimary = Navy,
            background = Navy,
            onBackground = Color.White,
            surface = Card,
            onSurface = Color.White,
            surfaceVariant = CardHigh,
            onSurfaceVariant = Muted,
            secondary = Color(0xFF7C5CFF),
            error = Danger
        ),
        content = content
    )
}
