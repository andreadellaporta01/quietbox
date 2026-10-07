import SwiftUI
import QuietBoxKit

@main
struct QuietBoxApp: App {
    init() {
        let env = ProcessInfo.processInfo.environment
        MainViewControllerKt.startQuietBox(
            engine: env["QUIETBOX_ENGINE"],
            proxyUrl: env["QUIETBOX_PROXY_URL"],
            token: env["QUIETBOX_TOKEN"]
        )
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(still: ProcessInfo.processInfo.environment["QUIETBOX_STILL"])
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
