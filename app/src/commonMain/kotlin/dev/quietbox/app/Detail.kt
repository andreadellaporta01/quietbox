package dev.quietbox.app

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
import dev.quietbox.core.engine.Opened
import dev.quietbox.core.engine.Row as InboxRow

@Composable
fun Detail(row: InboxRow?, opened: Opened?, opening: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val message = row?.message ?: return
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("← Inbox", color = Palette.accent, modifier = Modifier.clickable(onClick = onBack))
        Text(message.subject, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
        Text("${message.from} · ${message.receivedAt.date} ${message.receivedAt.time.toString().take(5)}", fontSize = 12.sp, color = Palette.muted)

        opened?.summary?.value?.let { summary ->
            Text(
                summary.tldr,
                modifier = Modifier.fillMaxWidth()
                    .background(Palette.accent.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                fontSize = 14.sp,
                color = Palette.ink,
            )
        }

        row.extraction.value?.actions?.forEach { ActionChip(it) }

        Text(message.body, fontSize = 15.sp, color = Palette.ink, lineHeight = 22.sp)

        opened?.replies?.value?.let { replies ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                replies.options.forEach { option ->
                    Text(
                        option,
                        modifier = Modifier
                            .border(1.dp, Palette.accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = Palette.accent,
                    )
                }
            }
        }
        if (opening) Text("·  ·  ·", color = Palette.line)
    }
}
