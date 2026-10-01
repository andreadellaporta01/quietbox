package dev.quietbox.core.cloud

import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration

class FlakyCloud(
    private val inner: CloudModel,
    private val extraLatency: Duration = Duration.ZERO,
    private val failureRate: Double = 0.0,
    private val malformedRate: Double = 0.0,
    private val random: Random = Random(42),
) : CloudModel {

    override val name = "flaky(${inner.name})"

    override suspend fun complete(request: CloudRequest): CloudReply {
        delay(extraLatency)
        if (random.nextDouble() < failureRate) throw CloudFailure(CloudFailure.Kind.SERVER, "injected 503")
        val reply = inner.complete(request)
        return if (random.nextDouble() < malformedRate) reply.copy(json = reply.json.dropLast(reply.json.length / 3)) else reply
    }
}
