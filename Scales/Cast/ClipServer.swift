// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Network
import OSLog

/// A small HTTP server that hands the board clips to the TV.
///
/// AirPlay video works by giving the TV a web address to fetch, and iOS can no longer
/// AirPlay a video file that only exists on the phone (playback fails with "external
/// playback not supported for asset"). So the phone serves its clips itself, on its Wi-Fi
/// address, and the player is given that address instead of the file. The server speaks
/// just enough HTTP for a media player: GET and HEAD, byte ranges, and the MP4 type.
@MainActor
final class ClipServer {
    private(set) var port: UInt16?
    private var listener: NWListener?
    private var clips: [String: Data] = [:]
    private var connections: [ObjectIdentifier: NWConnection] = [:]
    private var permissionProbe: NWBrowser?
    private let logger = Logger(subsystem: "com.alnourkhalifa.Scales", category: "ClipServer")

    func start() {
        guard listener == nil else { return }
        do {
            let parameters = NWParameters.tcp
            parameters.allowLocalEndpointReuse = true
            let listener = try NWListener(using: parameters, on: .any)
            listener.newConnectionHandler = { [weak self] connection in
                Task { @MainActor [weak self] in
                    self?.accept(connection)
                }
            }
            listener.stateUpdateHandler = { [weak self] state in
                Task { @MainActor [weak self] in
                    self?.listenerStateChanged(state)
                }
            }
            listener.start(queue: .main)
            self.listener = listener
            askForLocalNetworkAccess()
        } catch {
            logger.error("Could not start: \(error.localizedDescription, privacy: .public)")
        }
    }

    /// Publishes a clip under a name and returns the address the TV can fetch it from,
    /// or nil when the phone has no Wi-Fi address or the server isn't up yet.
    func publish(_ data: Data, named name: String) -> URL? {
        clips = [name: data]
        guard let port, let address = Self.wifiAddress() else { return nil }
        return URL(string: "http://\(address):\(port)/\(name)")
    }

    /// iOS refuses connections from other devices on the Wi-Fi (here, the TV fetching a
    /// clip) until the player allows Local Network access, and it only asks once the app
    /// does something visible on the network. Briefly looking for AirPlay devices makes
    /// iOS ask at launch instead of silently turning the TV away later.
    private func askForLocalNetworkAccess() {
        let browser = NWBrowser(for: .bonjour(type: "_airplay._tcp", domain: nil), using: .tcp)
        browser.start(queue: .main)
        permissionProbe = browser
        Task { [weak self] in
            try? await Task.sleep(for: .seconds(10))
            self?.permissionProbe?.cancel()
            self?.permissionProbe = nil
        }
    }

    private func listenerStateChanged(_ state: NWListener.State) {
        switch state {
        case .ready:
            port = listener?.port?.rawValue
            logger.notice("Serving clips on port \(self.port ?? 0, privacy: .public)")
        case .failed(let error):
            logger.error("Listener failed: \(error.localizedDescription, privacy: .public)")
            listener = nil
            port = nil
        default:
            break
        }
    }

    private func accept(_ connection: NWConnection) {
        let id = ObjectIdentifier(connection)
        connections[id] = connection
        connection.stateUpdateHandler = { [weak self] state in
            if case .cancelled = state {
                Task { @MainActor [weak self] in
                    self?.connections[id] = nil
                }
            }
            if case .failed = state {
                Task { @MainActor [weak self] in
                    self?.connections[id] = nil
                }
            }
        }
        connection.start(queue: .main)
        receiveRequest(on: connection, buffered: Data())
    }

