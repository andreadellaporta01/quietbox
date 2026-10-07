package dev.quietbox.core.cloud

import dev.quietbox.core.ai.Effort
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.time.TimeSource

/**
 * Gemini through Firebase AI Logic (Gemini Developer API backend), the same endpoint the
 * Firebase AI SDKs call. The only credential is the app's Firebase config key, which is
 * not a secret: in production App Check is what keeps other clients out.
 */
class FirebaseCloud(
    private val projectId: String,
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val appId: String? = null,
    private val http: HttpClient = ProxyCloud.defaultClient(),
) : CloudModel {

    override val name = model

    override suspend fun complete(request: CloudRequest): CloudReply {
        val mark = TimeSource.Monotonic.markNow()
        val response = try {
            http.post("$ENDPOINT/v1beta/projects/$projectId/models/$model:generateContent") {
                header("x-goog-api-key", apiKey)
                appId?.let { header("X-Firebase-AppId", it) }
                contentType(ContentType.Application.Json)
                setBody(body(request))
            }
        } catch (e: Exception) {
            throw CloudFailure(CloudFailure.Kind.OFFLINE, e.message ?: "network")
        }
        when (response.status) {
            HttpStatusCode.OK -> Unit
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                throw CloudFailure(CloudFailure.Kind.UNAUTHORIZED, response.bodyAsText().take(200))
            HttpStatusCode.TooManyRequests -> throw CloudFailure(CloudFailure.Kind.RATE_LIMITED, "quota")
            else -> throw CloudFailure(CloudFailure.Kind.SERVER, "firebase ${response.status.value}")
        }
        val reply: GenerateContentResponse = response.body()
        val candidate = reply.candidates.firstOrNull()
        if (candidate == null || candidate.finishReason in setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST")) {
            throw CloudFailure(CloudFailure.Kind.REFUSED, "model declined")
        }
        return CloudReply(
            json = candidate.content?.parts.orEmpty().mapNotNull { it.text }.joinToString(""),
            inputTokens = reply.usageMetadata?.promptTokenCount ?: 0,
            outputTokens = reply.usageMetadata?.candidatesTokenCount ?: 0,
            model = reply.modelVersion ?: model,
            latencyMs = mark.elapsedNow().inWholeMilliseconds,
        )
    }

    private fun body(request: CloudRequest): JsonObject = buildJsonObject {
        putJsonObject("systemInstruction") { putJsonArray("parts") { add(buildJsonObject { put("text", request.instructions) }) } }
        putJsonArray("contents") {
            add(
                buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") { add(buildJsonObject { put("text", request.input) }) }
                },
            )
        }
        putJsonObject("generationConfig") {
            put("responseMimeType", "application/json")
            put("responseJsonSchema", request.schema)
            put("maxOutputTokens", request.maxOutputTokens)
            putJsonObject("thinkingConfig") { put("thinkingLevel", request.effort.thinkingLevel()) }
        }
    }

    private fun Effort.thinkingLevel() = when (this) {
        Effort.LOW -> "minimal"
        Effort.MEDIUM -> "low"
        else -> "medium"
    }

    @Serializable
    private data class GenerateContentResponse(
        val candidates: List<Candidate> = emptyList(),
        val usageMetadata: Usage? = null,
        val modelVersion: String? = null,
    )

    @Serializable
    private data class Candidate(val content: Content? = null, val finishReason: String? = null)

    @Serializable
    private data class Content(val parts: List<Part> = emptyList())

    @Serializable
    private data class Part(val text: String? = null)

    @Serializable
    private data class Usage(val promptTokenCount: Int = 0, val candidatesTokenCount: Int = 0)

    companion object {
        const val ENDPOINT = "https://firebasevertexai.googleapis.com"
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
    }
}
