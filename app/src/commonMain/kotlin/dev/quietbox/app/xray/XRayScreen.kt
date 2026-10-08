package dev.quietbox.app.xray

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.quietbox.app.ui.Palette
import dev.quietbox.core.telemetry.Span

@Composable
fun XRayScreen(state: XRayContract.State, onIntent: (XRayContract.Intent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(Palette.xray).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("AI X-RAY · engine=${state.engine}", color = Palette.xrayText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Toggle("online", state.environment.conditions.online) { onIntent(XRayContract.Intent.ToggleOnline) }
            Toggle("battery low", state.environment.conditions.batteryLow) { onIntent(XRayContract.Intent.ToggleBatteryLow) }
            Toggle("chaos", state.environment.chaos) { onIntent(XRayContract.Intent.ToggleChaos) }
            Toggle("reset", false) { onIntent(XRayContract.Intent.Reset) }
        }
        Stats(state)
        Search(state.query) { onIntent(XRayContract.Intent.Search(it)) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            XRayContract.Lens.entries.forEach { lens ->
                Toggle("${lens.label} ${state.lensCounts[lens] ?: 0}", state.lens == lens && lens != XRayContract.Lens.All) {
                    onIntent(XRayContract.Intent.Focus(lens))
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.visible) { SpanRow(it) }
            if (state.visible.isEmpty() && state.spans.isNotEmpty()) {
                item { Text("no span matches", color = Palette.xrayText.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .background(if (on) Palette.accent else Color(0xFF1E1B3A), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun Search(query: String, onChange: (String) -> Unit) {
    val style = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(Palette.accent),
        modifier = Modifier.fillMaxWidth().background(Color(0xFF14132B), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        decorationBox = { field ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("search: blood, rate_limited, timeout…", style = style.copy(color = Palette.xrayText.copy(alpha = 0.5f)))
                    field()
                }
                if (query.isNotEmpty()) Text("✕", color = Palette.xrayText, fontSize = 12.sp, modifier = Modifier.clickable { onChange("") }.padding(start = 8.dp))
            }
        },
    )
}

@Composable
private fun Stats(state: XRayContract.State) {
    Text(
        "calls ${state.spans.size}  ${state.callsByTier.entries.joinToString("  ") { "${it.key} ${it.value}" }}\n" +
            "tokens ${state.tokensSpent} / ${state.tokenLimit}   p95 ${state.p95Ms}ms",
        color = Palette.xrayText,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
    )
}

@Composable
private fun SpanRow(span: Span) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF14132B), RoundedCornerShape(6.dp)).padding(8.dp)) {
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

