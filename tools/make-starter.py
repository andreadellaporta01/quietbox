#!/usr/bin/env python3
"""Turns the solved repo into the attendee starter: every LAB-N region becomes a TODO stub."""
import re, sys, pathlib

STUBS = {
    "LAB-1": """\
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
""",
    "LAB-2": """\
        // LAB-2 · Fall back. Walk the tiers the router chose, in order, until one gives a valid answer.
        //
        // You have:
        //   local                  onDevice()'s answer: Scored(value, confidence), or null
        //   task.validate(input, v)  reasons v is unacceptable; empty list = valid
        //   callCloud(task, input)   already does timeout, one retry, one repair. Returns a CloudOutcome:
        //                            .value (null if it failed) · .note · .invalid · .inputTokens · .outputTokens
        //   CLOUD_CONFIDENCE         the confidence we give a validated cloud answer
        //   val mark = TimeSource.Monotonic.markNow() ... mark.ms()   elapsed milliseconds
        //
        // For each tier:
        //   ON_DEVICE  local == null          -> attempts += Attempt(tier, false, 0, "no on-device path")
        //              validate() not empty   -> invalidSeen = true; attempts += Attempt(tier, false, ms, "local output failed validation")
        //              otherwise              -> attempts += Attempt(tier, true, ms, "conf ${local.confidence.fmt()}"); served = tier to local
        //   CLOUD      add the outcome's tokens to inTokens / outTokens, then
        //              value != null          -> attempts += Attempt(tier, true, ms, outcome.note); served = tier to Scored(value, CLOUD_CONFIDENCE)
        //              otherwise              -> invalidSeen = invalidSeen || outcome.invalid; attempts += Attempt(tier, false, ms, outcome.note)
        //   CACHE      nothing (the cache was checked above)
        // Then: if (served != null) break. Every Attempt is a line in the X-ray, so record failures too.
        //
        // Tests: sensitiveMessagesNeverReachTheCloud · slowCloudFallsBackToDevice (checks the exact
        // attempt list: forget the break and it's too long) · unsureAnswersAreHiddenNotShown · secondRunIsServedFromCache
        for (tier in route.tiers) {
            TODO("LAB-2: try each tier in order, record an Attempt, stop at the first valid answer.")
        }
""",
    "LAB-3": """\
    // LAB-3 · Guard. The schema guarantees the shape, not the truth. Return every reason this
    // extraction is unacceptable; an empty list means "ship it". The reasons are also sent back to
    // the model as a repair prompt, so make them specific, e.g. "actions[0].evidence is not a quote from the message".
    //
    // Rules (output.actions is a List<ActionItem>: type, title, date?, time?, amount?, evidence):
    //   1. at most 3 actions                                          rejectsMoreThanThreeItems
    //   2. date, if present, parses with LocalDate.parse  (yyyy-MM-dd)  rejectsDatesThatAreNotIso
    //      time, if present, parses with LocalTime.parse  (HH:mm)
    //   3. evidence is a verbatim quote from input.fullText            rejectsEvidenceThatIsNotAQuote
    //   4. amount, if present, literally appears in input.fullText     rejectsAnAmountThatIsNotInTheMessage
    //   (and a grounded item passes: acceptsAGroundedItem)
    //
    // Compare Text.normalize(input.fullText) with Text.normalize(...) of the quote, never raw strings:
    // whitespace and punctuation differ. runCatching { LocalDate.parse(it) }.isFailure is the date check.
    // Shape:  = buildList { if (output.actions.size > 3) add("more than 3 actions"); output.actions.forEachIndexed { i, a -> ... } }
    override fun validate(input: Message, output: Extraction): List<String> =
        TODO("LAB-3: reject anything the message does not literally support.")
""",
    "LAB-4": """\
    // LAB-4 · Nudge (stretch). Choose the banners at the top of the inbox. Proactive means it
    // interrupts, so be stingy.
    //
    // extracted maps each message to its extracted actions (ActionItem: type, title, date?, ...).
    //   1. skip ActionType.DELIVERY: a parcel never becomes a banner     deliveriesNeverNag
    //   2. skip actions without a date (LocalDate.parse; ignore bad ones)
    //   3. daysLeft = now.date.daysUntil(date); keep 0..horizonDays
    //   4. sort by daysLeft, then PAYMENT before anything else on ties
    //   5. take(max)                                                       surfacesAtMostTwoNudgesSoonestFirst
    // Shape:  extracted.flatMap { (message, actions) -> actions.mapNotNull { ... Nudge(message, action, days) } }
    //             .sortedWith(compareBy<Nudge> { it.daysLeft }.thenBy { ... }).take(max)
    fun nudges(now: LocalDateTime, extracted: Map<Message, List<ActionItem>>, horizonDays: Int = 3, max: Int = 2): List<Nudge> =
        TODO("LAB-4: at most two nudges, soonest first, never for deliveries.")
""",
}

REGION = re.compile(r"^[ \t]*// region (LAB-\d)\n.*?^[ \t]*// endregion\n", re.S | re.M)


def strip(root, only):
    for path in root.rglob("*.kt"):
        if "/build/" in str(path):
            continue
        text = path.read_text()
        new = REGION.sub(lambda m: STUBS[m.group(1)] if (only is None or m.group(1) in only) else m.group(0), text)
        if new != text:
            path.write_text(new)
            print(f"stubbed {path.relative_to(root)}")


if __name__ == "__main__":
    root = pathlib.Path(sys.argv[1]).resolve()
    labs = set(sys.argv[2:]) or None
    strip(root, labs)
