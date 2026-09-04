package com.binnaclellc.grokbotvoice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Amber = Color(0xFFF59E0B)
private val Ink = Color(0xFF0B1220)
private val Panel = Color(0xFF152036)
private val Cream = Color(0xFFF8FAFC)

private val Colors = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = Panel,
    onSurface = Cream,
    error = Color(0xFFFCA5A5),
    onError = Ink,
)

@Composable
fun GrokBotVoiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}
