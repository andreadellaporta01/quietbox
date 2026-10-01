package dev.quietbox.core.ai

import kotlin.time.Duration

enum class Tier { CACHE, ON_DEVICE, CLOUD }

enum class Privacy { LOCAL_ONLY, CLOUD_OK }

enum class Trigger { BACKGROUND, ON_OPEN }

enum class Effort { LOW, MEDIUM, HIGH }

data class TaskSpec(
    val name: String,
    val promptVersion: Int,
    val trigger: Trigger,
    val privacy: Privacy,
    val latencyBudget: Duration,
    val maxOutputTokens: Int,
    val cloudEffort: Effort,
    val escalateBelow: Double,
    val showAbove: Double,
)
