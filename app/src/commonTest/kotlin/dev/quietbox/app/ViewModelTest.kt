package dev.quietbox.app

import dev.quietbox.app.detail.DetailContract
import dev.quietbox.app.detail.DetailViewModel
import dev.quietbox.app.inbox.InboxContract
import dev.quietbox.app.inbox.InboxViewModel
import dev.quietbox.app.xray.XRayContract
import dev.quietbox.app.xray.XRayViewModel
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.inbox.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theInboxSortsItselfOnStart() = runTest(dispatcher) {
        val session = FakeAiSession()
        val state = InboxViewModel(session).state.first { !it.sorting && it.rows.isNotEmpty() }
        assertEquals(Fixtures.inbox.size, state.rows.size)
        assertEquals(1, session.sweeps)
    }

    @Test
    fun goingOfflineInTheXrayResortsTheInboxOnDevice() = runTest(dispatcher) {
        val session = FakeAiSession()
        val inbox = InboxViewModel(session)
        val xray = XRayViewModel(session)

        xray.onIntent(XRayContract.Intent.ToggleOnline)

        val state = inbox.state.first { !it.sorting && it.rows.all { row -> row.triage.servedBy == Tier.ON_DEVICE } }
        assertEquals(2, session.sweeps)
        assertEquals(false, xray.state.value.environment.conditions.online)
        assertTrue(state.rows.isNotEmpty())
    }

    @Test
    fun openingAMessageIsAnEffectNotState() = runTest(dispatcher) {
        val vm = InboxViewModel(FakeAiSession())
        vm.onIntent(InboxContract.Intent.Open("m15"))
        assertEquals(InboxContract.Effect.OpenMessage("m15"), vm.effects.first())
    }

    @Test
    fun aDeepLinkedDetailWaitsForTheFirstSortThenSummarises() = runTest(dispatcher) {
        val session = FakeAiSession()
        val detail = DetailViewModel("m09", session)
        assertTrue(detail.state.value.row == null)

        session.sweep()

        val opened = detail.state.first { !it.loading }
        assertEquals("m09", opened.row?.message?.id)
        assertEquals("tl;dr m09", opened.summary?.tldr)
    }

    @Test
    fun backIsAnEffect() = runTest(dispatcher) {
        val vm = DetailViewModel("m09", FakeAiSession())
        vm.onIntent(DetailContract.Intent.Back)
        assertEquals(DetailContract.Effect.Close, vm.effects.first())
    }
}
