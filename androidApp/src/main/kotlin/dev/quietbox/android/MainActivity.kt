package dev.quietbox.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.quietbox.app.Engines
import dev.quietbox.app.QuietBoxApp
import dev.quietbox.app.QuietBoxState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val state = remember {
                val (name, cloud) = Engines.create(BuildConfig.ENGINE, BuildConfig.PROXY_URL, BuildConfig.TOKEN)
                QuietBoxState(scope, cloud, name).also { it.toggleXray() }
            }
            QuietBoxApp(state)
        }
    }
}
