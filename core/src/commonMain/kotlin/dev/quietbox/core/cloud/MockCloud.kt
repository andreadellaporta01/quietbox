package dev.quietbox.core.cloud

import dev.quietbox.core.inbox.Fixtures
import dev.quietbox.core.local.Text
import dev.quietbox.core.tasks.Extraction
import dev.quietbox.core.tasks.Replies
import dev.quietbox.core.tasks.Summary
import dev.quietbox.core.tasks.Triage
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MockCloud(private val latency: Duration = 450.milliseconds) : CloudModel {

    override val name = "mock"

    private val json = Json { encodeDefaults = true }
    private val subjectLine = Regex("""(?im)^(?:subject:|MESSAGE from .*?, subject:)\s*(.+)$""")

    val sent = mutableListOf<CloudRequest>()

    companion object {
        const val HALLUCINATES = "m09"
    }

    override suspend fun complete(request: CloudRequest): CloudReply {
        sent += request
        delay(latency)
        val subject = subjectLine.findAll(request.input).lastOrNull()?.groupValues?.get(1)?.trim()
        val fixture = subject?.let(Fixtures::bySubject) ?: throw CloudFailure(CloudFailure.Kind.SERVER, "mock knows no such message")
        val golden = fixture.golden
        val repairing = request.input.contains("previous answer was rejected")

        val body = when (request.task) {
            "triage" -> json.encodeToString(Triage.serializer(), Triage(golden.category, golden.urgent, golden.reason))
            "extract" -> {
                val actions = if (fixture.message.id == HALLUCINATES && !repairing) {
                    golden.actions.map { it.copy(evidence = "RC must reach QA by Oct 16") }
                } else {
                    golden.actions
                }
                json.encodeToString(Extraction.serializer(), Extraction(actions))
            }
            "summarize" -> json.encodeToString(Summary.serializer(), Summary(golden.tldr ?: fixture.message.subject))
            "reply" -> json.encodeToString(Replies.serializer(), Replies(golden.replies ?: listOf("Thanks!", "Got it.", "Tell me more?")))
            else -> throw CloudFailure(CloudFailure.Kind.SERVER, "unknown task ${request.task}")
        }
        return CloudReply(
            json = body,
            inputTokens = Text.estimateTokens(request.instructions + request.input + request.schema.toString()),
            outputTokens = Text.estimateTokens(body),
            model = name,
            latencyMs = latency.inWholeMilliseconds,
        )
    }
}
