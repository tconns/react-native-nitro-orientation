//
//  HybridNitroOrientation.swift
//  Pods
//
//  Created by tconns94 on 8/21/2025.
//

import UIKit
import Foundation
import React
import NitroModules

class NitroOrientation: HybridNitroOrientationSpec {
    private final class WeakUIView {
        weak var value: UIView?
        
        init(_ value: UIView?) {
            self.value = value
        }
    }

    private struct ZoneState {
        var angleDeg: Double
        var orientation: String
        var attachedViews: Set<Int>
        var sequence: Int64
        var updatedAtMs: Int64
        var lastEmitAt: TimeInterval
    }

    private var uiOrientationListener: (String) -> Void = { _ in }
    private var deviceOrientationListener: (String) -> Void = { _ in }
    private var lockListener: (String) -> Void = { _ in }
    private var lockOrientation: String = "unknown"
    private var isLockedValue = false
    private var zoneListener: (String) -> Void = { _ in }
    private var zoneStates: [String: ZoneState] = [:]
    private var zoneSequence: Int64 = 0
    private var zoneHostViews: [Int: WeakUIView] = [:]
    private var lastUiEmitAt: TimeInterval = 0
    private var lastDeviceEmitAt: TimeInterval = 0
    private let minEmitInterval: TimeInterval = 0.12

    func lockToLandscape() throws {
        lockTo(.landscapeLeft, orientationName: "landscapeLeft")
    }
    
    func getAutoRotateState() throws -> Bool {
        return true
    }
    
    private var lastOrientation: String = "unknown"
    private var lastDeviceOrientation: String = "unknown"
    
    private var orientationObserver: NSObjectProtocol?
    
    private func now() -> TimeInterval {
        ProcessInfo.processInfo.systemUptime
    }
    
    private func canEmit(lastAt: TimeInterval) -> Bool {
        now() - lastAt >= minEmitInterval
    }
    
    private func nowMs() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
    
    private func runOnMainSync<T>(_ work: () -> T) -> T {
        if Thread.isMainThread {
            return work()
        }
        return DispatchQueue.main.sync(execute: work)
    }
    
    override init() {
        super.init()
        NitroOrientationZoneRegistry.shared.setCallbacks { [weak self] zoneId, hostView, isMounted in
            guard let self else { return }
            let hostKey = Int(bitPattern: Unmanaged.passUnretained(hostView).toOpaque())
            self.zoneHostViews[hostKey] = WeakUIView(hostView)
            if isMounted {
                self.registerZoneHost(zoneId: zoneId, nativeViewTag: Double(hostKey))
            } else {
                self.unregisterZoneHost(zoneId: zoneId, nativeViewTag: Double(hostKey))
                self.zoneHostViews.removeValue(forKey: hostKey)
            }
        }
        
        // Seed initial state so JS queries are meaningful
        let initialState = runOnMainSync {
            return (
                mapUIOrientation(),
                mapDeviceOrientation(UIDevice.current.orientation)
            )
        }
        lastOrientation = initialState.0
        lastDeviceOrientation = initialState.1

        // Lắng nghe thay đổi UI orientation
        orientationObserver = NotificationCenter.default.addObserver(
            forName: UIDevice.orientationDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleOrientationChange()
        }
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
    }
    
    private func handleOrientationChange() {
        let device = UIDevice.current.orientation
        let deviceOrientation = mapDeviceOrientation(device)
        if deviceOrientation != lastDeviceOrientation && canEmit(lastAt: lastDeviceEmitAt) {
            lastDeviceOrientation = deviceOrientation
            lastDeviceEmitAt = now()
            sendEvent("deviceOrientationDidChange", orientation: deviceOrientation)
        }
        
        let uiOrientation = mapUIOrientation()
        if uiOrientation != lastOrientation && canEmit(lastAt: lastUiEmitAt) {
            lastOrientation = uiOrientation
            lastUiEmitAt = now()
            sendEvent("orientationDidChange", orientation: uiOrientation)
        }
    }
    
