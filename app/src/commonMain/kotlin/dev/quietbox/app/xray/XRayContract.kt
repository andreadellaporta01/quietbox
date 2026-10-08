package dev.quietbox.app.xray

import dev.quietbox.app.session.Environment
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.telemetry.Outcome
import dev.quietbox.core.telemetry.Span

object XRayContract {

    /**
     * In the order the talk needs them: Lab 1 private, Lab 2 fallback and hidden, Lab 3 "2 tries".
     * One tap to the spans a moment of the talk is about. Each lens is a question you ask the X-ray
     * on stage, not a database filter: "did the private email leave the device?", "where did the cloud fail?".
     */
    enum class Lens(val label: String, val matches: (Span) -> Boolean) {
        All("all", { true }),
        Private("private", { it.route.startsWith("private") }),
        Fallback("fallback", { span -> span.attempts.any { !it.ok && it.tier == Tier.CLOUD } && span.attempts.any { it.ok && it.tier == Tier.ON_DEVICE } }),
        Hidden("hidden", { it.outcome == Outcome.HIDDEN_LOW_CONFIDENCE || it.outcome == Outcome.HIDDEN_INVALID }),
        Repaired("2 tries", { span -> span.attempts.any { it.ok && it.note.contains("tries") } }),
        Failed("failed", { it.outcome == Outcome.FAILED }),
    }

    data class State(
        val engine: String = "",
        val environment: Environment = Environment(),
        val spans: List<Span> = emptyList(),
        val tokensSpent: Int = 0,
        val tokenLimit: Int = 0,
        val query: String = "",
        val lens: Lens = Lens.All,
    ) {
        val callsByTier: Map<String, Int> get() = spans.groupingBy { it.servedBy?.name?.lowercase() ?: "none" }.eachCount()
        val p95Ms: Long
            get() = spans.map { it.totalMs }.sorted().let { it.getOrNull(((it.size - 1) * 0.95).toInt()) ?: 0 }

        /** The stats above always count every call; only the list below narrows. */
        val visible: List<Span> get() = spans.filter { lens.matches(it) && it.matches(query) }

        /** How many spans each lens would show, so an empty lens is visible before you tap it. */
        val lensCounts: Map<Lens, Int> get() = Lens.entries.associateWith { lens -> spans.count(lens.matches) }
    }

    sealed interface Intent {
        data object ToggleOnline : Intent
        data object ToggleBatteryLow : Intent
        data object ToggleChaos : Intent
        data object Reset : Intent
        data class Search(val query: String) : Intent
        data class Focus(val lens: Lens) : Intent
    }

    sealed interface Result {
        data class Observed(val environment: Environment, val spans: List<Span>, val tokensSpent: Int) : Result
        data class Searched(val query: String) : Result
        data class Focused(val lens: Lens) : Result
    }

    /** The x-ray has nothing one-shot to say. */
    sealed interface Effect
}

/**
 * Every word must appear somewhere in the span: task, subject, route, or an attempt's note. So
 * "blood", "rate_limited", "extract sofia" or "timeout" all find what you'd point at on stage.
 */
internal fun Span.matches(query: String): Boolean {
    val words = query.lowercase().split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return true
    val haystack = buildString {
        append("$task@v$promptVersion $subject $route ${outcome.name} ${servedBy?.name.orEmpty()} ")
        attempts.forEach { append("${it.tier.name} ${it.note} ") }
    }.lowercase()
    return words.all { it in haystack }
}
