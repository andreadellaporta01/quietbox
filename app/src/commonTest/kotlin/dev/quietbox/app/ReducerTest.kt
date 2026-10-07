package dev.quietbox.app

import dev.quietbox.app.detail.DetailContract
import dev.quietbox.app.detail.DetailReducer
import dev.quietbox.app.inbox.InboxContract
import dev.quietbox.app.inbox.InboxReducer
import dev.quietbox.core.ai.AiResult
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.engine.Row
import dev.quietbox.core.inbox.Fixtures
import dev.quietbox.core.tasks.Category
import dev.quietbox.core.tasks.Extraction
import dev.quietbox.core.tasks.Triage
import dev.quietbox.core.telemetry.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReducerTest {

    private fun row(id: String, category: Category?) = Row(
        Fixtures.inbox.first { it.id == id },
        AiResult(category?.let { Triage(it, false, "test") }, Tier.ON_DEVICE, 0.9, Outcome.SHOWN),
        AiResult(Extraction(emptyList()), Tier.ON_DEVICE, 0.9, Outcome.SHOWN),
    )

    @Test
    fun resortingKeepsTheOldRowsOnScreen() {
        val sorted = InboxContract.State(rows = listOf(row("m01", Category.ACTION)))
        val next = InboxReducer(sorted, InboxContract.Result.SortStarted)
        assertTrue(next.sorting)
        assertEquals(sorted.rows, next.rows)
    }

    @Test
    fun sectionsFollowWhatTheUserCaresAbout() {
        val state = InboxContract.State(
            rows = listOf(row("m03", Category.NOISE), row("m08", Category.REPLY), row("m01", Category.ACTION), row("m05", null)),
        )
        assertEquals(listOf(Category.ACTION, Category.REPLY, null, Category.NOISE), state.sections.map { it.category })
    }

    @Test
    fun aHiddenSummaryMeansTheScreenShowsLessNotAnError() {
        val loaded = DetailReducer(DetailContract.State(), DetailContract.Result.Loaded(row("m09", Category.ACTION)))
        assertTrue(loaded.loading)
        val opened = DetailReducer(loaded, DetailContract.Result.Opened(summary = null, replies = null))
        assertFalse(opened.loading)
        assertEquals(null, opened.summary)
    }

    @Test
    fun anUnknownMessageIsNotLoading() {
        val state = DetailReducer(DetailContract.State(), DetailContract.Result.Loaded(null))
        assertFalse(state.loading)
    }
}
