package dev.quietbox.core

import dev.quietbox.core.engine.Proactive
import dev.quietbox.core.inbox.Fixtures
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Lab4ProactiveTest {

    private val extracted = Fixtures.all.associate { it.message to it.golden.actions }

    @Test
    fun surfacesAtMostTwoNudgesSoonestFirst() {
        val nudges = Proactive.nudges(LocalDateTime(2026, 10, 7, 18, 0), extracted)
        assertEquals(2, nudges.size)
        assertEquals(listOf(0, 2), nudges.map { it.daysLeft })
    }

    @Test
    fun deliveriesNeverNag() {
        val nudges = Proactive.nudges(LocalDateTime(2026, 10, 5, 20, 0), extracted, max = 10)
        assertTrue(nudges.none { it.message.id == "m06" })
    }
}
