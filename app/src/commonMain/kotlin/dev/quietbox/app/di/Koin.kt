package dev.quietbox.app.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/** Called once per process by each platform's entry point. */
fun initKoin(config: PlatformConfig) {
    stopKoin()
    startKoin { modules(appModules(config)) }
}
