package dev.quietbox.app

import dev.quietbox.app.inbox.InboxContract
import dev.quietbox.app.inbox.InboxViewModel
import dev.quietbox.app.session.AiSession
import dev.quietbox.app.xray.XRayContract
import dev.quietbox.app.xray.XRayViewModel
import dev.quietbox.core.cloud.MockCloud
import dev.quietbox.core.inbox.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun session() = AiSession(
        cloud = MockCloud(latency = 1.milliseconds),
        engineName = "mock",
        messages = Fixtures.inbox,
        history = Fixtures.threadContext,
        now = { LocalDateTime(2026, 10, 7, 18, 0) },
    )

    @Test
    fun sortsTheWholeInboxOnStart() = runTest(dispatcher) {
        val vm = InboxViewModel(session())
        val state = vm.state.first { !it.sorting && it.rows.isNotEmpty() }
        assertEquals(Fixtures.inbox.size, state.rows.size)
        assertTrue(state.nudges.size <= 2)
    }

    @Test
    fun goingOfflineFromTheXrayResortsTheInbox() = runTest(dispatcher) {
        val session = session()
        val inbox = InboxViewModel(session)
        val xray = XRayViewModel(session)
        inbox.state.first { !it.sorting && it.rows.isNotEmpty() }

        xray.onIntent(XRayContract.Intent.ToggleOnline)
        inbox.state.first { it.sorting }
        val offline = inbox.state.first { !it.sorting }

        assertEquals(false, session.environment.value.conditions.online)
        assertTrue(offline.rows.all { it.triage.servedBy != dev.quietbox.core.ai.Tier.CLOUD })
    }

    @Test
    fun openingAMessageIsAnEffectNotState() = runTest(dispatcher) {
        val vm = InboxViewModel(session())
        vm.onIntent(InboxContract.Intent.Open("m15"))
        assertEquals(InboxContract.Effect.OpenMessage("m15"), vm.effects.first())
    }
}
