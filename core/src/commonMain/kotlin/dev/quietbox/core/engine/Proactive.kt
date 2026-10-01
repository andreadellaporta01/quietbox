package dev.quietbox.core.engine

import dev.quietbox.core.inbox.Message
import dev.quietbox.core.tasks.ActionItem
import dev.quietbox.core.tasks.ActionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil

data class Nudge(val message: Message, val action: ActionItem, val daysLeft: Int)

object Proactive {

    // region LAB-4
    fun nudges(now: LocalDateTime, extracted: Map<Message, List<ActionItem>>, horizonDays: Int = 3, max: Int = 2): List<Nudge> =
        extracted.flatMap { (message, actions) ->
            actions.mapNotNull { action ->
                if (action.type == ActionType.DELIVERY) return@mapNotNull null
                val date = action.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                val days = now.date.daysUntil(date)
                if (days in 0..horizonDays) Nudge(message, action, days) else null
            }
        }
            .sortedWith(compareBy<Nudge> { it.daysLeft }.thenBy { if (it.action.type == ActionType.PAYMENT) 0 else 1 })
            .take(max)
    // endregion
}
