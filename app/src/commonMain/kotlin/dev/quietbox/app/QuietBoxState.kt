package dev.quietbox.app

import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.ai.Pipeline
import dev.quietbox.core.ai.TokenBudget
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.FlakyCloud
import dev.quietbox.core.engine.InboxEngine
import dev.quietbox.core.engine.Nudge
import dev.quietbox.core.engine.Opened
import dev.quietbox.core.engine.Row
import dev.quietbox.core.inbox.Fixtures
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

data class UiState(
    val rows: List<Row> = emptyList(),
    val sweeping: Boolean = false,
    val nudges: List<Nudge> = emptyList(),
    val open: Message? = null,
    val opened: Opened? = null,
    val opening: Boolean = false,
    val conditions: Conditions = Conditions(),
    val chaos: Boolean = false,
    val xray: Boolean = true,
)

class QuietBoxState(
    private val scope: CoroutineScope,
    private val cloud: CloudModel,
    val engineName: String,
    private val now: LocalDateTime = LocalDateTime(2026, 10, 7, 18, 0),
) {
    val telemetry = Telemetry()
    val budget = TokenBudget(limit = 20_000)

    private val calm = cloud
    private val chaotic = FlakyCloud(cloud, extraLatency = 1200.milliseconds, failureRate = 0.3, malformedRate = 0.2)

    private var pipeline = newPipeline(calm, Conditions())
    private var openJob: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private fun newPipeline(model: CloudModel, conditions: Conditions) = Pipeline(model, telemetry, budget, conditions)

    private fun engine() = InboxEngine(pipeline, Fixtures.inbox, Fixtures.threadContext)

    fun sweep() {
        _state.update { it.copy(sweeping = true) }
        scope.launch {
            val rows = engine().sweep()
            _state.update { it.copy(rows = rows, sweeping = false, nudges = engine().nudges(now, rows)) }
        }
    }

    fun open(message: Message) {
        openJob?.cancel()
        _state.update { it.copy(open = message, opened = null, opening = true) }
        val triage = _state.value.rows.firstOrNull { it.message.id == message.id }?.triage?.value
        openJob = scope.launch {
            val opened = engine().open(message, triage)
            _state.update { it.copy(opened = opened, opening = false) }
        }
    }

    fun close() {
        openJob?.cancel()
        _state.update { it.copy(open = null, opened = null, opening = false) }
    }

    fun setConditions(conditions: Conditions) {
        _state.update { it.copy(conditions = conditions) }
        rebuild()
    }

    fun setChaos(on: Boolean) {
        _state.update { it.copy(chaos = on) }
        rebuild()
    }

    fun toggleXray() = _state.update { it.copy(xray = !it.xray) }

    fun reset() {
        budget.reset()
        telemetry.clear()
        rebuild()
    }

    private fun rebuild() {
        val s = _state.value
        pipeline = newPipeline(if (s.chaos) chaotic else calm, s.conditions)
        sweep()
    }
}
