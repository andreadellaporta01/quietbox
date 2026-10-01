import SwiftUI
import QuietBoxKit

@main
struct QuietBoxApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let env = ProcessInfo.processInfo.environment
        return MainViewControllerKt.MainViewController(
            engine: env["QUIETBOX_ENGINE"],
            proxyUrl: env["QUIETBOX_PROXY_URL"],
            token: env["QUIETBOX_TOKEN"]
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
