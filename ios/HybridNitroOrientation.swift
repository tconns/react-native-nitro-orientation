//
//  HybridNitroOrientation.swift
//  Pods
//
//  Created by tconns94 on 8/21/2025.
//

import UIKit
import Foundation

class HybridNitroOrientation: HybridNitroOrientationSpec {
    private var lastOrientation: String = "unknown"
    private var lastDeviceOrientation: String = "unknown"
    
    private var orientationObserver: NSObjectProtocol?
    
    override init() {
        super.init()
        
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
        if deviceOrientation != lastDeviceOrientation {
            lastDeviceOrientation = deviceOrientation
            sendEvent("deviceOrientationDidChange", orientation: deviceOrientation)
        }
        
        let uiOrientation = mapUIOrientation()
        if uiOrientation != lastOrientation {
            lastOrientation = uiOrientation
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
        guard let orientation = UIApplication.shared
            .connectedScenes
            .compactMap({ ($0 as? UIWindowScene)?.interfaceOrientation })
            .first else {
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
    
    private func sendEvent(_ name: String, orientation: String) {
        let params = ["orientation": orientation]
        if let bridge = NitroModules.applicationContext {
            bridge.emitDeviceEvent(name, body: params)
        }
    }
    
    // ---- API public cho JS ----
    override func getOrientation() -> String {
        return mapUIOrientation()
    }
    
    override func getDeviceOrientation() -> String {
        return lastDeviceOrientation
    }
    
    override func lockToPortrait() {
        lockTo(.portrait, orientationName: "portrait")
    }
    
    override func lockToLandscapeLeft() {
        lockTo(.landscapeLeft, orientationName: "landscapeLeft")
    }
    
    override func lockToLandscapeRight() {
        lockTo(.landscapeRight, orientationName: "landscapeRight")
    }
    
    override func lockToPortraitUpsideDown() {
        lockTo(.portraitUpsideDown, orientationName: "portraitUpsideDown")
    }
    
    override func unlockAllOrientations() {
        // iOS cần quản lý ở AppDelegate / SceneDelegate
        requestOrientation(.all, orientationName: "unknown")
    }
    
    private func lockTo(_ mask: UIInterfaceOrientationMask, orientationName: String) {
        requestOrientation(mask, orientationName: orientationName)
    }
    
    private func requestOrientation(_ mask: UIInterfaceOrientationMask, orientationName: String) {
        if #available(iOS 16.0, *),
           let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
            scene.requestGeometryUpdate(.iOS(interfaceOrientations: mask)) { error in
                if error == nil {
                    self.lastOrientation = orientationName
                    self.sendEvent("orientationDidChange", orientation: orientationName)
                    self.sendEvent("lockDidChange", orientation: orientationName)
                }
            }
        } else {
            // fallback iOS < 16 → quản lý qua AppDelegate
            // thường phải custom supportedInterfaceOrientations
            self.lastOrientation = orientationName
            self.sendEvent("orientationDidChange", orientation: orientationName)
            self.sendEvent("lockDidChange", orientation: orientationName)
        }
    }
    
    override func release() {
        if let obs = orientationObserver {
            NotificationCenter.default.removeObserver(obs)
        }
        UIDevice.current.endGeneratingDeviceOrientationNotifications()
    }
}
