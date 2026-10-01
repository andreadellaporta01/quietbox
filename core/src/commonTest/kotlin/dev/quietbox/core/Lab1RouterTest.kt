package dev.quietbox.core

import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.ai.Privacy
import dev.quietbox.core.ai.Router
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.tasks.SummarizeTask
import dev.quietbox.core.tasks.TriageTask
import kotlin.test.Test
import kotlin.test.assertEquals

class Lab1RouterTest {

    private val online = Conditions()

    @Test
    fun privateContentNeverLeavesTheDevice() {
        val route = Router.route(TriageTask.spec, Privacy.LOCAL_ONLY, online, localConfidence = 0.1)
        assertEquals(listOf(Tier.ON_DEVICE), route.tiers)
    }

    @Test
    fun offlineStaysLocal() {
        val route = Router.route(TriageTask.spec, Privacy.CLOUD_OK, online.copy(online = false), localConfidence = 0.1)
        assertEquals(listOf(Tier.ON_DEVICE), route.tiers)
    }

    @Test
    fun backgroundWorkStaysLocalOnLowBattery() {
        val route = Router.route(TriageTask.spec, Privacy.CLOUD_OK, online.copy(batteryLow = true), localConfidence = 0.1)
        assertEquals(listOf(Tier.ON_DEVICE), route.tiers)
    }

    @Test
    fun userInitiatedWorkMayStillUseCloudOnLowBattery() {
        val route = Router.route(SummarizeTask.spec, Privacy.CLOUD_OK, online.copy(batteryLow = true), localConfidence = 0.1)
        assertEquals(Tier.CLOUD, route.tiers.first())
    }

    @Test
    fun spentBudgetStaysLocal() {
        val route = Router.route(TriageTask.spec, Privacy.CLOUD_OK, online.copy(cloudBudgetLeft = false), localConfidence = 0.1)
        assertEquals(listOf(Tier.ON_DEVICE), route.tiers)
    }

    @Test
    fun confidentLocalSkipsTheCloud() {
        val route = Router.route(TriageTask.spec, Privacy.CLOUD_OK, online, localConfidence = 0.95)
        assertEquals(listOf(Tier.ON_DEVICE), route.tiers)
    }

    @Test
    fun unsureLocalEscalatesAndKeepsLocalAsFallback() {
        val route = Router.route(TriageTask.spec, Privacy.CLOUD_OK, online, localConfidence = 0.3)
        assertEquals(listOf(Tier.CLOUD, Tier.ON_DEVICE), route.tiers)
    }
}
