import AVFoundation
import Foundation

/// Repeats a short board clip into one continuous five-minute clip, without re-encoding.
///
/// Over AirPlay, restarting a clip means asking the TV to seek back to the start, and the
/// Roku sometimes drops the whole session when that happens, so clips run five minutes.
/// Encoding five minutes of frames would take far too long; instead the short clip's
/// compressed frames are written thirty times over with new timestamps. Each copy starts
/// on a full picture (a keyframe), so the joins decode cleanly.
///
/// The result is a single unbroken stream. An earlier version stitched thirty copies
/// together with an edit list, and the silent audio's lead-in at every join made the
/// picture stutter on the TV every ten seconds. The silent audio is now written fresh
/// for the whole length instead of being copied.
enum ClipExtender {
    static let repetitions = 30

    enum ExtenderError: Error {
        case noVideoTrack
        case readFailed(String)
        case writeFailed(String)
    }

    static func extend(_ shortClip: URL, to output: URL) async throws {
        let asset = AVURLAsset(url: shortClip)
        let pieceDuration = try await asset.load(.duration)
        guard let videoTrack = try await asset.loadTracks(withMediaType: .video).first else {
            throw ExtenderError.noVideoTrack
        }
        let formatHint = try await videoTrack.load(.formatDescriptions).first
        let frames = try readCompressedFrames(of: videoTrack, in: asset)
        try writeRepeated(
            frames,
            formatHint: formatHint,
            pieceDuration: pieceDuration,
            to: output
        )
    }

    /// Reads the clip's frames exactly as they are stored, still compressed.
    private nonisolated static func readCompressedFrames(
        of track: AVAssetTrack,
        in asset: AVAsset
    ) throws -> [CMSampleBuffer] {
        let reader = try AVAssetReader(asset: asset)
        let output = AVAssetReaderTrackOutput(track: track, outputSettings: nil)
        output.alwaysCopiesSampleData = false
        reader.add(output)
        guard reader.startReading() else {
            throw ExtenderError.readFailed(reader.error?.localizedDescription ?? "start")
        }
        var frames: [CMSampleBuffer] = []
        while let buffer = output.copyNextSampleBuffer() {
            if CMSampleBufferGetNumSamples(buffer) > 0 {
                frames.append(buffer)
            }
        }
        guard reader.status == .completed else {
            throw ExtenderError.readFailed(reader.error?.localizedDescription ?? "read")
        }
        return frames
    }

    private nonisolated static func writeRepeated(
        _ frames: [CMSampleBuffer],
        formatHint: CMFormatDescription?,
        pieceDuration: CMTime,
        to output: URL
    ) throws {
        try? FileManager.default.removeItem(at: output)
        let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
        // Puts the file's index first so the TV can start playing before the whole file
        // has arrived over Wi-Fi.
        writer.shouldOptimizeForNetworkUse = true
        let videoInput = AVAssetWriterInput(
            mediaType: .video,
            outputSettings: nil,
            sourceFormatHint: formatHint
        )
        videoInput.expectsMediaDataInRealTime = false
        let audioInput = BoardVideoWriter.makeAudioInput()
        writer.add(videoInput)
        writer.add(audioInput)
        guard writer.startWriting() else {
            throw ExtenderError.writeFailed(writer.error?.localizedDescription ?? "start")
        }
        writer.startSession(atSourceTime: .zero)

        let videoFeeder = TrackFeeder(
            input: videoInput,
            unitCount: frames.count * repetitions
        ) { unit in
            let repetition = unit / frames.count
            let frame = frames[unit % frames.count]
            let offset = CMTimeMultiply(pieceDuration, multiplier: Int32(repetition))
            guard let shifted = shifted(frame, by: offset) else { return false }
            return videoInput.append(shifted)
        }

        let silence = try BoardVideoWriter.makeSilenceSource()
        let framesPerBuffer = BoardVideoWriter.audioFramesPerBuffer
        let sampleRate = BoardVideoWriter.audioSampleRate
        let totalSeconds = pieceDuration.seconds * Double(repetitions)
        let totalAudioFrames = Int(totalSeconds * sampleRate)
        let audioBufferCount = (totalAudioFrames + framesPerBuffer - 1) / framesPerBuffer
        let audioFeeder = TrackFeeder(input: audioInput, unitCount: audioBufferCount) {
            bufferIndex in
            let startFrame = bufferIndex * framesPerBuffer
            let frameCount = min(framesPerBuffer, totalAudioFrames - startFrame)
            guard
                let buffer = try? BoardVideoWriter.makeSilentBuffer(
                    frames: frameCount,
                    startFrame: startFrame,
                    format: silence
                )
            else { return false }
            return audioInput.append(buffer)
        }

        let group = DispatchGroup()
        videoFeeder.start(on: DispatchQueue(label: "ClipExtender.video"), group: group)
        audioFeeder.start(on: DispatchQueue(label: "ClipExtender.audio"), group: group)
        let timeLimit = BoardVideoWriter.timeLimitSeconds
        guard group.wait(timeout: .now() + timeLimit) == .success else {
            writer.cancelWriting()
            throw ExtenderError.writeFailed("timed out writing the long clip")
        }
        guard !videoFeeder.failed, !audioFeeder.failed else {
            writer.cancelWriting()
            throw ExtenderError.writeFailed(writer.error?.localizedDescription ?? "append")
        }

        let finished = DispatchSemaphore(value: 0)
        writer.finishWriting { finished.signal() }
        guard finished.wait(timeout: .now() + timeLimit) == .success else {
            writer.cancelWriting()
            throw ExtenderError.writeFailed("timed out finishing the long clip")
        }
        guard writer.status == .completed else {
            throw ExtenderError.writeFailed(writer.error?.localizedDescription ?? "finish")
        }
    }

    /// A copy of a compressed frame moved later in time by the given offset.
    private nonisolated static func shifted(
        _ frame: CMSampleBuffer,
        by offset: CMTime
    ) -> CMSampleBuffer? {
        var timingCount: CMItemCount = 0
        CMSampleBufferGetSampleTimingInfoArray(
            frame,
            entryCount: 0,
            arrayToFill: nil,
            entriesNeededOut: &timingCount
        )
        var timings = [CMSampleTimingInfo](repeating: CMSampleTimingInfo(), count: timingCount)
        CMSampleBufferGetSampleTimingInfoArray(
            frame,
            entryCount: timingCount,
            arrayToFill: &timings,
            entriesNeededOut: &timingCount
        )
        for index in timings.indices {
            timings[index].presentationTimeStamp = timings[index].presentationTimeStamp + offset
            if timings[index].decodeTimeStamp.isValid {
                timings[index].decodeTimeStamp = timings[index].decodeTimeStamp + offset
            }
        }
        var copy: CMSampleBuffer?
        CMSampleBufferCreateCopyWithNewTiming(
            allocator: nil,
            sampleBuffer: frame,
            sampleTimingEntryCount: timingCount,
            sampleTimingArray: &timings,
            sampleBufferOut: &copy
        )
        return copy
    }
}