    private func mapDeviceOrientation(_ orientation: UIDeviceOrientation) -> String {
        switch orientation {
        case .portrait: return "portrait"
        case .portraitUpsideDown: return "portraitUpsideDown"
        case .landscapeLeft: return "landscapeLeft"
        case .landscapeRight: return "landscapeRight"
        default: return "unknown"
        }
    }
    
    private func mapUIOrientation() -> String {
        let foregroundScene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        
        let fallbackScene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first
        
        guard let orientation = (foregroundScene ?? fallbackScene)?.interfaceOrientation else {
            return "unknown"
        }
        
        switch orientation {
        case .portrait: return "portrait"
        case .portraitUpsideDown: return "portraitUpsideDown"
        case .landscapeLeft: return "landscapeLeft"
        case .landscapeRight: return "landscapeRight"
        default: return "unknown"
        }
    }
    
    func sendEvent(_ name: String, orientation: String) {
        switch name {
        case "orientationDidChange":
            uiOrientationListener(orientation)
        case "deviceOrientationDidChange":
            deviceOrientationListener(orientation)
        case "lockDidChange":
            lockListener(orientation)
        default:
            break
        }
    }
    
    // ---- API public cho JS ----

    func setChangeListener(listener: @escaping (String) -> Void) {
        uiOrientationListener = listener
    }

    func setDeviceOrientationListener(listener: @escaping (String) -> Void) {
        deviceOrientationListener = listener
    }

    func setLockListener(listener: @escaping (String) -> Void) {
        lockListener = listener
    }

     func getOrientation() -> String {
        return lastOrientation
    }
    
     func getDeviceOrientation() -> String {
        return lastDeviceOrientation
    }

    func getLockOrientation() -> String {
        return lockOrientation
    }

    func isLocked() -> Bool {
        return isLockedValue
    }
    
     func lockToPortrait() {
        lockTo(.portrait, orientationName: "portrait")
    }
    
     func lockToLandscapeLeft() {
        lockTo(.landscapeLeft, orientationName: "landscapeLeft")
    }
    
     func lockToLandscapeRight() {
        lockTo(.landscapeRight, orientationName: "landscapeRight")
    }
    
     func lockToPortraitUpsideDown() {
        lockTo(.portraitUpsideDown, orientationName: "portraitUpsideDown")
    }
    
     func unlockAllOrientations() {
        isLockedValue = false
        lockOrientation = "unknown"
        requestOrientation(.all, orientationName: "unknown")
    }
    
    private func lockTo(_ mask: UIInterfaceOrientationMask, orientationName: String) {
        requestOrientation(mask, orientationName: orientationName)
    }
    
