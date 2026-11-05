import SwiftUI
import UIKit

/// Reports whether the phone screen is being mirrored or recorded.
///
/// iOS exposes this as a trait on views rather than as a notification, so a zero-size
/// UIKit view sits in the hierarchy and listens for the trait to change. AirPlay screen
/// mirroring sets it, which is how the app knows the TV is at least showing a copy of
/// the phone even when iOS never offers a separate TV screen.
struct CaptureStateReader: UIViewRepresentable {
    let onChange: (Bool) -> Void

    func makeUIView(context: Context) -> CaptureStateView {
        let view = CaptureStateView()
        view.onChange = onChange
        return view
    }

    func updateUIView(_ uiView: CaptureStateView, context: Context) {
        uiView.onChange = onChange
    }

    final class CaptureStateView: UIView {
        var onChange: ((Bool) -> Void)?

        override init(frame: CGRect) {
            super.init(frame: frame)
            isUserInteractionEnabled = false
            registerForTraitChanges([UITraitSceneCaptureState.self]) {
                (view: CaptureStateView, _: UITraitCollection) in
                view.report()
            }
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("CaptureStateView is created in code only")
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            report()
        }

        private func report() {
            let isCaptured = traitCollection.sceneCaptureState == .active
            onChange?(isCaptured)
        }
    }
}
