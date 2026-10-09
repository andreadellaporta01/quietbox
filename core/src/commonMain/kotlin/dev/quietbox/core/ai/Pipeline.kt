package dev.quietbox.core.ai

import dev.quietbox.core.cloud.CloudFailure
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.CloudRequest
import dev.quietbox.core.telemetry.Attempt
import dev.quietbox.core.telemetry.Outcome
import dev.quietbox.core.telemetry.Span
import dev.quietbox.core.telemetry.Telemetry
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.TimeSource

data class AiResult<O>(val value: O?, val servedBy: Tier?, val confidence: Double?, val outcome: Outcome)

class Pipeline(
    private val cloud: CloudModel,
    private val telemetry: Telemetry,
    private val budget: TokenBudget,
    var conditions: Conditions = Conditions(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, AiResult<*>>()

    fun invalidate() = cache.clear()

    /**
     * On the `start` branch the labs are TODO()s. The app still has to open: an unfinished lab
     * becomes a FAILED span that says which lab, so the X-ray shows exactly what's missing.
     */
    suspend fun <I, O> run(task: AiTask<I, O>, input: I, subject: String): AiResult<O> = try {
        runTiers(task, input, subject)
    } catch (todo: NotImplementedError) {
        val lab = todo.message?.substringAfter("LAB-", "")?.take(1)?.let { "LAB-$it" } ?: "a lab"
        telemetry.record(
            Span(task.spec.name, task.spec.promptVersion, subject, "$lab not done yet", emptyList(), null, Outcome.FAILED, null, 0, 0, 0),
        )
        AiResult(null, null, null, Outcome.FAILED)
    }

    private suspend fun <I, O> runTiers(task: AiTask<I, O>, input: I, subject: String): AiResult<O> {
        val key = "${task.spec.name}@v${task.spec.promptVersion}:${task.id(input)}"
        @Suppress("UNCHECKED_CAST")
        (cache[key] as AiResult<O>?)?.let { return it.copy(servedBy = Tier.CACHE) }

        val started = TimeSource.Monotonic.markNow()
        val local = task.onDevice(input)
        val effective = conditions.copy(cloudBudgetLeft = budget.hasRoom)
        val route = Router.route(task.spec, task.privacy(input), effective, local?.confidence)
        val attempts = mutableListOf<Attempt>()
        var inTokens = 0
        var outTokens = 0
        var invalidSeen = false

        var served: Pair<Tier, Scored<O>>? = null
        // region LAB-2
        for (tier in route.tiers) {
            val mark = TimeSource.Monotonic.markNow()
            when (tier) {
                Tier.ON_DEVICE -> {
                    if (local == null) {
                        attempts += Attempt(tier, false, 0, "no on-device path")
                    } else if (task.validate(input, local.value).isNotEmpty()) {
                        invalidSeen = true
                        attempts += Attempt(tier, false, mark.ms(), "local output failed validation")
                    } else {
                        attempts += Attempt(tier, true, mark.ms(), "conf ${local.confidence.fmt()}")
                        served = tier to local
                    }
                }
                Tier.CLOUD -> {
                    val outcome = callCloud(task, input)
                    inTokens += outcome.inputTokens
                    outTokens += outcome.outputTokens
                    if (outcome.value != null) {
                        attempts += Attempt(tier, true, mark.ms(), outcome.note)
                        served = tier to Scored(outcome.value, CLOUD_CONFIDENCE)
                    } else {
                        invalidSeen = invalidSeen || outcome.invalid
                        attempts += Attempt(tier, false, mark.ms(), outcome.note)
                    }
                }
                Tier.CACHE -> Unit
            }
            if (served != null) break
        }
        // endregion

        val pick = served
        val result: AiResult<O> = when {
            pick == null -> AiResult(null, null, null, if (invalidSeen) Outcome.HIDDEN_INVALID else Outcome.FAILED)
            pick.second.confidence < task.spec.showAbove ->
                AiResult(null, pick.first, pick.second.confidence, Outcome.HIDDEN_LOW_CONFIDENCE)
            else -> AiResult(pick.second.value, pick.first, pick.second.confidence, Outcome.SHOWN)
        }
        telemetry.record(
            Span(
                task = task.spec.name,
                promptVersion = task.spec.promptVersion,
                subject = subject,
                route = route.why,
                attempts = attempts,
                servedBy = result.servedBy,
                outcome = result.outcome,
                confidence = result.confidence,
                inputTokens = inTokens,
                outputTokens = outTokens,
                totalMs = started.ms(),
            ),
        )
        if (result.outcome != Outcome.FAILED) cache[key] = result
        return result
    }

    private class CloudOutcome<O>(
        val value: O?,
        val note: String,
        val invalid: Boolean = false,
        val inputTokens: Int = 0,
        val outputTokens: Int = 0,
    )

    private suspend fun <I, O> callCloud(task: AiTask<I, O>, input: I): CloudOutcome<O> {
        var inTokens = 0
        var outTokens = 0
        var feedback: List<String> = emptyList()
        repeat(MAX_CLOUD_TRIES) { attempt ->
            val prompt = if (feedback.isEmpty()) task.render(input) else repairPrompt(task.render(input), feedback)
            val reply = try {
                withTimeout(task.spec.latencyBudget) {
                    cloud.complete(
                        CloudRequest(
                            task = task.spec.name,
                            promptVersion = task.spec.promptVersion,
                            instructions = task.instructions,
                            input = prompt,
                            schema = task.outputSchema,
                            maxOutputTokens = task.spec.maxOutputTokens,
                            effort = task.spec.cloudEffort,
                        ),
                    )
                }
            } catch (_: TimeoutCancellationException) {
                return CloudOutcome(null, "timeout after ${task.spec.latencyBudget}", inputTokens = inTokens, outputTokens = outTokens)
            } catch (e: CloudFailure) {
                if (e.kind == CloudFailure.Kind.SERVER && attempt < MAX_CLOUD_TRIES - 1) return@repeat
                return CloudOutcome(null, e.kind.name.lowercase(), inputTokens = inTokens, outputTokens = outTokens)
            }
            inTokens += reply.inputTokens
            outTokens += reply.outputTokens
            budget.charge(reply.inputTokens + reply.outputTokens)

            val parsed = runCatching { json.decodeFromString(task.serializer, reply.json) }.getOrNull()
            if (parsed == null) {
                feedback = listOf("the previous answer was not valid JSON for the schema")
                return@repeat
            }
            val errors = task.validate(input, parsed)
            if (errors.isEmpty()) {
                val retried = if (attempt > 0) ", ${attempt + 1} tries" else ""
                // A repair says what it repaired: that's the line the eval and the X-ray point at.
                val repaired = if (feedback.isNotEmpty()) " · repaired: ${feedback.first()}" else ""
                return CloudOutcome(parsed, "${reply.model}$retried$repaired", inputTokens = inTokens, outputTokens = outTokens)
            }
            feedback = errors
        }
        return CloudOutcome(null, "invalid: ${feedback.first()}", invalid = true, inputTokens = inTokens, outputTokens = outTokens)
    }

    private fun repairPrompt(original: String, errors: List<String>) = buildString {
        appendLine(original)
        appendLine()
        appendLine("Your previous answer was rejected by the app for these reasons:")
        errors.forEach { appendLine("- $it") }
        append("Answer again, fixing only these problems.")
    }

    private companion object {
        const val MAX_CLOUD_TRIES = 2
        const val CLOUD_CONFIDENCE = 0.9
    }
}

private fun TimeSource.Monotonic.ValueTimeMark.ms() = elapsedNow().inWholeMilliseconds

private fun Double.fmt() = ((this * 100).toInt() / 100.0).toString()
