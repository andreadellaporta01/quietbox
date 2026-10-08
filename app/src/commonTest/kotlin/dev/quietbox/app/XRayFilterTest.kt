package dev.quietbox.app

import dev.quietbox.app.xray.XRayContract
import dev.quietbox.app.xray.XRayContract.Lens
import dev.quietbox.app.xray.XRayReducer
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.telemetry.Attempt
import dev.quietbox.core.telemetry.Outcome
import dev.quietbox.core.telemetry.Span
import kotlin.test.Test
import kotlin.test.assertEquals

class XRayFilterTest {

    private fun span(task: String, subject: String, route: String, outcome: Outcome, vararg attempts: Attempt) =
        Span(task, 4, subject, route, attempts.toList(), attempts.lastOrNull { it.ok }?.tier, outcome, 0.5, 0, 0, 10)

    private val blood = span(
        "extract", "Your blood test results", "private: never leaves the device", Outcome.HIDDEN_LOW_CONFIDENCE,
        Attempt(Tier.ON_DEVICE, true, 0, "conf 0.3"),
    )
    private val quota = span(
        "triage", "retro tomorrow?", "low local confidence: escalate", Outcome.HIDDEN_LOW_CONFIDENCE,
        Attempt(Tier.CLOUD, false, 320, "rate_limited"), Attempt(Tier.ON_DEVICE, true, 0, "conf 0.4"),
    )
    private val repaired = span(
        "extract", "Re: 2.0 release plan", "low local confidence: escalate", Outcome.SHOWN,
        Attempt(Tier.CLOUD, true, 900, "gemini-3.5-flash-lite, 2 tries"),
    )
    private val state = XRayContract.State(spans = listOf(blood, quota, repaired))

    @Test
    fun eachLensAnswersTheQuestionOfItsMoment() {
        assertEquals(listOf(blood), state.copy(lens = Lens.Private).visible)
        assertEquals(listOf(quota), state.copy(lens = Lens.Fallback).visible)
        assertEquals(listOf(repaired), state.copy(lens = Lens.Repaired).visible)
        assertEquals(listOf(blood, quota), state.copy(lens = Lens.Hidden).visible)
        assertEquals(1, state.lensCounts[Lens.Fallback])
    }

    @Test
    fun searchMatchesEveryWordAnywhereInTheSpan() {
        assertEquals(listOf(blood), state.copy(query = "blood").visible)
        assertEquals(listOf(quota), state.copy(query = "RATE_LIMITED").visible)
        assertEquals(listOf(repaired), state.copy(query = "extract release").visible)
        assertEquals(emptyList(), state.copy(query = "extract retro").visible)
    }

    @Test
    fun theStatsKeepCountingEverythingWhileTheListNarrows() {
        val narrowed = XRayReducer(state, XRayContract.Result.Searched("blood"))
        assertEquals(1, narrowed.visible.size)
        assertEquals(3, narrowed.spans.size)
    }

    @Test
    fun tappingTheActiveLensTurnsItOff() {
        val on = XRayReducer(state, XRayContract.Result.Focused(Lens.Private))
        assertEquals(Lens.Private, on.lens)
        assertEquals(Lens.All, XRayReducer(on, XRayContract.Result.Focused(Lens.Private)).lens)
    }
}
