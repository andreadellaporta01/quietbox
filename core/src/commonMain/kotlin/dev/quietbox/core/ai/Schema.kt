package dev.quietbox.core.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

fun objectSchema(vararg properties: Pair<String, JsonObject>): JsonObject = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") { properties.forEach { (name, schema) -> put(name, schema) } }
    putJsonArray("required") { properties.forEach { add(it.first) } }
    put("additionalProperties", false)
}

fun stringSchema(description: String? = null): JsonObject = buildJsonObject {
    put("type", "string")
    description?.let { put("description", it) }
}

fun enumSchema(values: Iterable<Enum<*>>): JsonObject = buildJsonObject {
    put("type", "string")
    putJsonArray("enum") { values.forEach { add(it.name) } }
}

fun arraySchema(items: JsonObject, extra: JsonObjectBuilder.() -> Unit = {}): JsonObject = buildJsonObject {
    put("type", "array")
    put("items", items)
    extra()
}
