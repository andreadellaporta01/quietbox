package dev.quietbox.app.xray

import androidx.lifecycle.viewModelScope
import dev.quietbox.app.mvi.MviViewModel
import dev.quietbox.app.session.AiSession
import dev.quietbox.app.xray.XRayContract.Effect
import dev.quietbox.app.xray.XRayContract.Intent
import dev.quietbox.app.xray.XRayContract.Result
import dev.quietbox.app.xray.XRayContract.State
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class XRayViewModel(private val session: AiSession) :
    MviViewModel<State, Intent, Result, Effect>(State(engine = session.engineName, tokenLimit = session.budget.limit)) {

    init {
        viewModelScope.launch {
            combine(session.environment, session.telemetry.spans, session.budget.spent) { env, spans, spent ->
                Result.Observed(env, spans, spent)
            }.collect(::dispatch)
        }
    }

    override fun onIntent(intent: Intent) {
        when (intent) {
            Intent.ToggleOnline -> session.update { it.copy(conditions = it.conditions.copy(online = !it.conditions.online)) }
            Intent.ToggleBatteryLow -> session.update { it.copy(conditions = it.conditions.copy(batteryLow = !it.conditions.batteryLow)) }
            Intent.ToggleChaos -> session.update { it.copy(chaos = !it.chaos) }
            Intent.Reset -> session.reset()
        }
    }

    override fun reduce(state: State, result: Result): State = XRayReducer(state, result)
}

object XRayReducer : (State, Result) -> State {
    override fun invoke(state: State, result: Result): State = when (result) {
        is Result.Observed -> state.copy(environment = result.environment, spans = result.spans, tokensSpent = result.tokensSpent)
    }
}
