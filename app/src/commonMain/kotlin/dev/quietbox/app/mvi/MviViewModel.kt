package dev.quietbox.app.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One direction only: the screen sends an [Intent], the ViewModel turns it into [Result]s
 * (synchronously or from coroutines), and a pure [reduce] folds each Result into the next [State].
 * One-shot things the screen must do once (navigate, show a toast) go out as [Effect]s.
 */
abstract class MviViewModel<State, Intent, Result, Effect>(initial: State) : ViewModel() {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    /** The only entry point from the UI. */
    abstract fun onIntent(intent: Intent)

    /** Pure: no I/O, no coroutines, no clock. Unit-tested on its own. */
    protected abstract fun reduce(state: State, result: Result): State

    protected fun dispatch(result: Result) = _state.update { reduce(it, result) }

    protected fun emit(effect: Effect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
