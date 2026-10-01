package dev.quietbox.core.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TokenBudget(val limit: Int) {
    private val used = MutableStateFlow(0)

    val spent: StateFlow<Int> = used
    val hasRoom: Boolean get() = used.value < limit

    fun charge(tokens: Int) = used.update { it + tokens }

    fun reset() = used.update { 0 }
}
