package dev.quietbox.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.quietbox.core.tasks.ActionItem

@Composable
fun ActionChip(action: ActionItem, modifier: Modifier = Modifier) {
    val parts = listOfNotNull(action.title, action.date, action.time, action.amount)
    Text(
        parts.joinToString(" · "),
        modifier = modifier
            .background(Palette.accent.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 12.sp,
        color = Palette.accent,
    )
}
