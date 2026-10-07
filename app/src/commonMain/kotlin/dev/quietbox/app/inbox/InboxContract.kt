package dev.quietbox.app.inbox

import dev.quietbox.core.engine.Nudge
import dev.quietbox.core.engine.Row
import dev.quietbox.core.tasks.Category

object InboxContract {

    data class State(
        val sorting: Boolean = false,
        val rows: List<Row> = emptyList(),
        val nudges: List<Nudge> = emptyList(),
    ) {
        /** Newest first inside each section, sections in the order the user cares about. */
        val sections: List<Section>
            get() {
                val byCategory = rows.sortedByDescending { it.message.receivedAt }.groupBy { it.triage.value?.category }
                return SECTION_ORDER.mapNotNull { category -> byCategory[category]?.let { Section(category, it) } }
            }
    }

    data class Section(val category: Category?, val rows: List<Row>)

    sealed interface Intent {
        data object Refresh : Intent
        data class Open(val messageId: String) : Intent
    }

    /** What happened, as data. The reducer is the only place that turns it into State. */
    sealed interface Result {
        data object SortStarted : Result
        data class Sorted(val rows: List<Row>, val nudges: List<Nudge>) : Result
    }

    sealed interface Effect {
        data class OpenMessage(val messageId: String) : Effect
    }

    private val SECTION_ORDER = listOf(Category.ACTION, Category.REPLY, null, Category.FYI, Category.NOISE)
}
