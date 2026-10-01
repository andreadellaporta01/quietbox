package dev.quietbox.core.ai

data class Conditions(
    val online: Boolean = true,
    val batteryLow: Boolean = false,
    val cloudBudgetLeft: Boolean = true,
)
