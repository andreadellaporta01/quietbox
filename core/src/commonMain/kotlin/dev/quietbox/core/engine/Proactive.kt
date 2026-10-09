package dev.quietbox.core.engine

import dev.quietbox.core.inbox.Message
import dev.quietbox.core.tasks.ActionItem
import dev.quietbox.core.tasks.ActionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil

data class Nudge(val message: Message, val action: ActionItem, val daysLeft: Int)

object Proactive {

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
}
