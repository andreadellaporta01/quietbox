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
import dev.quietbox.core.tasks.ActionItem
import dev.quietbox.core.tasks.ActionType
import dev.quietbox.core.tasks.Extraction
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

class Lab3GuardrailTest {

    private val telemetry = Telemetry()
    private fun pipeline(cloud: CloudModel) = Pipeline(cloud, telemetry, TokenBudget(50_000))

    @Test
    fun repairsAParaphrasedEvidenceWithOneRetry() = runTest {
        val release = Fixtures.all.first { it.message.id == MockCloud.HALLUCINATES }.message
        val result = pipeline(MockCloud(1.milliseconds)).run(ExtractTask, release, release.subject)
        assertEquals(Outcome.SHOWN, result.outcome)
        assertEquals("release candidate by 16 October", result.value!!.actions.single().evidence)
        assertTrue(telemetry.spans.value.single().attempts.single().note.contains("2 tries"))
    }

    private val release = Fixtures.all.first { it.message.id == "m09" }.message
    private val good = ActionItem(ActionType.DEADLINE, "RC to QA", "2026-10-16", null, null, "release candidate by 16 October")

    @Test
    fun acceptsAGroundedItem() {
        assertTrue(ExtractTask.validate(release, Extraction(listOf(good))).isEmpty())
    }

    @Test
    fun rejectsEvidenceThatIsNotAQuote() {
        val paraphrased = good.copy(evidence = "RC must reach QA by Oct 16")
        assertTrue(ExtractTask.validate(release, Extraction(listOf(paraphrased))).isNotEmpty())
    }

    @Test
    fun rejectsAnAmountThatIsNotInTheMessage() {
        val invented = good.copy(type = ActionType.PAYMENT, amount = "€1,200")
        assertTrue(ExtractTask.validate(release, Extraction(listOf(invented))).isNotEmpty())
    }

    @Test
    fun rejectsDatesThatAreNotIso() {
        assertTrue(ExtractTask.validate(release, Extraction(listOf(good.copy(date = "16/10/2026")))).isNotEmpty())
    }

    @Test
    fun rejectsMoreThanThreeItems() {
        assertTrue(ExtractTask.validate(release, Extraction(List(4) { good })).isNotEmpty())
    }
}
