package dev.quietbox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuietBoxApp(state: QuietBoxState) {
    val ui by state.state.collectAsState()
    LaunchedEffect(Unit) { if (state.state.value.rows.isEmpty()) state.sweep() }
    QuietTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(Palette.paper).safeDrawingPadding()) {
            val wide = maxWidth > 900.dp
            // Survives a trip into Detail and back, so the inbox keeps its scroll position.
            val inboxScroll = rememberLazyListState()
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Header(ui, onXray = state::toggleXray)
                    val row = ui.open?.let { open -> ui.rows.firstOrNull { it.message.id == open.id } }
                    when {
                        // On a phone there is no room for a side panel: the x-ray takes over the screen.
                        ui.xray && !wide -> XRay(state, ui, Modifier.fillMaxSize())
                        row != null -> Detail(row, ui.opened, ui.opening, onBack = state::close, modifier = Modifier.fillMaxSize())
                        else -> Inbox(ui.rows, ui.nudges, onOpen = state::open, listState = inboxScroll, modifier = Modifier.fillMaxSize())
                    }
                }
                if (ui.xray && wide) {
                    XRay(state, ui, Modifier.fillMaxHeight().width(460.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(ui: UiState, onXray: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("QuietBox", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (ui.sweeping) Text("sorting…", fontSize = 12.sp, color = Palette.muted, modifier = Modifier.padding(end = 12.dp))
        Text("x-ray", fontSize = 12.sp, color = Palette.accent, modifier = Modifier.clickable(onClick = onXray))
    }
}
