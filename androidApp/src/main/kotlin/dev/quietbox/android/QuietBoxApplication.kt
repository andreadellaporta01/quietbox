package dev.quietbox.android

import android.app.Application
import dev.quietbox.app.di.FirebaseConfig
import dev.quietbox.app.di.PlatformConfig
import dev.quietbox.app.di.initKoin

class QuietBoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(PlatformConfig(BuildConfig.ENGINE, firebaseConfig(), BuildConfig.PROXY_URL, BuildConfig.TOKEN))
    }

    // Written by the google-services plugin from google-services.json.
    private fun firebaseConfig() = FirebaseConfig(
        projectId = getString(R.string.project_id),
        apiKey = getString(R.string.google_api_key),
        appId = getString(R.string.google_app_id),
    )
}
