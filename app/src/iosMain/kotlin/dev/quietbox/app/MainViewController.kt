package dev.quietbox.app

import androidx.compose.ui.window.ComposeUIViewController
import dev.quietbox.app.di.FirebaseConfig
import dev.quietbox.app.di.PlatformConfig
import dev.quietbox.app.di.initKoin
import platform.Foundation.NSBundle
import platform.Foundation.NSDictionary
import platform.Foundation.dictionaryWithContentsOfFile
import platform.UIKit.UIViewController

/** Called once from Swift, before the first view controller. */
fun startQuietBox(engine: String?, proxyUrl: String?, token: String?) =
    initKoin(PlatformConfig(engine, firebaseConfig(), proxyUrl, token))

/**
 * [still] puts the app in a named state for slide screenshots, e.g. "xray" or "detail:m15".
 * Simulators can't be tapped from a script, so this is how the iOS stills are made.
 */
fun MainViewController(still: String? = null): UIViewController = ComposeUIViewController {
    QuietBoxApp(startWithXray = still == "xray", initialOpenId = still?.removePrefix("detail:")?.takeIf { still.startsWith("detail:") })
}

// GoogleService-Info.plist, the file the Firebase console hands out for the iOS app.
private fun firebaseConfig(): FirebaseConfig? {
    val path = NSBundle.mainBundle.pathForResource("GoogleService-Info", "plist") ?: return null
    val plist = NSDictionary.dictionaryWithContentsOfFile(path) ?: return null
    return FirebaseConfig(
        projectId = plist["PROJECT_ID"] as? String ?: return null,
        apiKey = plist["API_KEY"] as? String ?: return null,
        appId = plist["GOOGLE_APP_ID"] as? String,
    )
}
