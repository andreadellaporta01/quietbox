package dev.quietbox.app.ui

import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.tasks.Category
import dev.quietbox.core.telemetry.Outcome

object Palette {
    // Shared with the slides: near-black indigo, violet → cyan accents.
    val paper = Color(0xFF0B0B1A)
    val ink = Color(0xFFEDE9FE)
    val muted = Color(0xFF9CA3C4)
    val line = Color(0xFF26264A)
    val accent = Color(0xFFA78BFA)
    val warm = Color(0xFF22D3EE)
    val xray = Color(0xFF06060F)
    val xrayText = Color(0xFFD7DCF5)

    fun of(category: Category) = when (category) {
        Category.ACTION -> warm
        Category.REPLY -> accent
        Category.FYI -> muted
        Category.NOISE -> line
    }

    fun of(tier: Tier?) = when (tier) {
        Tier.CACHE -> Color(0xFF8CE99A)
        Tier.ON_DEVICE -> Color(0xFF74C0FC)
        Tier.CLOUD -> Color(0xFFFFC078)
        null -> Color(0xFFFF8787)
    }

    fun of(outcome: Outcome) = when (outcome) {
        Outcome.SHOWN -> Color(0xFF8CE99A)
        Outcome.HIDDEN_LOW_CONFIDENCE, Outcome.HIDDEN_INVALID -> Color(0xFFFFD43B)
        Outcome.FAILED -> Color(0xFFFF8787)
    }
}

@Composable
fun QuietTheme(content: @Composable () -> Unit) = MaterialTheme(
    colors = darkColors(primary = Palette.accent, background = Palette.paper, surface = Palette.paper, onSurface = Palette.ink, onBackground = Palette.ink),
) {
    // Text without an explicit color follows the theme instead of defaulting to black.
    CompositionLocalProvider(LocalContentColor provides Palette.ink, content = content)
}
