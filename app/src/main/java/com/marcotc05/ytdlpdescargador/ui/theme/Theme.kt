package com.marcotc05.ytdlpdescargador.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF),
    secondary = Color(0xFFFF0055),
    background = Color(0xFF0B0D17),
    surface = Color(0xFF141926)
)

@Composable
fun DownAllTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}