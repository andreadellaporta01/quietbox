package dev.quietbox.app.xray

import dev.quietbox.app.session.Environment
import dev.quietbox.core.telemetry.Span

object XRayContract {

    data class State(
        val engine: String = "",
        val environment: Environment = Environment(),
        val spans: List<Span> = emptyList(),
        val tokensSpent: Int = 0,
        val tokenLimit: Int = 0,
    ) {
        val callsByTier: Map<String, Int> get() = spans.groupingBy { it.servedBy?.name?.lowercase() ?: "none" }.eachCount()
        val p95Ms: Long
            get() = spans.map { it.totalMs }.sorted().let { it.getOrNull(((it.size - 1) * 0.95).toInt()) ?: 0 }
    }

    sealed interface Intent {
        data object ToggleOnline : Intent
        data object ToggleBatteryLow : Intent
        data object ToggleChaos : Intent
        data object Reset : Intent
    }

    sealed interface Result {
        data class Observed(val environment: Environment, val spans: List<Span>, val tokensSpent: Int) : Result
    }

    /** The x-ray has nothing one-shot to say. */
    sealed interface Effect
}
