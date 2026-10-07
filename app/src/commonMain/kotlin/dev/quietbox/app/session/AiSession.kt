package dev.quietbox.app.session

import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.ai.Pipeline
import dev.quietbox.core.ai.TokenBudget
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.FlakyCloud
import dev.quietbox.core.engine.InboxEngine
import dev.quietbox.core.engine.Nudge
import dev.quietbox.core.engine.Opened
import dev.quietbox.core.engine.Row
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.tasks.Triage
import dev.quietbox.core.telemetry.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/** What the device and the demo controls say right now. Every screen reads the same one. */
data class Environment(val conditions: Conditions = Conditions(), val chaos: Boolean = false)

/**
 * The app's single source of truth for everything AI: which cloud, which conditions, the shared
 * token budget and telemetry. A singleton in Koin, so the inbox, the detail and the x-ray agree.
 * The ViewModels never build a Pipeline themselves.
 */
class AiSession(
    cloud: CloudModel,
    val engineName: String,
    private val messages: List<Message>,
    private val history: List<Message>,
    private val now: () -> LocalDateTime,
    val telemetry: Telemetry = Telemetry(),
    val budget: TokenBudget = TokenBudget(limit = 20_000),
) {
    private val calm = cloud
    private val chaotic = FlakyCloud(cloud, extraLatency = 1200.milliseconds, failureRate = 0.3, malformedRate = 0.2)

    private val _environment = MutableStateFlow(Environment())
    val environment: StateFlow<Environment> = _environment.asStateFlow()

    /** Bumped whenever the environment changes, so screens know their results are stale. */
    private val _generation = MutableStateFlow(0)
    val generation: StateFlow<Int> = _generation.asStateFlow()

    private fun engine(): InboxEngine {
        val env = _environment.value
        val pipeline = Pipeline(if (env.chaos) chaotic else calm, telemetry, budget, env.conditions)
        return InboxEngine(pipeline, messages, history)
    }

    /** The last sweep, so a screen that only has a message id can find its triage and extraction. */
    private val _rows = MutableStateFlow<List<Row>>(emptyList())
    val rows: StateFlow<List<Row>> = _rows.asStateFlow()

    suspend fun sweep(): List<Row> = engine().sweep().also { _rows.value = it }

    fun nudges(rows: List<Row>): List<Nudge> = engine().nudges(now(), rows)

    suspend fun open(message: Message, triage: Triage?): Opened = engine().open(message, triage)

    fun update(change: (Environment) -> Environment) {
        _environment.update(change)
        _generation.update { it + 1 }
    }

    fun reset() {
        budget.reset()
        telemetry.clear()
        _generation.update { it + 1 }
    }
}
