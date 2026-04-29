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

    private var uiOrientationListener: (String) -> Void = { _ in }
    private var deviceOrientationListener: (String) -> Void = { _ in }
    private var lockListener: (String) -> Void = { _ in }
    private var lockOrientation: String = "unknown"
    private var isLockedValue = false
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
    
    private func runOnMainSync<T>(_ work: () -> T) -> T {
        if Thread.isMainThread {
            return work()
        }
        return DispatchQueue.main.sync(execute: work)
    }
    
    override init() {
        super.init()
        
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
    } 
}
