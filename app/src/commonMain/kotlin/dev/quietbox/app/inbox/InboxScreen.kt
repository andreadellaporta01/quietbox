package dev.quietbox.app.inbox

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.quietbox.app.ui.ActionChip
import dev.quietbox.app.ui.Palette
import dev.quietbox.core.engine.Nudge
import dev.quietbox.core.engine.Row as InboxRow
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.tasks.Category

@Composable
fun InboxScreen(
    state: InboxContract.State,
    onIntent: (InboxContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val open = { message: Message -> onIntent(InboxContract.Intent.Open(message.id)) }
    LazyColumn(modifier, state = listState) {
        if (state.nudges.isNotEmpty()) {
            item { NudgeStrip(state.nudges, open) }
        }
        state.sections.forEach { section ->
            item(key = "h-${section.category}") { SectionHeader(section.category, section.rows.size) }
            items(section.rows, key = { it.message.id }) { row -> MessageRow(row, open) }
        }
    }
}

@Composable
private fun SectionHeader(category: Category?, count: Int) {
    val label = when (category) {
        Category.ACTION -> "Needs you"
        Category.REPLY -> "Someone's waiting"
        Category.FYI -> "For your information"
        Category.NOISE -> "Quiet"
        null -> "Unsorted"
    }
    Text(
        "$label · $count",
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Palette.muted,
    )
}

@Composable
private fun NudgeStrip(nudges: List<Nudge>, onOpen: (Message) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        nudges.forEach { nudge ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.warm.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable { onOpen(nudge.message) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when (nudge.daysLeft) { 0 -> "Today"; 1 -> "Tomorrow"; else -> "In ${nudge.daysLeft} days" },
                    fontWeight = FontWeight.Bold,
                    color = Palette.warm,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.width(10.dp))
                Text(nudge.action.title, fontSize = 14.sp, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MessageRow(row: InboxRow, onOpen: (Message) -> Unit) {
    val category = row.triage.value?.category
    val muted = category == Category.NOISE
    Column(Modifier.fillMaxWidth().clickable { onOpen(row.message) }.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(category?.let(Palette::of) ?: Palette.line, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                row.message.senderName,
                fontWeight = if (muted) FontWeight.Normal else FontWeight.SemiBold,
                color = if (muted) Palette.muted else Palette.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.triage.value?.urgent == true) {
                Text("urgent", fontSize = 11.sp, color = Palette.warm, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
            }
            Text(row.message.receivedAt.time.toString().take(5), fontSize = 12.sp, color = Palette.muted)
        }
        Text(
            row.message.subject,
            modifier = Modifier.padding(start = 18.dp, top = 2.dp),
            color = if (muted) Palette.muted else Palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        row.extraction.value?.actions?.firstOrNull()?.let { ActionChip(it, Modifier.padding(start = 18.dp, top = 6.dp)) }
    }
    Divider(color = Palette.line, modifier = Modifier.padding(start = 38.dp))
}