    private func requestOrientation(_ mask: UIInterfaceOrientationMask, orientationName: String) {
        guard Thread.isMainThread else {
            DispatchQueue.main.async { [weak self] in
                self?.requestOrientation(mask, orientationName: orientationName)
            }
            return
        }
        
        if #available(iOS 16.0, *),
           let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }) {
            scene.requestGeometryUpdate(.iOS(interfaceOrientations: mask)) { error in
                if error == nil {
                    self.lastOrientation = orientationName
                    self.lockOrientation = orientationName
                    self.isLockedValue = orientationName != "unknown"
                    self.lastUiEmitAt = self.now()
                    self.sendEvent("orientationDidChange", orientation: orientationName)
                    self.sendEvent("lockDidChange", orientation: orientationName)
                } else {
                    print("[NitroOrientation] requestGeometryUpdate failed: \(error.localizedDescription)")
                }
            }
        } else {
            // On iOS < 16 this is best-effort and depends on host app orientation handling.
            self.lastOrientation = orientationName
            self.lockOrientation = orientationName
            self.isLockedValue = orientationName != "unknown"
            self.lastUiEmitAt = self.now()
            self.sendEvent("orientationDidChange", orientation: orientationName)
            self.sendEvent("lockDidChange", orientation: orientationName)
        }
    }
    
    func release() {
        if let obs = orientationObserver {
            NotificationCenter.default.removeObserver(obs)
        }
        UIDevice.current.endGeneratingDeviceOrientationNotifications()
        uiOrientationListener = { _ in }
        deviceOrientationListener = { _ in }
        lockListener = { _ in }
        zoneListener = { _ in }
        zoneStates.removeAll()
        zoneHostViews.removeAll()
        NitroOrientationZoneRegistry.shared.setCallbacks { _, _, _ in }
    }
    
    private func normalizeOrientationForZone(_ orientation: String) -> String {
        switch orientation {
        case "portrait", "portraitUpsideDown", "landscapeLeft", "landscapeRight":
            return orientation
        default:
            return "unknown"
        }
    }
    
    private func angleForOrientation(_ orientation: String) -> Double {
        switch orientation {
        case "portrait": return 0
        case "landscapeLeft": return -90
        case "landscapeRight": return 90
        case "portraitUpsideDown": return 180
        default: return 0
        }
    }
    
    private func viewFromTag(_ tag: Int) -> UIView? {
        if let hostView = zoneHostViews[tag]?.value {
            return hostView
        }
        if let keyWindow = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive })?
            .windows.first(where: { $0.isKeyWindow }) {
            return keyWindow.viewWithTag(tag)
        }
        return UIApplication.shared.windows.first?.viewWithTag(tag)
    }
    
    private func applyZoneTransform(_ zoneId: String, animated: Bool, durationMs: Double = 160) {
        guard let state = zoneStates[zoneId] else { return }
        let radians = state.angleDeg * .pi / 180.0
        for tag in state.attachedViews {
            guard let view = viewFromTag(tag) else { continue }
            if animated {
                UIView.animate(withDuration: max(0, durationMs) / 1000.0) {
                    view.transform = CGAffineTransform(rotationAngle: radians)
                }
            } else {
                view.transform = CGAffineTransform(rotationAngle: radians)
            }
        }
    }
    
    private func emitZoneEvent(zoneId: String, source: String, animationState: String = "idle", force: Bool = false) {
        guard var state = zoneStates[zoneId] else { return }
        if !force && !canEmit(lastAt: state.lastEmitAt) {
            return
        }
        state.lastEmitAt = now()
        zoneStates[zoneId] = state
        let payload: [String: Any] = [
            "source": source,
            "animationState": animationState,
            "snapshot": [
                "zoneId": zoneId,
                "angleDeg": state.angleDeg,
                "orientation": state.orientation,
                "attachedViews": state.attachedViews.count,
                "sequence": state.sequence,
                "updatedAt": state.updatedAtMs
            ]
        ]
        if let data = try? JSONSerialization.data(withJSONObject: payload),
           let json = String(data: data, encoding: .utf8) {
            zoneListener(json)
        }
    }
    
    private func upsertZone(_ zoneId: String) -> ZoneState {
        if let state = zoneStates[zoneId] {
            return state
        }
        let state = ZoneState(
            angleDeg: 0,
            orientation: "portrait",
            attachedViews: [],
            sequence: 0,
            updatedAtMs: nowMs(),
            lastEmitAt: 0
        )
        zoneStates[zoneId] = state
        return state
    }
    
    func createZone(zoneId: String, options: String) {
        _ = options
        guard !zoneId.isEmpty else { return }
        _ = upsertZone(zoneId)
        emitZoneEvent(zoneId: zoneId, source: "create", force: true)
    }
    
    func registerZoneHost(zoneId: String, nativeViewTag: Double) {
        guard !zoneId.isEmpty else { return }
        var state = upsertZone(zoneId)
        let tag = Int(nativeViewTag)
        state.attachedViews.insert(tag)
        zoneSequence += 1
        state.sequence = zoneSequence
        state.updatedAtMs = nowMs()
        zoneStates[zoneId] = state
        applyZoneTransform(zoneId, animated: false)
        emitZoneEvent(zoneId: zoneId, source: "mount", force: true)
    }
    
    func unregisterZoneHost(zoneId: String, nativeViewTag: Double) {
        guard var state = zoneStates[zoneId] else { return }
        let tag = Int(nativeViewTag)
        state.attachedViews.remove(tag)
        zoneSequence += 1
        state.sequence = zoneSequence
        state.updatedAtMs = nowMs()
        zoneStates[zoneId] = state
        emitZoneEvent(zoneId: zoneId, source: "unmount", force: true)
    }
    
    func destroyZone(zoneId: String) {
        guard let state = zoneStates[zoneId] else { return }
        for tag in state.attachedViews {
            viewFromTag(tag)?.transform = .identity
            zoneHostViews.removeValue(forKey: tag)
        }
        zoneStates.removeValue(forKey: zoneId)
    }
    
    func setZoneRotation(zoneId: String, angleDeg: Double, options: String) {
        var state = upsertZone(zoneId)
        if abs(state.angleDeg - angleDeg) < 0.1 {
            return
        }
        var animated = false
        var durationMs = 160.0
        if let data = options.data(using: .utf8),
           let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            animated = (object["animated"] as? Bool) ?? false
            durationMs = (object["durationMs"] as? Double) ?? 160.0
        }
        state.angleDeg = angleDeg
        zoneSequence += 1
        state.sequence = zoneSequence
        state.updatedAtMs = nowMs()
        zoneStates[zoneId] = state
        applyZoneTransform(zoneId, animated: animated, durationMs: durationMs)
        emitZoneEvent(
            zoneId: zoneId,
            source: "update",
            animationState: animated ? "running" : "idle"
        )
    }
    
    func setZoneOrientation(zoneId: String, orientation: String) {
        let normalized = normalizeOrientationForZone(orientation)
        var state = upsertZone(zoneId)
        state.orientation = normalized
        state.angleDeg = angleForOrientation(normalized)
        zoneSequence += 1
        state.sequence = zoneSequence
        state.updatedAtMs = nowMs()
        zoneStates[zoneId] = state
        applyZoneTransform(zoneId, animated: false)
        emitZoneEvent(zoneId: zoneId, source: "update")
    }
    
    func resetZoneRotation(zoneId: String) {
        var state = upsertZone(zoneId)
        state.angleDeg = 0
        state.orientation = "portrait"
        zoneSequence += 1
        state.sequence = zoneSequence
        state.updatedAtMs = nowMs()
        zoneStates[zoneId] = state
        applyZoneTransform(zoneId, animated: false)
        emitZoneEvent(zoneId: zoneId, source: "reset", force: true)
    }
    
    func getZoneSnapshot(zoneId: String) -> String {
        guard let state = zoneStates[zoneId] else { return "{}" }
        let payload: [String: Any] = [
            "zoneId": zoneId,
            "angleDeg": state.angleDeg,
            "orientation": state.orientation,
            "attachedViews": state.attachedViews.count,
            "sequence": state.sequence,
            "updatedAt": state.updatedAtMs
        ]
        if let data = try? JSONSerialization.data(withJSONObject: payload),
           let json = String(data: data, encoding: .utf8) {
            return json
        }
        return "{}"
    }
    
    func getAllZoneSnapshots() -> String {
        let snapshots: [[String: Any]] = zoneStates.map { zoneId, state in
            [
                "zoneId": zoneId,
                "angleDeg": state.angleDeg,
                "orientation": state.orientation,
                "attachedViews": state.attachedViews.count,
                "sequence": state.sequence,
                "updatedAt": state.updatedAtMs
            ]
        }
        if let data = try? JSONSerialization.data(withJSONObject: snapshots),
           let json = String(data: data, encoding: .utf8) {
            return json
        }
        return "[]"
    }
    
    func setZoneListener(listener: @escaping (String) -> Void) {
        zoneListener = listener
    } 
}
