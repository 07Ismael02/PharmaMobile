import SwiftUI

@main
struct iOSApp: App {
    init() {
        KoinIosKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
