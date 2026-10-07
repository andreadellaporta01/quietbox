package dev.quietbox.core.eval

import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.ai.Pipeline
import dev.quietbox.core.ai.Privacy
import dev.quietbox.core.ai.Tier
import dev.quietbox.core.ai.TokenBudget
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.engine.InboxEngine
import dev.quietbox.core.inbox.Fixtures
import dev.quietbox.core.local.Sensitivity
import dev.quietbox.core.telemetry.Outcome
import dev.quietbox.core.telemetry.Telemetry
import dev.quietbox.core.tasks.ActionItem

data class Scoreboard(
    val engine: String,
    val triageAccuracy: Double,
    val urgentAccuracy: Double,
    val extractPrecision: Double,
    val extractRecall: Double,
    val shownWrong: Int,
    val privacyLeaks: Int,
    val cloudShare: Double,
    val cloudTokens: Int,
    val p50Ms: Long,
    val p95Ms: Long,
    val failures: Int,
    /** Why cloud attempts failed, e.g. "unauthorized" → 31. A 0% cloud share is only a result if this is empty. */
    val cloudErrors: Map<String, Int> = emptyMap(),
) {
    fun render(): String = buildString {
        appendLine("── QuietBox eval · engine=$engine · ${Fixtures.all.size} messages ──")
        appendLine("triage accuracy        ${triageAccuracy.pct()}")
        appendLine("urgent accuracy        ${urgentAccuracy.pct()}")
        appendLine("extraction precision   ${extractPrecision.pct()}")
        appendLine("extraction recall      ${extractRecall.pct()}")
        appendLine("wrong items shown      $shownWrong")
        appendLine("privacy leaks          $privacyLeaks${if (privacyLeaks > 0) "   ← must be 0" else ""}")
        appendLine("served by cloud        ${cloudShare.pct()}")
        appendLine("cloud tokens           $cloudTokens")
        appendLine("latency p50 / p95      ${p50Ms} ms / ${p95Ms} ms")
        append("hard failures          $failures")
        if (cloudErrors.isNotEmpty()) {
            appendLine()
            append("cloud errors           ${cloudErrors.entries.sortedByDescending { it.value }.joinToString(" · ") { "${it.value} × ${it.key}" }}")
            hint()?.let { appendLine(); append("                       ← $it") }
        }
    }

    /** The one line that tells you whether you measured the model or your setup. */
    private fun hint(): String? = when {
        "unauthorized" in cloudErrors -> "the cloud refused every call: check App Check is Unenforced for Firebase AI Logic"
        "rate_limited" in cloudErrors -> "free-tier quota: the numbers above are partly the device's, not the model's"
        "offline" in cloudErrors -> "no network to the model"
        else -> null
    }
}

object Eval {

    suspend fun run(cloud: CloudModel, conditions: Conditions = Conditions(), tokenLimit: Int = 50_000): Scoreboard {
        val recorder = Recorder(cloud)
        val telemetry = Telemetry(keep = 1_000)
        val pipeline = Pipeline(recorder, telemetry, TokenBudget(tokenLimit), conditions)
        val rows = InboxEngine(pipeline, Fixtures.inbox, Fixtures.threadContext).sweep()

        var triageHits = 0
        var urgentHits = 0
        var predicted = 0
        var truePositives = 0
        var expected = 0
        var shownWrong = 0
        var acceptedShown = 0
        rows.forEach { row ->
            val golden = Fixtures.goldenFor(row.message) ?: return@forEach
            val triage = row.triage.value
            if (triage?.category == golden.category) triageHits++ else if (triage != null) shownWrong++
            if (triage?.urgent == golden.urgent) urgentHits++
            val got = row.extraction.value?.actions.orEmpty()
            predicted += got.size
            expected += golden.actions.size
            truePositives += golden.actions.count { want -> got.any { it.matches(want) } }
            val acceptable = got.count { g -> golden.actions.any { it.matches(g) } || (g.type to g.date) in golden.alsoOk }
            shownWrong += got.size - acceptable
            acceptedShown += acceptable
        }

        val sensitive = Fixtures.inbox.filter { Sensitivity.of(it) == Privacy.LOCAL_ONLY }.map { it.subject }
        val leaks = recorder.seen.count { request -> sensitive.any { request.input.contains(it) } }

        val spans = telemetry.spans.value
        val latencies = spans.map { it.totalMs }.sorted()
        val n = Fixtures.all.size.toDouble()
        return Scoreboard(
            engine = if (conditions.online) cloud.name else "offline",
            triageAccuracy = triageHits / n,
            urgentAccuracy = urgentHits / n,
            extractPrecision = if (predicted == 0) 1.0 else acceptedShown.toDouble() / predicted,
            extractRecall = if (expected == 0) 1.0 else truePositives.toDouble() / expected,
            shownWrong = shownWrong,
            privacyLeaks = leaks,
            cloudShare = spans.count { it.servedBy == Tier.CLOUD }.toDouble() / spans.size,
            cloudTokens = telemetry.cloudTokens,
            p50Ms = latencies.percentile(0.5),
            p95Ms = latencies.percentile(0.95),
            failures = spans.count { it.outcome == Outcome.FAILED },
            cloudErrors = spans.flatMap { it.attempts }
                .filter { it.tier == Tier.CLOUD && !it.ok }
                .groupingBy { it.note.substringBefore(" after").substringBefore(":").trim() }
                .eachCount(),
        )
    }

    private fun ActionItem.matches(other: ActionItem) = type == other.type && date == other.date

    private fun List<Long>.percentile(p: Double): Long = if (isEmpty()) 0 else this[((size - 1) * p).toInt()]
}

private fun Double.pct() = "${(this * 1000).toInt() / 10.0}%"
