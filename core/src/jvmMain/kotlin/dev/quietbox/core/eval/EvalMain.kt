package dev.quietbox.core.eval

import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.FlakyCloud
import dev.quietbox.core.cloud.MockCloud
import dev.quietbox.core.cloud.ProxyCloud
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) = runBlocking {
    val engine = args.firstOrNull() ?: "mock"
    val cloud: CloudModel = when (engine) {
        "proxy" -> ProxyCloud(
            baseUrl = System.getenv("QUIETBOX_PROXY_URL").orEmpty().ifBlank { error("set QUIETBOX_PROXY_URL") },
            token = System.getenv("QUIETBOX_TOKEN").orEmpty(),
        )
        "chaos" -> FlakyCloud(MockCloud(), extraLatency = 900.milliseconds, failureRate = 0.3, malformedRate = 0.2)
        else -> MockCloud()
    }
    val conditions = if (engine == "offline") Conditions(online = false) else Conditions()
    println(Eval.run(cloud, conditions).render())
}
