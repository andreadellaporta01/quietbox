package dev.quietbox.core.ai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

data class Scored<O>(val value: O, val confidence: Double)

interface AiTask<I, O> {
    val spec: TaskSpec
    val serializer: KSerializer<O>
    val instructions: String
    val outputSchema: JsonObject

    fun id(input: I): String

    fun render(input: I): String

    fun privacy(input: I): Privacy = spec.privacy

    fun validate(input: I, output: O): List<String>

    fun onDevice(input: I): Scored<O>?
}