    /// Reads until the end of the request headers, then answers and closes.
    private func receiveRequest(on connection: NWConnection, buffered: Data) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 16_384) {
            [weak self] data, _, isComplete, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                var buffered = buffered
                if let data { buffered.append(data) }
                let headerEnd = Data("\r\n\r\n".utf8)
                if let range = buffered.range(of: headerEnd) {
                    let headerText = String(decoding: buffered[..<range.lowerBound], as: UTF8.self)
                    self.respond(to: HTTPRequest(headerText), on: connection)
                } else if isComplete || error != nil || buffered.count > 16_384 {
                    connection.cancel()
                } else {
                    self.receiveRequest(on: connection, buffered: buffered)
                }
            }
        }
    }

    private func respond(to request: HTTPRequest, on connection: NWConnection) {
        let name = request.path.split(separator: "/").last.map(String.init) ?? ""
        guard request.method == "GET" || request.method == "HEAD", let clip = clips[name] else {
            send(
                status: "404 Not Found",
                headers: ["Content-Length": "0"],
                body: nil,
                on: connection
            )
            return
        }
        var headers = [
            "Content-Type": "video/mp4",
            "Accept-Ranges": "bytes",
            "Connection": "close",
        ]
        var body = clip
        var status = "200 OK"
        if let range = request.byteRange(totalLength: clip.count) {
            body = clip.subdata(in: range)
            status = "206 Partial Content"
            headers["Content-Range"] =
                "bytes \(range.lowerBound)-\(range.upperBound - 1)/\(clip.count)"
        }
        headers["Content-Length"] = String(body.count)
        let payload = request.method == "HEAD" ? nil : body
        send(status: status, headers: headers, body: payload, on: connection)
    }

    private func send(
        status: String,
        headers: [String: String],
        body: Data?,
        on connection: NWConnection
    ) {
        var response = "HTTP/1.1 \(status)\r\n"
        for (name, value) in headers {
            response += "\(name): \(value)\r\n"
        }
        response += "\r\n"
        var payload = Data(response.utf8)
        if let body { payload.append(body) }
        connection.send(
            content: payload,
            completion: .contentProcessed { _ in
                connection.cancel()
            }
        )
    }

    /// The phone's IPv4 address on Wi-Fi (interface en0), which the TV can reach.
    private static func wifiAddress() -> String? {
        var addresses: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&addresses) == 0, let first = addresses else { return nil }
        defer { freeifaddrs(addresses) }
        var found: String?
        var current: UnsafeMutablePointer<ifaddrs>? = first
        while let interface = current {
            let isWifi = String(cString: interface.pointee.ifa_name) == "en0"
            let isIPv4 = interface.pointee.ifa_addr.pointee.sa_family == UInt8(AF_INET)
            if isWifi, isIPv4 {
                var host = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                let length = socklen_t(interface.pointee.ifa_addr.pointee.sa_len)
                let result = getnameinfo(
                    interface.pointee.ifa_addr,
                    length,
                    &host,
                    socklen_t(host.count),
                    nil,
                    0,
                    NI_NUMERICHOST
                )
                if result == 0 {
                    let bytes = host.prefix { $0 != 0 }.map { UInt8(bitPattern: $0) }
                    found = String(decoding: bytes, as: UTF8.self)
                }
            }
            current = interface.pointee.ifa_next
        }
        return found
    }
}

/// The parts of an HTTP request the clip server cares about.
struct HTTPRequest {
    let method: String
    let path: String
    let rangeHeader: String?

    init(_ headerText: String) {
        let lines = headerText.split(separator: "\r\n", omittingEmptySubsequences: true)
        let requestLine = lines.first?.split(separator: " ") ?? []
        method = requestLine.count > 0 ? String(requestLine[0]) : ""
        path = requestLine.count > 1 ? String(requestLine[1]) : ""
        var range: String?
        for line in lines.dropFirst() {
            let parts = line.split(separator: ":", maxSplits: 1)
            guard parts.count == 2, parts[0].lowercased() == "range" else { continue }
            range = parts[1].trimmingCharacters(in: .whitespaces)
        }
        rangeHeader = range
    }

    /// Resolves a "bytes=start-end" header (either end optional) against the clip size.
    func byteRange(totalLength: Int) -> Range<Int>? {
        guard let rangeHeader, rangeHeader.hasPrefix("bytes=") else { return nil }
        let bounds = rangeHeader.dropFirst("bytes=".count).split(
            separator: "-",
            maxSplits: 1,
            omittingEmptySubsequences: false
        )
        guard bounds.count == 2 else { return nil }
        let start = Int(bounds[0])
        let end = Int(bounds[1])
        switch (start, end) {
        case (let start?, let end?):
            guard start <= end, start < totalLength else { return nil }
            return start..<min(end + 1, totalLength)
        case (let start?, nil):
            guard start < totalLength else { return nil }
            return start..<totalLength
        case (nil, let suffixLength?):
            guard suffixLength > 0 else { return nil }
            return max(0, totalLength - suffixLength)..<totalLength
        default:
            return nil
        }
    }
}
