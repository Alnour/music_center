import AVFoundation
import AVKit
import SwiftUI

/// Apple's AirPlay menu button, the same control video apps show. Tapping it lists the
/// TVs on the network; picking one sends the board clip there. `onMenuClosed` runs when
/// the menu goes away, so the app can claim the TV even when iOS reports no route change
/// because another app was already streaming to that same TV.
struct AirPlayPickerButton: UIViewRepresentable {
    let onMenuClosed: @MainActor () -> Void

    func makeUIView(context: Context) -> AVRoutePickerView {
        let picker = AVRoutePickerView()
        picker.prioritizesVideoDevices = true
        picker.tintColor = .tintColor
        picker.activeTintColor = .systemGreen
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {
        context.coordinator.onMenuClosed = onMenuClosed
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onMenuClosed: onMenuClosed)
    }

    /// The delegate protocol is not main-actor isolated, but AVKit calls it on the main
    /// thread, so the callback hops back onto the main actor before touching the closure.
    @MainActor
    final class Coordinator: NSObject, AVRoutePickerViewDelegate {
        var onMenuClosed: @MainActor () -> Void

        init(onMenuClosed: @escaping @MainActor () -> Void) {
            self.onMenuClosed = onMenuClosed
        }

        nonisolated func routePickerViewDidEndPresentingRoutes(
            _ routePickerView: AVRoutePickerView
        ) {
            MainActor.assumeIsolated { onMenuClosed() }
        }
    }
}

/// A small preview of the clip being cast, so the player can see what the TV shows.
struct ClipPreview: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> PlayerLayerView {
        let view = PlayerLayerView()
        view.playerLayer.player = player
        view.playerLayer.videoGravity = .resizeAspect
        return view
    }

    func updateUIView(_ uiView: PlayerLayerView, context: Context) {
        guard UIApplication.shared.applicationState != .background else { return }
        uiView.playerLayer.player = player
    }

    /// Hosts the player's picture. iOS pauses a player that is still attached to a view
    /// when the app leaves the foreground, which would stop the TV too, so the view lets
    /// go of the player in the background and takes it back on return.
    final class PlayerLayerView: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }

        private var detachedPlayer: AVPlayer?
        private var observers: [NSObjectProtocol] = []

        var playerLayer: AVPlayerLayer {
            guard let layer = layer as? AVPlayerLayer else {
                fatalError("layerClass guarantees an AVPlayerLayer")
            }
            return layer
        }

        override init(frame: CGRect) {
            super.init(frame: frame)
            let center = NotificationCenter.default
            observers.append(
                center.addObserver(
                    forName: UIApplication.didEnterBackgroundNotification,
                    object: nil,
                    queue: .main
                ) { [weak self] _ in
                    MainActor.assumeIsolated { self?.detachPlayer() }
                }
            )
            observers.append(
                center.addObserver(
                    forName: UIApplication.willEnterForegroundNotification,
                    object: nil,
                    queue: .main
                ) { [weak self] _ in
                    MainActor.assumeIsolated { self?.reattachPlayer() }
                }
            )
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("PlayerLayerView is created in code only")
        }

        private func detachPlayer() {
            detachedPlayer = playerLayer.player
            playerLayer.player = nil
        }

        private func reattachPlayer() {
            guard let detachedPlayer else { return }
            playerLayer.player = detachedPlayer
            self.detachedPlayer = nil
        }
    }
}
