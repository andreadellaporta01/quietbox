package dev.quietbox.app

import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.MockCloud
import dev.quietbox.core.cloud.ProxyCloud

object Engines {
    fun create(engine: String?, proxyUrl: String?, token: String?): Pair<String, CloudModel> =
        if (engine == "proxy" && !proxyUrl.isNullOrBlank()) {
            "proxy" to ProxyCloud(proxyUrl, token.orEmpty())
        } else {
            "mock" to MockCloud()
        }
}
