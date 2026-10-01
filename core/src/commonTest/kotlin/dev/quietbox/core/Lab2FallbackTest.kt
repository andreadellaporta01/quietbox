package dev.quietbox.core

import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.ai.Pipeline
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.ai.TokenBudget
import dev.quietbox.core.cloud.CloudFailure
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.CloudReply
import dev.quietbox.core.cloud.CloudRequest
import dev.quietbox.core.cloud.MockCloud
import dev.quietbox.core.inbox.Fixtures
import dev.quietbox.core.tasks.Category
import dev.quietbox.core.tasks.ExtractTask
import dev.quietbox.core.tasks.TriageTask
import dev.quietbox.core.telemetry.Outcome
import dev.quietbox.core.telemetry.Telemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Lab2FallbackTest {

    private val telemetry = Telemetry()
    private fun pipeline(cloud: CloudModel, conditions: Conditions = Conditions()) =
        Pipeline(cloud, telemetry, TokenBudget(50_000), conditions)

    private val invoice = Fixtures.all.first { it.message.id == "m01" }.message
    private val bloodTest = Fixtures.all.first { it.message.id == "m04" }.message

    @Test
    fun confidentLocalExtractionNeverCallsTheCloud() = runTest {
        val cloud = MockCloud(1.milliseconds)
        val result = pipeline(cloud).run(ExtractTask, invoice, invoice.subject)
        assertEquals(Tier.ON_DEVICE, result.servedBy)
        assertEquals("184,50 €", result.value!!.actions.single().amount)
        assertTrue(cloud.sent.isEmpty())
    }

    @Test
    fun sensitiveMessagesNeverReachTheCloud() = runTest {
        val cloud = MockCloud(1.milliseconds)
        pipeline(cloud).run(TriageTask, bloodTest, bloodTest.subject)
        assertTrue(cloud.sent.isEmpty())
    }

    @Test
    fun slowCloudFallsBackToDevice() = runTest {
        val slow = object : CloudModel {
            override val name = "slow"
            override suspend fun complete(request: CloudRequest): CloudReply {
                delay(10.seconds)
                error("unreachable")
            }
        }
        val result = pipeline(slow).run(TriageTask, invoice, invoice.subject)
        val span = telemetry.spans.value.single()
        assertEquals(listOf(Tier.CLOUD, Tier.ON_DEVICE), span.attempts.map { it.tier })
        assertTrue(span.attempts.first().note.startsWith("timeout"))
        assertEquals(Tier.ON_DEVICE, result.servedBy)
    }

    @Test
    fun unsureAnswersAreHiddenNotShown() = runTest {
        val down = object : CloudModel {
            override val name = "down"
            override suspend fun complete(request: CloudRequest): CloudReply =
                throw CloudFailure(CloudFailure.Kind.OFFLINE, "no route")
        }
        val result = pipeline(down).run(TriageTask, invoice, invoice.subject)
        assertNull(result.value)
        assertEquals(Outcome.HIDDEN_LOW_CONFIDENCE, result.outcome)
    }

    @Test
    fun secondRunIsServedFromCache() = runTest {
        val cloud = MockCloud(1.milliseconds)
        val p = pipeline(cloud)
        p.run(TriageTask, invoice, invoice.subject)
        val again = p.run(TriageTask, invoice, invoice.subject)
        assertEquals(Tier.CACHE, again.servedBy)
        assertEquals(Category.ACTION, again.value!!.category)
        assertEquals(1, cloud.sent.size)
    }
}
