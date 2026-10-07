package dev.quietbox.app.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.quietbox.app.ui.ActionChip
import dev.quietbox.app.ui.Palette

@Composable
fun DetailScreen(state: DetailContract.State, onIntent: (DetailContract.Intent) -> Unit, modifier: Modifier = Modifier) {
    val row = state.row ?: return
    val message = row.message
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("← Inbox", color = Palette.accent, modifier = Modifier.clickable { onIntent(DetailContract.Intent.Back) })
        Text(message.subject, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
        Text("${message.from} · ${message.receivedAt.date} ${message.receivedAt.time.toString().take(5)}", fontSize = 12.sp, color = Palette.muted)

        state.summary?.let { summary ->
            Text(
                summary.tldr,
                modifier = Modifier.fillMaxWidth()
                    .background(Palette.accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                fontSize = 14.sp,
                color = Palette.ink,
            )
        }

        row.extraction.value?.actions?.forEach { ActionChip(it) }

        Text(message.body, fontSize = 15.sp, color = Palette.ink, lineHeight = 22.sp)

        state.replies?.let { replies ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                replies.options.forEach { option ->
                    Text(
                        option,
                        modifier = Modifier
                            .clickable { onIntent(DetailContract.Intent.PickReply(option)) }
                            .border(1.dp, Palette.accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = Palette.accent,
                    )
                }
            }
        }
        if (state.loading) Text("·  ·  ·", color = Palette.line)
    }
}
