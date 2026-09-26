import UIKit
import Photos

enum WallpaperExporter {
    static func save(theme: ThemePack, completion: @escaping (Result<Void, Error>) -> Void) {
        let size = CGSize(width: 1290, height: 2796)
        let renderer = UIGraphicsImageRenderer(size: size)

        let image = renderer.image { ctx in
            let colors = [
                UIColor.black.cgColor,
                UIColor(theme.a).cgColor,
                UIColor(theme.b).cgColor,
                UIColor.black.cgColor
            ] as CFArray
            let locations: [CGFloat] = [0, 0.32, 0.72, 1]
            let space = CGColorSpaceCreateDeviceRGB()
            guard let gradient = CGGradient(colorsSpace: space, colors: colors, locations: locations) else { return }
            ctx.cgContext.drawLinearGradient(
                gradient,
                start: .zero,
                end: CGPoint(x: size.width, y: size.height),
                options: []
            )

            for index in 0..<10 {
                let alpha = 0.04 + CGFloat(index) * 0.007
                UIColor.white.withAlphaComponent(alpha).setFill()
                let radius = 80 + CGFloat(index % 4) * 35
                let x = CGFloat((index * 229) % Int(size.width))
                let y = CGFloat((index * 419) % Int(size.height))
                ctx.cgContext.fillEllipse(in: CGRect(x: x, y: y, width: radius, height: radius))
            }
        }

        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                completion(.failure(NSError(domain: "VibeOS", code: 1, userInfo: [NSLocalizedDescriptionKey: "Photo permission denied"])))
                return
            }
            PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAsset(from: image)
            } completionHandler: { success, error in
                if success {
                    completion(.success(()))
                } else {
                    completion(.failure(error ?? NSError(domain: "VibeOS", code: 2)))
                }
            }
        }
    }
}
