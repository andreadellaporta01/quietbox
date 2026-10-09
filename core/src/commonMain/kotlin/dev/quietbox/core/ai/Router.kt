package dev.quietbox.core.ai

data class Route(val tiers: List<Tier>, val why: String)

object Router {

    fun route(spec: TaskSpec, privacy: Privacy, conditions: Conditions, localConfidence: Double?): Route {
        // LAB-1 · Route. Decide WHERE this task may run: return the tiers to try, in order, and why.
        //
        // You have:
        //   privacy          Privacy.LOCAL_ONLY or CLOUD_OK (a sensitive message is LOCAL_ONLY)
        //   conditions       .online, .batteryLow, .cloudBudgetLeft
        //   spec.trigger     Trigger.BACKGROUND (we sorted it) or ON_OPEN (the user tapped it)
        //   spec.escalateBelow   the confidence under which the device asks the cloud for help
        //   localConfidence  how sure onDevice() is, 0..1; null = this task has no on-device path
        //
        // The rules, first match wins. The order IS the design: put privacy anywhere else and
        // a later "optimisation" can leak a private message.
        //   1. private                         -> [ON_DEVICE]   privateContentNeverLeavesTheDevice
        //   2. offline                         -> [ON_DEVICE]   offlineStaysLocal
        //   3. BACKGROUND task and battery low -> [ON_DEVICE]   backgroundWorkStaysLocalOnLowBattery
        //      (ON_OPEN on low battery falls through)          userInitiatedWorkMayStillUseCloudOnLowBattery
        //   4. no cloud budget left            -> [ON_DEVICE]   spentBudgetStaysLocal
        //   5. no on-device path (null)        -> [CLOUD]
        //      confident (>= escalateBelow)    -> [ON_DEVICE]   confidentLocalSkipsTheCloud
        //      otherwise                       -> [CLOUD, ON_DEVICE]  unsureLocalEscalatesAndKeepsLocalAsFallback
        //
        // `why` is free text, and it is what the X-ray prints next to "route:".
        // Shape:  if (privacy == Privacy.LOCAL_ONLY) return Route(listOf(Tier.ON_DEVICE), "private: never leaves the device")
        TODO("LAB-1: decide the tiers. Order matters: privacy, offline, battery, budget, confidence.")
    }
}
