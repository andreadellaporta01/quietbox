package dev.quietbox.app

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "QuietBox", state = rememberWindowState(width = 1280.dp, height = 820.dp)) {
        val scope = rememberCoroutineScope()
        val state = remember {
            val (name, cloud) = Engines.create(
                System.getenv("QUIETBOX_ENGINE"),
                System.getenv("QUIETBOX_PROXY_URL"),
                System.getenv("QUIETBOX_TOKEN"),
            )
            QuietBoxState(scope, cloud, name)
        }
        QuietBoxApp(state)
    }
}
