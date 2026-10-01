package dev.quietbox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.quietbox.core.telemetry.Span

@Composable
fun XRay(state: QuietBoxState, ui: UiState, modifier: Modifier = Modifier) {
    val spans by state.telemetry.spans.collectAsState()
    val spent by state.budget.spent.collectAsState()
    Column(modifier.background(Palette.xray).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("AI X-RAY · engine=${state.engineName}", color = Palette.xrayText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Toggle("online", ui.conditions.online) { state.setConditions(ui.conditions.copy(online = !ui.conditions.online)) }
            Toggle("battery low", ui.conditions.batteryLow) { state.setConditions(ui.conditions.copy(batteryLow = !ui.conditions.batteryLow)) }
            Toggle("chaos", ui.chaos) { state.setChaos(!ui.chaos) }
            Toggle("reset", false) { state.reset() }
        }
        Stats(spans, spent, state.budget.limit)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(spans) { SpanRow(it) }
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .background(if (on) Palette.accent else Color(0xFF2A2E37), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun Stats(spans: List<Span>, spent: Int, limit: Int) {
    val byTier = spans.groupingBy { it.servedBy?.name ?: "NONE" }.eachCount()
    val latencies = spans.map { it.totalMs }.sorted()
    val p95 = latencies.getOrNull(((latencies.size - 1) * 0.95).toInt()) ?: 0
    Text(
        "calls ${spans.size}  ${byTier.entries.joinToString("  ") { "${it.key.lowercase()} ${it.value}" }}\n" +
            "tokens $spent / $limit   p95 ${p95}ms",
        color = Palette.xrayText,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
    )
}

@Composable
private fun SpanRow(span: Span) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF1F232B), RoundedCornerShape(6.dp)).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(Palette.of(span.outcome), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                "${span.task}@v${span.promptVersion}",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            Text(span.subject, color = Palette.xrayText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(
                "${span.servedBy?.name?.lowercase() ?: "—"} ${span.totalMs}ms",
                color = Palette.of(span.servedBy),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
        Text(
            buildString {
                append("route: ${span.route}")
                span.attempts.forEach { append("\n  ${if (it.ok) "✓" else "✗"} ${it.tier.name.lowercase()} ${it.latencyMs}ms ${it.note}") }
                if (span.inputTokens + span.outputTokens > 0) append("\n  tokens ${span.inputTokens}→${span.outputTokens}")
                append("\n  ${span.outcome.name.lowercase()}${span.confidence?.let { " conf ${(it * 100).toInt()}%" } ?: ""}")
            },
            color = Palette.xrayText.copy(alpha = 0.75f),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

