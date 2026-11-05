import AVFoundation
import CoreGraphics
import CoreMedia
import CoreVideo
import OSLog

/// Turns a still image of the board into a short, ordinary-looking H.264 movie.
///
/// AirPlay can send an app's video to a TV without screen mirroring, but only video: a
/// receiver such as a Roku has no way to show a plain image. So the board is wrapped in a
/// ten-second clip that the caster plays on a loop. The clip deliberately looks like any
/// phone video (30 frames a second, a silent AAC audio track) because TV decoders reject
/// unusual files; a one-frame-per-second clip without audio played on the phone but never
/// appeared on the Roku. Identical frames compress to almost nothing, so the file stays
/// small and is written in well under a second.
enum BoardVideoWriter {
    static let frameRate: Int32 = 30
    static let clipSeconds: Int32 = 10
    static let audioSampleRate: Double = 44_100
    static let audioFramesPerBuffer = 1024
    /// A clip takes about a second to write; anything far beyond that means the encoder is
    /// stuck, and failing lets the caster report it instead of waiting forever.
    static let timeLimitSeconds: Double = 20

    enum WriterError: Error {
        case pixelBufferUnavailable
        case audioBufferUnavailable
        case writerFailed(String)
    }

    nonisolated static func write(_ image: CGImage, to url: URL) throws {
        try? FileManager.default.removeItem(at: url)
        let writer = try AVAssetWriter(outputURL: url, fileType: .mp4)
        writer.shouldOptimizeForNetworkUse = true
        let videoInput = makeVideoInput(width: image.width, height: image.height, writer: writer)
        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: videoInput,
            sourcePixelBufferAttributes: [
                kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
                kCVPixelBufferWidthKey as String: image.width,
                kCVPixelBufferHeightKey as String: image.height,
            ]
        )
        let audioInput = makeAudioInput()
        writer.add(videoInput)
        writer.add(audioInput)
        guard writer.startWriting() else {
            throw WriterError.writerFailed(writer.error?.localizedDescription ?? "start")
        }
        writer.startSession(atSourceTime: .zero)

        let pixelBuffer = try makePixelBuffer(from: image, pool: adaptor.pixelBufferPool)
        let silence = try makeSilenceSource()
        let videoFeeder = TrackFeeder(input: videoInput, unitCount: Int(clipSeconds * frameRate)) {
            frameIndex in
            let time = CMTime(value: CMTimeValue(frameIndex), timescale: frameRate)
            return adaptor.append(pixelBuffer, withPresentationTime: time)
        }
        let totalAudioFrames = Int(audioSampleRate) * Int(clipSeconds)
        let audioBufferCount = (totalAudioFrames + audioFramesPerBuffer - 1) / audioFramesPerBuffer
        let audioFeeder = TrackFeeder(input: audioInput, unitCount: audioBufferCount) {
            bufferIndex in
            let startFrame = bufferIndex * audioFramesPerBuffer
            let frames = min(audioFramesPerBuffer, totalAudioFrames - startFrame)
            guard
                let buffer = try? makeSilentBuffer(
                    frames: frames,
                    startFrame: startFrame,
                    format: silence
                )
            else { return false }
            return audioInput.append(buffer)
        }

        // Each track feeds itself on its own queue whenever the writer asks for more, which
        // lets the writer interleave them however it likes. Feeding both from one loop
        // deadlocked: the writer held back video until it had more audio, while the loop
        // was still waiting on video.
        let group = DispatchGroup()
        videoFeeder.start(on: DispatchQueue(label: "BoardVideoWriter.video"), group: group)
        audioFeeder.start(on: DispatchQueue(label: "BoardVideoWriter.audio"), group: group)
        guard group.wait(timeout: .now() + timeLimitSeconds) == .success else {
            writer.cancelWriting()
            throw WriterError.writerFailed("timed out feeding the clip")
        }
        guard !videoFeeder.failed, !audioFeeder.failed else {
            writer.cancelWriting()
            throw WriterError.writerFailed(writer.error?.localizedDescription ?? "append failed")
        }

