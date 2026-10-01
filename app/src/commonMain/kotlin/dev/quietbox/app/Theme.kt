package dev.quietbox.app

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.tasks.Category
import dev.quietbox.core.telemetry.Outcome

object Palette {
    val paper = Color(0xFFF7F5F0)
    val ink = Color(0xFF1D1B18)
    val muted = Color(0xFF7A746B)
    val line = Color(0xFFE4DFD6)
    val accent = Color(0xFF3B5BDB)
    val warm = Color(0xFFE8590C)
    val xray = Color(0xFF15171C)
    val xrayText = Color(0xFFD7DCE5)

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
    colors = lightColors(primary = Palette.accent, background = Palette.paper, surface = Color.White, onSurface = Palette.ink),
    content = content,
)
