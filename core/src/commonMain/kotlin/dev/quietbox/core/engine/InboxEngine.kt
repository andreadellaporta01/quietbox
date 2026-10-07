package dev.quietbox.core.engine

import dev.quietbox.core.ai.AiResult
import dev.quietbox.core.ai.Pipeline
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.local.Retriever
import dev.quietbox.core.tasks.ActionItem
import dev.quietbox.core.tasks.Category
import dev.quietbox.core.tasks.ExtractTask
import dev.quietbox.core.tasks.Extraction
import dev.quietbox.core.tasks.Replies
import dev.quietbox.core.tasks.SmartReplyTask
import dev.quietbox.core.tasks.SummarizeTask
import dev.quietbox.core.tasks.Summary
import dev.quietbox.core.tasks.SummaryInput
import dev.quietbox.core.tasks.Triage
import dev.quietbox.core.tasks.TriageTask
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.LocalDateTime

data class Row(val message: Message, val triage: AiResult<Triage>, val extraction: AiResult<Extraction>)

data class Opened(val summary: AiResult<Summary>?, val replies: AiResult<Replies>?)

class InboxEngine(
    private val pipeline: Pipeline,
    private val messages: List<Message>,
    history: List<Message> = emptyList(),
    private val parallelism: Int = 3,
) {
    private val retriever = Retriever(messages + history)

    suspend fun sweep(): List<Row> = coroutineScope {
        val gate = Semaphore(parallelism)
        messages.map { message ->
            async {
                gate.withPermit {
                    val triage = pipeline.run(TriageTask, message, message.subject)
                    val extraction = if (triage.value?.category == Category.NOISE) {
                        AiResult(Extraction(emptyList()), triage.servedBy, triage.confidence, triage.outcome)
                    } else {
                        pipeline.run(ExtractTask, message, message.subject)
                    }
                    Row(message, triage, extraction)
                }
            }
        }.awaitAll()
    }

    suspend fun open(message: Message, triage: Triage?): Opened = coroutineScope {
        val summary = if (SummarizeTask.worthIt(message)) {
            async { pipeline.run(SummarizeTask, SummaryInput(message, retriever.related(message)), message.subject) }
        } else {
            null
        }
        val replies = if (triage?.category == Category.REPLY) {
            async { pipeline.run(SmartReplyTask, message, message.subject) }
        } else {
            null
        }
        Opened(summary?.await(), replies?.await())
    }

    /** Lab 4 is a stretch: until it's written, the inbox simply has no banners. */
    fun nudges(now: LocalDateTime, rows: List<Row>): List<Nudge> = try {
        Proactive.nudges(now, rows.associate { row -> row.message to (row.extraction.value?.actions ?: emptyList<ActionItem>()) })
    } catch (_: NotImplementedError) {
        emptyList()
    }
}