        let finished = DispatchSemaphore(value: 0)
        writer.finishWriting { finished.signal() }
        guard finished.wait(timeout: .now() + timeLimitSeconds) == .success else {
            writer.cancelWriting()
            throw WriterError.writerFailed("timed out finishing the clip")
        }
        guard writer.status == .completed else {
            throw WriterError.writerFailed(writer.error?.localizedDescription ?? "finish")
        }
    }

    /// Encodes at a fixed quality when the device's encoder supports it: every frame then
    /// comes out equally sharp and the file stays small (about 120 KB per ten seconds).
    /// At a fixed bit rate the encoder instead spends its whole budget polishing an
    /// unchanging picture, starting each repeat a little soft, which pulses on the TV.
    /// The bit-rate setting remains as a fallback for encoders without a quality setting.
    private nonisolated static func makeVideoInput(
        width: Int,
        height: Int,
        writer: AVAssetWriter
    ) -> AVAssetWriterInput {
        let qualitySettings = videoSettings(
            width: width,
            height: height,
            rateControl: [AVVideoQualityKey: 0.7]
        )
        let bitRateSettings = videoSettings(
            width: width,
            height: height,
            rateControl: [AVVideoAverageBitRateKey: 400_000]
        )
        let usesQuality = writer.canApply(outputSettings: qualitySettings, forMediaType: .video)
        let settings = usesQuality ? qualitySettings : bitRateSettings
        let logger = Logger(subsystem: "com.alnourkhalifa.Scales", category: "Encoder")
        logger.notice(
            "Encoding with \(usesQuality ? "fixed quality" : "fixed bit rate", privacy: .public)"
        )
        let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
        input.expectsMediaDataInRealTime = false
        return input
    }

    private nonisolated static func videoSettings(
        width: Int,
        height: Int,
        rateControl: [String: Any]
    ) -> [String: Any] {
        var compression: [String: Any] = [
            AVVideoProfileLevelKey: AVVideoProfileLevelH264MainAutoLevel,
            // One full picture per clip keeps the file small (every other frame is a
            // tiny "nothing changed" frame) and makes each repeated copy start on it.
            AVVideoMaxKeyFrameIntervalKey: Int(clipSeconds * frameRate),
            AVVideoAllowFrameReorderingKey: false,
        ]
        compression.merge(rateControl) { _, new in new }
        return [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width,
            AVVideoHeightKey: height,
            AVVideoCompressionPropertiesKey: compression,
        ]
    }

    nonisolated static func makeAudioInput() -> AVAssetWriterInput {
        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatMPEG4AAC,
            AVSampleRateKey: audioSampleRate,
            AVNumberOfChannelsKey: 1,
            AVEncoderBitRateKey: 32_000,
        ]
        let input = AVAssetWriterInput(mediaType: .audio, outputSettings: settings)
        input.expectsMediaDataInRealTime = false
        return input
    }

    /// Describes the silent 16-bit mono PCM the audio encoder is fed.
    nonisolated static func makeSilenceSource() throws -> CMAudioFormatDescription {
        var description = AudioStreamBasicDescription(
            mSampleRate: audioSampleRate,
            mFormatID: kAudioFormatLinearPCM,
            mFormatFlags: kAudioFormatFlagIsSignedInteger | kAudioFormatFlagIsPacked,
            mBytesPerPacket: 2,
            mFramesPerPacket: 1,
            mBytesPerFrame: 2,
            mChannelsPerFrame: 1,
            mBitsPerChannel: 16,
            mReserved: 0
        )
        var formatDescription: CMAudioFormatDescription?
        CMAudioFormatDescriptionCreate(
            allocator: nil,
            asbd: &description,
            layoutSize: 0,
            layout: nil,
            magicCookieSize: 0,
            magicCookie: nil,
            extensions: nil,
            formatDescriptionOut: &formatDescription
        )
        guard let formatDescription else { throw WriterError.audioBufferUnavailable }
        return formatDescription
    }

    nonisolated static func makeSilentBuffer(
        frames: Int,
        startFrame: Int,
        format: CMAudioFormatDescription
    ) throws -> CMSampleBuffer {
        let byteCount = frames * 2
        var blockBuffer: CMBlockBuffer?
        CMBlockBufferCreateWithMemoryBlock(
            allocator: nil,
            memoryBlock: nil,
            blockLength: byteCount,
            blockAllocator: nil,
            customBlockSource: nil,
            offsetToData: 0,
            dataLength: byteCount,
            flags: 0,
            blockBufferOut: &blockBuffer
        )
        guard let blockBuffer else { throw WriterError.audioBufferUnavailable }
        CMBlockBufferFillDataBytes(
            with: 0,
            blockBuffer: blockBuffer,
            offsetIntoDestination: 0,
            dataLength: byteCount
        )

        let sampleRate = Int32(audioSampleRate)
        var timing = CMSampleTimingInfo(
            duration: CMTime(value: 1, timescale: sampleRate),
            presentationTimeStamp: CMTime(value: CMTimeValue(startFrame), timescale: sampleRate),
            decodeTimeStamp: .invalid
        )
        var sampleBuffer: CMSampleBuffer?
        CMSampleBufferCreate(
            allocator: nil,
            dataBuffer: blockBuffer,
            dataReady: true,
            makeDataReadyCallback: nil,
            refcon: nil,
            formatDescription: format,
            sampleCount: frames,
            sampleTimingEntryCount: 1,
            sampleTimingArray: &timing,
            sampleSizeEntryCount: 0,
            sampleSizeArray: nil,
            sampleBufferOut: &sampleBuffer
        )
        guard let sampleBuffer else { throw WriterError.audioBufferUnavailable }
        return sampleBuffer
    }

    /// Draws the image into a buffer the encoder can consume directly.
    private nonisolated static func makePixelBuffer(
        from image: CGImage,
        pool: CVPixelBufferPool?
    ) throws -> CVPixelBuffer {
        var buffer: CVPixelBuffer?
        if let pool {
            CVPixelBufferPoolCreatePixelBuffer(nil, pool, &buffer)
        } else {
            CVPixelBufferCreate(
                nil,
                image.width,
                image.height,
                kCVPixelFormatType_32BGRA,
                nil,
                &buffer
            )
        }
        guard let buffer else { throw WriterError.pixelBufferUnavailable }

        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }
        let context = CGContext(
            data: CVPixelBufferGetBaseAddress(buffer),
            width: image.width,
            height: image.height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue
                | CGBitmapInfo.byteOrder32Little.rawValue
        )
        guard let context else { throw WriterError.pixelBufferUnavailable }
        let frame = CGRect(x: 0, y: 0, width: image.width, height: image.height)
        context.draw(image, in: frame)
        return buffer
    }
}

