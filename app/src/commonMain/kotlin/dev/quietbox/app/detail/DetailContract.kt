package dev.quietbox.app.detail

import dev.quietbox.core.engine.Row
import dev.quietbox.core.tasks.Replies
import dev.quietbox.core.tasks.Summary

object DetailContract {

    data class State(
        val row: Row? = null,
        val loading: Boolean = true,
        val summary: Summary? = null,
        val replies: Replies? = null,
    )

    sealed interface Intent {
        data object Back : Intent
        data class PickReply(val text: String) : Intent
    }

    sealed interface Result {
        data class Loaded(val row: Row?) : Result
        data class Opened(val summary: Summary?, val replies: Replies?) : Result
    }

    sealed interface Effect {
        data object Close : Effect
        data class Send(val text: String) : Effect
    }
}
