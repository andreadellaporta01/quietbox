package dev.quietbox.core.eval

import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.FirebaseCloud
import dev.quietbox.core.cloud.FlakyCloud
import dev.quietbox.core.cloud.MockCloud
import dev.quietbox.core.cloud.ProxyCloud
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) = runBlocking {
    val engine = args.firstOrNull() ?: "mock"
    val cloud: CloudModel = when (engine) {
        "proxy" -> ProxyCloud(
            baseUrl = System.getenv("QUIETBOX_PROXY_URL").orEmpty().ifBlank { error("set QUIETBOX_PROXY_URL") },
            token = System.getenv("QUIETBOX_TOKEN").orEmpty(),
        )
        "firebase" -> googleServices().let { (project, key) ->
            FirebaseCloud(project, key, model = System.getenv("QUIETBOX_MODEL") ?: FirebaseCloud.DEFAULT_MODEL)
        }
        "chaos" -> FlakyCloud(MockCloud(), extraLatency = 900.milliseconds, failureRate = 0.3, malformedRate = 0.2)
        else -> MockCloud()
    }
    val conditions = if (engine == "offline") Conditions(online = false) else Conditions()
    println(Eval.run(cloud, conditions).render())
}

// The same google-services.json the Android app uses, so the eval hits whichever project you dropped in.
private fun googleServices(): Pair<String, String> {
    val file = File(System.getenv("QUIETBOX_GOOGLE_SERVICES") ?: "androidApp/google-services.json")
    require(file.exists()) { "no ${file.path}: download it from your Firebase project" }
    val root = Json.parseToJsonElement(file.readText()).jsonObject
    val project = root.getValue("project_info").jsonObject.getValue("project_id").jsonPrimitive.content
    val key = root.getValue("client").jsonArray.first().jsonObject.getValue("api_key").jsonArray.first()
        .jsonObject.getValue("current_key").jsonPrimitive.content
    return project to key
}
