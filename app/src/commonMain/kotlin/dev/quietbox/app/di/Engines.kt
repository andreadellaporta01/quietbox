package dev.quietbox.app.di

import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.FirebaseCloud
import dev.quietbox.core.cloud.MockCloud
import dev.quietbox.core.cloud.ProxyCloud

/** Firebase settings as each platform reads them from its own config file. */
data class FirebaseConfig(val projectId: String, val apiKey: String, val appId: String?)

object Engines {
    fun create(engine: String?, firebase: FirebaseConfig?, proxyUrl: String? = null, token: String? = null): Pair<String, CloudModel> =
        when {
            engine == "mock" -> "mock" to MockCloud()
            engine == "proxy" && !proxyUrl.isNullOrBlank() -> "proxy" to ProxyCloud(proxyUrl, token.orEmpty())
            firebase != null && firebase.apiKey.isNotBlank() ->
                FirebaseCloud.DEFAULT_MODEL to FirebaseCloud(firebase.projectId, firebase.apiKey, appId = firebase.appId)
            else -> "mock" to MockCloud()
        }
}
