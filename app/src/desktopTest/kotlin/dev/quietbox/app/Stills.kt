package dev.quietbox.app

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.quietbox.core.ai.Conditions
import dev.quietbox.core.cloud.MockCloud
import dev.quietbox.core.inbox.Fixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class Stills {

    private val dir = File(System.getProperty("stills.dir")).apply { mkdirs() }

    private fun shoot(name: String, state: QuietBoxState) {
        val scene = ImageComposeScene(1440, 900, Density(1.25f)) { QuietBoxApp(state) }
        repeat(3) { scene.render() }
        val bytes = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
        File(dir, "$name.png").writeBytes(bytes)
        scene.close()
    }

    private suspend fun QuietBoxState.settled() {
        state.first { !it.sweeping && it.rows.isNotEmpty() }
        delay(50.milliseconds)
    }

    @Test
    fun renderFallbackStills() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val online = QuietBoxState(scope, MockCloud(5.milliseconds), "mock")
        online.sweep(); online.settled()
        shoot("01-inbox-online", online)

        online.open(Fixtures.all.first { it.message.id == "m09" }.message)
        online.state.first { !it.opening }
        shoot("02-detail-summary", online)

        online.open(Fixtures.all.first { it.message.id == "m15" }.message)
        online.state.first { !it.opening }
        shoot("03-detail-smart-reply", online)
        online.close()

        val offline = QuietBoxState(scope, MockCloud(5.milliseconds), "mock")
        offline.setConditions(Conditions(online = false)); offline.settled()
        shoot("04-inbox-offline", offline)

        val chaos = QuietBoxState(scope, MockCloud(5.milliseconds), "mock")
        chaos.setChaos(true); chaos.settled()
        shoot("05-inbox-chaos", chaos)
    }
}
