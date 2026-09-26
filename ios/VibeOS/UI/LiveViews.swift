import SwiftUI
import CoreMotion

@MainActor
final class MotionController: ObservableObject {
    @Published var x: Double = 0
    @Published var y: Double = 0

    private let manager = CMMotionManager()

    init() {
        guard manager.isDeviceMotionAvailable else { return }
        manager.deviceMotionUpdateInterval = 1.0 / 30.0
        manager.startDeviceMotionUpdates(to: .main) { [weak self] motion, _ in
            guard let gravity = motion?.gravity else { return }
            self?.x = gravity.x
            self?.y = gravity.y
        }
    }

    deinit {
        manager.stopDeviceMotionUpdates()
    }
}

struct LiveWorldsView: View {
    let onWorld: (LiveWorld) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Live Worlds")
                        .font(.system(size: 28, weight: .black))
                    Text("Interactive, animated, living wallpapers.")
                        .font(.system(size: 14))
                        .foregroundStyle(VibeColors.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(["All", "Nature", "Space", "Cars", "Animals"].enumerated()), id: \.offset) { index, label in
                            VibePill(text: label, selected: index == 0)
                        }
                    }
                }

                ForEach(Catalog.liveWorlds) { world in
                    Button {
                        AppServices.shared.analytics.log("live_world_open", params: ["world_id": world.id])
                        onWorld(world)
                    } label: {
                        GlassCard {
                            HStack(spacing: 14) {
                                ZStack {
                                    LinearGradient(
                                        colors: [world.a, world.b],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                    Text(world.emoji)
                                        .font(.system(size: 38))
                                }
                                .frame(width: 84, height: 84)
                                .clipShape(RoundedRectangle(cornerRadius: 21, style: .continuous))

                                VStack(alignment: .leading, spacing: 5) {
                                    Text(world.name)
                                        .font(.system(size: 18, weight: .black))
                                    Text(world.subtitle)
                                        .font(.system(size: 13))
                                        .foregroundStyle(VibeColors.muted)
                                        .multilineTextAlignment(.leading)
                                    Text("▶ Preview")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(VibeColors.cyan)
                                }

                                Spacer()

                                if world.premium {
                                    Text("PRO")
                                        .font(.system(size: 10, weight: .black))
                                        .foregroundStyle(VibeColors.gold)
                                }
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(18)
        }
        .scrollIndicators(.hidden)
    }
}

struct LiveWorldDetailView: View {
    let world: LiveWorld
    let onBack: () -> Void
    let onPremium: () -> Void

    @State private var saveMessage = ""

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                LiveWorldPreview(world: world)
                    .ignoresSafeArea(edges: .top)

                VStack {
                    HStack {
                        CircleIcon(text: "‹", action: onBack)
                        Spacer()
                        CircleIcon(text: "♡") {
                            AppServices.shared.analytics.log("live_world_favorite", params: ["world_id": world.id])
                        }
                    }
                    .padding(18)

                    Spacer()

                    VStack(alignment: .leading, spacing: 10) {
                        Text(world.name)
                            .font(.system(size: 30, weight: .black))
                        Text(world.subtitle)
                            .font(.system(size: 14))
                            .foregroundStyle(.white.opacity(0.78))

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 7) {
                                ForEach(world.features, id: \.self) { feature in
                                    VibePill(text: feature)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(20)
                }
            }

            VStack(spacing: 9) {
                if !saveMessage.isEmpty {
                    Text(saveMessage)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(VibeColors.green)
                }

                VibeButton(title: world.premium ? "Unlock Live World" : "Save Live World") {
                    if world.premium {
                        onPremium()
                    } else {
                        AppServices.shared.analytics.log("live_world_export_tap", params: ["world_id": world.id])
                        let poster = ThemePack(
                            id: "live_" + world.id,
                            name: world.name,
                            subtitle: world.subtitle,
                            tags: world.features,
                            a: world.a,
                            b: world.b
                        )
                        WallpaperExporter.save(theme: poster) { result in
                            DispatchQueue.main.async {
                                switch result {
                                case .success:
                                    saveMessage = "Saved. Set it from Photos; the interactive preview remains inside VibeOS."
                                case .failure(let error):
                                    saveMessage = error.localizedDescription
                                    AppServices.shared.analytics.record(error, context: "live_world_export")
                                }
                            }
                        }
                    }
                }
            }
            .padding(18)
        }
    }
}

struct LiveWorldPreview: View {
    let world: LiveWorld

    @StateObject private var motion = MotionController()
    @State private var touch = CGPoint(x: 0.5, y: 0.5)

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let phase = timeline.date.timeIntervalSinceReferenceDate
                .truncatingRemainder(dividingBy: 5) / 5

            Canvas { context, size in
                let background = GraphicsContext.Shading.linearGradient(
                    Gradient(colors: [world.a, world.b, Color(red: 0.02, green: 0.03, blue: 0.07)]),
                    startPoint: .zero,
                    endPoint: CGPoint(x: size.width, y: size.height)
                )
                context.fill(Path(CGRect(origin: .zero, size: size)), with: background)

                for index in 0..<38 {
                    let travel = CGFloat(phase) * size.width * (0.12 + CGFloat(index % 5) * 0.025)
                    var x = (CGFloat(index) * 97 + travel).truncatingRemainder(dividingBy: size.width)
                    let y = (CGFloat(index) * 167 + touch.y * 90 + CGFloat(phase) * 100)
                        .truncatingRemainder(dividingBy: size.height)

                    x += CGFloat(motion.x) * CGFloat(index % 5) * 16
                    let radius = CGFloat(2 + index % 5)
                    let rect = CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2)
                    context.fill(Path(ellipseIn: rect), with: .color(.white.opacity(0.15 + Double(index % 4) * 0.06)))
                }

                let pulse = 110 + CGFloat(phase) * 20
                let cx = touch.x * size.width + CGFloat(motion.x) * 18
                let cy = touch.y * size.height + CGFloat(motion.y) * 18
                context.fill(
                    Path(ellipseIn: CGRect(x: cx - pulse, y: cy - pulse, width: pulse * 2, height: pulse * 2)),
                    with: .color(.white.opacity(0.10))
                )
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        touch = CGPoint(
                            x: min(max(value.location.x / max(1, value.startLocation.x + abs(value.translation.width) + 200), 0), 1),
                            y: min(max(value.location.y / 900, 0), 1)
                        )
                    }
            )
        }
    }
}
