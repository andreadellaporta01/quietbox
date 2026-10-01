package dev.quietbox.core.cloud

import dev.quietbox.core.ai.Effort
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CloudRequest(
    val task: String,
    val promptVersion: Int,
    val instructions: String,
    val input: String,
    val schema: JsonObject,
    val maxOutputTokens: Int,
    val effort: Effort,
)

@Serializable
data class CloudReply(
    val json: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val model: String,
    val latencyMs: Long = 0,
)

class CloudFailure(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { OFFLINE, REFUSED, RATE_LIMITED, SERVER, UNAUTHORIZED }
}

interface CloudModel {
    val name: String

    suspend fun complete(request: CloudRequest): CloudReply
}
