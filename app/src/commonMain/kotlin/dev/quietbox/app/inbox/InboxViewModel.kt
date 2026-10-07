package dev.quietbox.app.inbox

import androidx.lifecycle.viewModelScope
import dev.quietbox.app.inbox.InboxContract.Effect
import dev.quietbox.app.inbox.InboxContract.Intent
import dev.quietbox.app.inbox.InboxContract.Result
import dev.quietbox.app.inbox.InboxContract.State
import dev.quietbox.app.mvi.MviViewModel
import dev.quietbox.app.session.AiSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class InboxViewModel(private val session: AiSession) : MviViewModel<State, Intent, Result, Effect>(State()) {

    private var sorting: Job? = null

    init {
        onIntent(Intent.Refresh)
        // A new environment (offline, chaos, reset) means a new sort.
        viewModelScope.launch { session.generation.drop(1).collect { onIntent(Intent.Refresh) } }
    }

    override fun onIntent(intent: Intent) {
        when (intent) {
            Intent.Refresh -> sort()
            is Intent.Open -> emit(Effect.OpenMessage(intent.messageId))
        }
    }

    private fun sort() {
        sorting?.cancel()
        dispatch(Result.SortStarted)
        sorting = viewModelScope.launch {
            val rows = session.sweep()
            dispatch(Result.Sorted(rows, session.nudges(rows)))
        }
    }

    override fun reduce(state: State, result: Result): State = InboxReducer(state, result)
}

/** Pure, so the interesting rules are testable without a coroutine in sight. */
object InboxReducer : (State, Result) -> State {
    override fun invoke(state: State, result: Result): State = when (result) {
        // Keep the old rows on screen while we re-sort: nothing jumps, nothing blinks.
        Result.SortStarted -> state.copy(sorting = true)
        is Result.Sorted -> state.copy(sorting = false, rows = result.rows, nudges = result.nudges)
    }
}
