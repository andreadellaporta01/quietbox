package dev.quietbox.app.di

import dev.quietbox.app.detail.DetailViewModel
import dev.quietbox.app.inbox.InboxViewModel
import dev.quietbox.app.session.AiSession
import dev.quietbox.app.session.PipelineSession
import dev.quietbox.app.xray.XRayViewModel
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.inbox.Fixtures
import kotlinx.datetime.LocalDateTime
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** What each platform knows and common code doesn't: its config file and launch arguments. */
data class PlatformConfig(
    val engine: String?,
    val firebase: FirebaseConfig?,
    val proxyUrl: String? = null,
    val token: String? = null,
)

val ENGINE_NAME = named("engineName")

/** The cloud model: Firebase by default, mock or proxy when asked for. */
fun cloudModule(config: PlatformConfig) = module {
    val (name, cloud) = Engines.create(config.engine, config.firebase, config.proxyUrl, config.token)
    single<CloudModel> { cloud }
    single(ENGINE_NAME) { name }
}

/** The inbox the workshop runs on, and the fixed "now" its dates are written against. */
val dataModule = module {
    single<AiSession> {
        PipelineSession(
            cloud = get(),
            engineName = get(ENGINE_NAME),
            messages = Fixtures.inbox,
            history = Fixtures.threadContext,
            now = { LocalDateTime(2026, 10, 7, 18, 0) },
        )
    }
}

val presentationModule = module {
    viewModelOf(::InboxViewModel)
    viewModelOf(::XRayViewModel)
    viewModel { (messageId: String) -> DetailViewModel(messageId, get()) }
}

fun appModules(config: PlatformConfig) = listOf(cloudModule(config), dataModule, presentationModule)
