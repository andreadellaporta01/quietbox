package dev.quietbox.core.ai

data class Route(val tiers: List<Tier>, val why: String)

object Router {

    fun route(spec: TaskSpec, privacy: Privacy, conditions: Conditions, localConfidence: Double?): Route {
        // region LAB-1
        if (privacy == Privacy.LOCAL_ONLY) return Route(listOf(Tier.ON_DEVICE), "private: never leaves the device")
        if (!conditions.online) return Route(listOf(Tier.ON_DEVICE), "offline")
        if (spec.trigger == Trigger.BACKGROUND && conditions.batteryLow) {
            return Route(listOf(Tier.ON_DEVICE), "battery low: background work stays local")
        }
        if (!conditions.cloudBudgetLeft) return Route(listOf(Tier.ON_DEVICE), "cloud token budget spent")
        if (localConfidence == null) return Route(listOf(Tier.CLOUD), "no on-device path for this task")
        if (localConfidence >= spec.escalateBelow) return Route(listOf(Tier.ON_DEVICE), "on-device is confident")
        return Route(listOf(Tier.CLOUD, Tier.ON_DEVICE), "low local confidence: escalate")
        // endregion
    }
}
