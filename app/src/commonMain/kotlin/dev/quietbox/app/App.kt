package dev.quietbox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quietbox.app.detail.DetailContract
import dev.quietbox.app.detail.DetailScreen
import dev.quietbox.app.detail.DetailViewModel
import dev.quietbox.app.inbox.InboxContract
import dev.quietbox.app.inbox.InboxScreen
import dev.quietbox.app.inbox.InboxViewModel
import dev.quietbox.app.ui.Palette
import dev.quietbox.app.ui.QuietTheme
import dev.quietbox.app.xray.XRayScreen
import dev.quietbox.app.xray.XRayViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Navigation is UI state, not AI state, so it lives here and survives configuration changes.
 * [initialOpenId] and [startWithXray] exist for slide screenshots; the app itself starts empty.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuietBoxApp(startWithXray: Boolean = false, initialOpenId: String? = null) {
    var openId by rememberSaveable { mutableStateOf(initialOpenId) }
    var xray by rememberSaveable { mutableStateOf(startWithXray) }
    // System back closes the x-ray, then the open message, before it closes the app.
    BackHandler(enabled = xray || openId != null) { if (xray) xray = false else openId = null }
    QuietBoxApp(openId, xray, onOpen = { openId = it }, onClose = { openId = null }, onToggleXray = { xray = !xray })
}

@Composable
fun QuietBoxApp(openId: String?, xray: Boolean, onOpen: (String) -> Unit, onClose: () -> Unit, onToggleXray: () -> Unit) {
    val inbox = koinViewModel<InboxViewModel>()
    val inboxState by inbox.state.collectAsStateWithLifecycle()
    LaunchedEffect(inbox) {
        inbox.effects.collect { effect ->
            when (effect) {
                is InboxContract.Effect.OpenMessage -> onOpen(effect.messageId)
            }
        }
    }
    // Survives a trip into Detail and back, so the inbox keeps its scroll position.
    val inboxScroll = rememberLazyListState()

    QuietTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(Palette.paper).safeDrawingPadding()) {
            val wide = maxWidth > 900.dp
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Header(sorting = inboxState.sorting, onXray = onToggleXray)
                    when {
                        // On a phone there is no room for a side panel: the x-ray takes over the screen.
                        xray && !wide -> XRay(Modifier.fillMaxSize())
                        openId != null -> Detail(openId, onClose, Modifier.fillMaxSize())
                        else -> InboxScreen(inboxState, inbox::onIntent, Modifier.fillMaxSize(), inboxScroll)
                    }
                }
                if (xray && wide) XRay(Modifier.fillMaxHeight().width(460.dp))
            }
        }
    }
}

@Composable
private fun Detail(messageId: String, onClose: () -> Unit, modifier: Modifier) {
    // Keyed by message: a new message gets a new ViewModel, and leaving cancels its model calls.
    val vm = koinViewModel<DetailViewModel>(key = "detail-$messageId") { parametersOf(messageId) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                DetailContract.Effect.Close -> onClose()
                is DetailContract.Effect.Send -> onClose()
            }
        }
    }
    DetailScreen(state, vm::onIntent, modifier)
}

@Composable
private fun XRay(modifier: Modifier) {
    val vm = koinViewModel<XRayViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    XRayScreen(state, vm::onIntent, modifier)
}

@Composable
private fun Header(sorting: Boolean, onXray: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "QuietBox",
            style = TextStyle(brush = Brush.linearGradient(listOf(Palette.accent, Palette.warm))),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (sorting) Text("sorting…", fontSize = 12.sp, color = Palette.muted, modifier = Modifier.padding(end = 12.dp))
        Text("x-ray", fontSize = 12.sp, color = Palette.accent, modifier = Modifier.clickable(onClick = onXray))
    }
}
