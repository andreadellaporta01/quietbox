package dev.quietbox.app

import dev.quietbox.app.session.AiSession
import dev.quietbox.app.session.Environment
import dev.quietbox.core.ai.AiResult
import dev.quietbox.core.ai.TokenBudget
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.engine.Nudge
import dev.quietbox.core.engine.Opened
import dev.quietbox.core.engine.Row
import dev.quietbox.core.inbox.Fixtures
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.tasks.Category
import dev.quietbox.core.tasks.Extraction
import dev.quietbox.core.tasks.Summary
import dev.quietbox.core.tasks.Triage
import dev.quietbox.core.telemetry.Outcome
import dev.quietbox.core.telemetry.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The presentation layer's tests don't need a model, or the labs to be solved: the ViewModels
 * only see [AiSession]. Offline, everything is served on-device; online, from the cloud.
 */
class FakeAiSession : AiSession {
    override val engineName = "fake"
    override val telemetry = Telemetry()
    override val budget = TokenBudget(limit = 100)
    override val environment = MutableStateFlow(Environment())
    override val generation = MutableStateFlow(0)
    override val rows = MutableStateFlow<List<Row>>(emptyList())
    var sweeps = 0

    override suspend fun sweep(): List<Row> {
        sweeps++
        val tier = if (environment.value.conditions.online) Tier.CLOUD else Tier.ON_DEVICE
        return Fixtures.inbox.map { message ->
            Row(
                message,
                AiResult(Triage(Category.FYI, false, "fake"), tier, 0.9, Outcome.SHOWN),
                AiResult(Extraction(emptyList()), tier, 0.9, Outcome.SHOWN),
            )
        }.also { rows.value = it }
    }

    override fun nudges(rows: List<Row>): List<Nudge> = emptyList()

    override suspend fun open(message: Message, triage: Triage?): Opened =
        Opened(AiResult(Summary("tl;dr ${message.id}"), Tier.CLOUD, 0.9, Outcome.SHOWN), null)

    override fun update(change: (Environment) -> Environment) {
        environment.value = change(environment.value)
        generation.value++
    }

    override fun reset() {
        generation.value++
    }
}
