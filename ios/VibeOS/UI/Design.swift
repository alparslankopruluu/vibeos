import SwiftUI

enum VibeColors {
    static let ink = Color(red: 0.027, green: 0.031, blue: 0.051)
    static let panel = Color(red: 0.067, green: 0.075, blue: 0.102)
    static let text = Color(red: 0.97, green: 0.97, blue: 0.99)
    static let muted = Color(red: 0.60, green: 0.62, blue: 0.68)
    static let cyan = Color(red: 0.26, green: 0.85, blue: 1.0)
    static let blue = Color(red: 0.26, green: 0.47, blue: 1.0)
    static let purple = Color(red: 0.55, green: 0.28, blue: 1.0)
    static let pink = Color(red: 0.95, green: 0.22, blue: 0.87)
    static let green = Color(red: 0.27, green: 0.90, blue: 0.67)
    static let gold = Color(red: 1.0, green: 0.79, blue: 0.35)
}

struct VibeBackground<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color.black, VibeColors.ink, Color(red: 0.04, green: 0.045, blue: 0.075)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            content
        }
    }
}

struct GlassCard<Content: View>: View {
    var padding: CGFloat = 16
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(padding)
            .background(
                LinearGradient(
                    colors: [.white.opacity(0.09), .white.opacity(0.035)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(.white.opacity(0.12), lineWidth: 1)
            )
    }
}

struct VibeButton: View {
    let title: String
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .foregroundStyle(.white)
                .background(
                    LinearGradient(
                        colors: enabled ? [VibeColors.cyan, VibeColors.blue, VibeColors.purple, VibeColors.pink] : [.gray.opacity(.5), .gray.opacity(.35)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct VibeSecondaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .foregroundStyle(.white)
                .background(.white.opacity(0.07))
                .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 17).stroke(.white.opacity(0.11)))
        }
        .buttonStyle(.plain)
    }
}

struct VibePill: View {
    let text: String
    var selected = false

    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(selected ? .black : .white.opacity(0.82))
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? Color.white : .white.opacity(0.07))
            .clipShape(Capsule())
    }
}
