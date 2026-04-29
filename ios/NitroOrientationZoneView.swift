import UIKit

@objc(NitroOrientationZoneView)
class NitroOrientationZoneView: UIView {
    @objc var zoneId: NSString = "" {
        didSet {
            let id = zoneId as String
            guard !id.isEmpty else { return }
            NitroOrientationZoneRegistry.shared.register(zoneId: id, view: self)
        }
    }
    
    override func didMoveToWindow() {
        super.didMoveToWindow()
        let id = zoneId as String
        guard !id.isEmpty else { return }
        if window == nil {
            NitroOrientationZoneRegistry.shared.unregister(zoneId: id, view: self)
        } else {
            NitroOrientationZoneRegistry.shared.register(zoneId: id, view: self)
        }
    }
}

class NitroOrientationZoneRegistry {
    static let shared = NitroOrientationZoneRegistry()
    private init() {}
    
    private var callbacks: ((String, UIView, Bool) -> Void)?
    
    func setCallbacks(_ callbacks: @escaping (String, UIView, Bool) -> Void) {
        self.callbacks = callbacks
    }
    
    func register(zoneId: String, view: UIView) {
        callbacks?(zoneId, view, true)
    }
    
    func unregister(zoneId: String, view: UIView) {
        callbacks?(zoneId, view, false)
    }
}
