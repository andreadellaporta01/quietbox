package dev.quietbox.app

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(engine: String?, proxyUrl: String?, token: String?): UIViewController = ComposeUIViewController {
    val scope = rememberCoroutineScope()
    val state = remember {
        val (name, cloud) = Engines.create(engine, proxyUrl, token)
        QuietBoxState(scope, cloud, name).also { it.toggleXray() }
    }
    QuietBoxApp(state)
}
