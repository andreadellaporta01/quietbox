package dev.quietbox.proxy

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.RateLimitException
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import dev.quietbox.core.ai.Effort
import dev.quietbox.core.cloud.CloudFailure
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.CloudReply
import dev.quietbox.core.cloud.CloudRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.TimeSource

class Claude(
    private val model: String,
    private val fallbacks: Boolean,
    private val client: AnthropicClient = AnthropicOkHttpClient.fromEnv(),
) : CloudModel {

    override val name = model

    override suspend fun complete(request: CloudRequest): CloudReply = withContext(Dispatchers.IO) {
        val started = TimeSource.Monotonic.markNow()
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(request.maxOutputTokens.toLong() + THINKING_HEADROOM)
            .system(request.instructions)
            .addUserMessage(request.input)
            .outputConfig(
                OutputConfig.builder()
                    .effort(request.effort.toSdk())
                    .format(JsonOutputFormat.builder().schema(request.schema.toSdkSchema()).build())
                    .build(),
            )
            .apply {
                if (fallbacks) {
                    putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                }
            }
            .build()

        val message = try {
            client.messages().create(params)
        } catch (e: RateLimitException) {
            throw CloudFailure(CloudFailure.Kind.RATE_LIMITED, e.message ?: "rate limited")
        } catch (e: AnthropicServiceException) {
            throw CloudFailure(CloudFailure.Kind.SERVER, "upstream ${e.statusCode()}: ${e.message}")
        }

        if (message.stopReason().orElse(null) == StopReason.REFUSAL) {
            throw CloudFailure(CloudFailure.Kind.REFUSED, "model declined")
        }
        val text = message.content().mapNotNull { block -> block.text().orElse(null)?.text() }.joinToString("")
        CloudReply(
            json = text,
            inputTokens = message.usage().inputTokens().toInt(),
            outputTokens = message.usage().outputTokens().toInt(),
            model = message.model().asString(),
            latencyMs = started.elapsedNow().inWholeMilliseconds,
        )
    }

    private fun Effort.toSdk() = when (this) {
        Effort.LOW -> OutputConfig.Effort.LOW
        Effort.MEDIUM -> OutputConfig.Effort.MEDIUM
        Effort.HIGH -> OutputConfig.Effort.HIGH
    }

    private fun JsonObject.toSdkSchema(): JsonOutputFormat.Schema {
        val builder = JsonOutputFormat.Schema.builder()
        forEach { (key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value.toPlain())) }
        return builder.build()
    }

    private fun JsonElement.toPlain(): Any? = when (this) {
        is JsonObject -> mapValues { it.value.toPlain() }
        is JsonArray -> map { it.toPlain() }
        is JsonNull -> null
        is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull
    }

    private companion object {
        const val THINKING_HEADROOM = 4_000L
    }
}
