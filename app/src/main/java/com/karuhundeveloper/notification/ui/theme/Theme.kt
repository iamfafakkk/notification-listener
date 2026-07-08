package com.karuhundeveloper.notification.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────────
// Deep navy / electric indigo / cyan accent — feels premium and purposeful.

val NavyDeep      = Color(0xFF0A0E1A)
val NavySurface   = Color(0xFF111827)
val NavyCard      = Color(0xFF1A2236)
val NavyElevated  = Color(0xFF1E2B42)
val IndigoAccent  = Color(0xFF6C63FF)
val IndigoLight   = Color(0xFF8B85FF)
val CyanAccent    = Color(0xFF00D4FF)
val ErrorRed      = Color(0xFFFF5C6A)
val SuccessGreen  = Color(0xFF34D399)
val TextPrimary   = Color(0xFFF0F4FF)
val TextSecondary = Color(0xFF8899BB)
val Divider       = Color(0xFF1E2B42)

private val DarkColorScheme = darkColorScheme(
    primary          = IndigoAccent,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = TextPrimary,
    secondary        = CyanAccent,
    onSecondary      = NavyDeep,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = TextPrimary,
    tertiary         = SuccessGreen,
    background       = NavyDeep,
    onBackground     = TextPrimary,
    surface          = NavySurface,
    onSurface        = TextPrimary,
    surfaceVariant   = NavyCard,
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed,
    onError          = Color.White,
    outline          = Color(0xFF2D3F5E),
    outlineVariant   = Color(0xFF1A2B45),
)

@Composable
fun NotificationInterceptorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
