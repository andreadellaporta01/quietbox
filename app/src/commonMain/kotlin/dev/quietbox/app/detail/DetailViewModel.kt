package dev.quietbox.app.detail

import androidx.lifecycle.viewModelScope
import dev.quietbox.app.detail.DetailContract.Effect
import dev.quietbox.app.detail.DetailContract.Intent
import dev.quietbox.app.detail.DetailContract.Result
import dev.quietbox.app.detail.DetailContract.State
import dev.quietbox.app.mvi.MviViewModel
import dev.quietbox.app.session.AiSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Summary and replies are user-initiated work: they run when the message is opened and are
 * cancelled with the ViewModel when the user leaves, so a slow model never answers into the void.
 */
class DetailViewModel(messageId: String, private val session: AiSession) :
    MviViewModel<State, Intent, Result, Effect>(State()) {

    init {
        viewModelScope.launch {
            // Opened from the inbox the row is already there; deep-linked, wait for the first sort.
            val row = session.rows.first { rows -> rows.isNotEmpty() }.firstOrNull { it.message.id == messageId }
            dispatch(Result.Loaded(row))
            if (row != null) {
                val opened = session.open(row.message, row.triage.value)
                dispatch(Result.Opened(opened.summary?.value, opened.replies?.value))
            }
        }
    }

    override fun onIntent(intent: Intent) {
        when (intent) {
            Intent.Back -> emit(Effect.Close)
            is Intent.PickReply -> emit(Effect.Send(intent.text))
        }
    }

    override fun reduce(state: State, result: Result): State = DetailReducer(state, result)
}

object DetailReducer : (State, Result) -> State {
    override fun invoke(state: State, result: Result): State = when (result) {
        is Result.Loaded -> state.copy(row = result.row, loading = result.row != null)
        // A hidden or failed result arrives as null: the screen simply shows less.
        is Result.Opened -> state.copy(loading = false, summary = result.summary, replies = result.replies)
    }
}
