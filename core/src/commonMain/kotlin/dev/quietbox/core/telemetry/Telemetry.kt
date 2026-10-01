package dev.quietbox.core.telemetry

import dev.quietbox.core.ai.Tier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class Outcome { SHOWN, HIDDEN_LOW_CONFIDENCE, HIDDEN_INVALID, FAILED }

data class Attempt(val tier: Tier, val ok: Boolean, val latencyMs: Long, val note: String)

data class Span(
    val task: String,
    val promptVersion: Int,
    val subject: String,
    val route: String,
    val attempts: List<Attempt>,
    val servedBy: Tier?,
    val outcome: Outcome,
    val confidence: Double?,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalMs: Long,
)

class Telemetry(private val keep: Int = 200) {
    private val _spans = MutableStateFlow<List<Span>>(emptyList())
    val spans: StateFlow<List<Span>> = _spans

    fun record(span: Span) = _spans.update { (listOf(span) + it).take(keep) }

    fun clear() = _spans.update { emptyList() }

    val cloudTokens: Int get() = _spans.value.sumOf { it.inputTokens + it.outputTokens }
}
