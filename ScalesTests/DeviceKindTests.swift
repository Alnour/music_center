import Testing

@testable import Scales

/// The app names the device in its sentences and changes a few behaviours on the Mac.
@MainActor
struct DeviceKindTests {
    @Test func theMacBuildKnowsItIsOnAMac() {
        #if targetEnvironment(macCatalyst)
            #expect(DeviceKind.current == .mac)
            #expect(!OrientationLock.canRotateScreen, "a Mac window never rotates")
        #else
            #expect(DeviceKind.current != .mac)
        #endif
    }

    @Test func devicesAreNamedTheWayPlayersSayThem() {
        #expect(DeviceKind.phone.name == "phone")
        #expect(DeviceKind.pad.name == "iPad")
        #expect(DeviceKind.mac.name == "Mac")
    }
}