/// Feeds one writer input from its own serial queue whenever the writer asks for data.
///
/// The writer calls back repeatedly; each call appends units (video frames or audio
/// buffers) until the input is full or the track is complete, then marks it finished.
/// All mutable state is touched only on the feeder's queue, which is why the unchecked
/// Sendable conformance is safe.
final class TrackFeeder: @unchecked Sendable {
    private let input: AVAssetWriterInput
    private let unitCount: Int
    private let appendUnit: (Int) -> Bool
    private var nextUnit = 0
    private var isFinished = false
    private(set) var failed = false

    init(input: AVAssetWriterInput, unitCount: Int, appendUnit: @escaping (Int) -> Bool) {
        self.input = input
        self.unitCount = unitCount
        self.appendUnit = appendUnit
    }

    func start(on queue: DispatchQueue, group: DispatchGroup) {
        group.enter()
        input.requestMediaDataWhenReady(on: queue) { [self] in
            feed(group: group)
        }
    }

    private func feed(group: DispatchGroup) {
        guard !isFinished else { return }
        while input.isReadyForMoreMediaData, nextUnit < unitCount {
            if !appendUnit(nextUnit) {
                failed = true
                break
            }
            nextUnit += 1
        }
        guard nextUnit >= unitCount || failed else { return }
        isFinished = true
        input.markAsFinished()
        group.leave()
    }
}
