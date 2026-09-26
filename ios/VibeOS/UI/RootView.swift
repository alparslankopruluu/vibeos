import SwiftUI

enum RootTab: String, CaseIterable {
    case discover = "Discover"
    case live = "Live Worlds"
    case create = "Create"
    case mine = "My Screen"

    var glyph: String {
        switch self {
        case .discover: return "⌂"
        case .live: return "◉"
        case .create: return "✦"
        case .mine: return "▦"
        }
    }
}

struct RootView: View {
    @AppStorage("vibeos.onboarded") private var onboarded = false
    @State private var tab: RootTab = .discover
    @State private var selectedTheme: ThemePack?
    @State private var selectedWorld: LiveWorld?
    @State private var applyingTheme: ThemePack?
    @State private var showPaywall = false
    @State private var premiumActive = false

    var body: some View {
        VibeBackground {
            if !onboarded {
                OnboardingView {
                    AppServices.shared.analytics.log("onboarding_complete")
                    onboarded = true
                }
            } else if showPaywall {
                PaywallView(
                    onBack: { showPaywall = false },
                    onPurchased: {
                        premiumActive = true
                        showPaywall = false
                    }
                )
            } else if let theme = applyingTheme {
                ApplyThemeView(theme: theme) { applyingTheme = nil }
            } else if let theme = selectedTheme {
                ThemeDetailView(
                    theme: theme,
                    premiumActive: premiumActive,
                    onBack: { selectedTheme = nil },
                    onPremium: { showPaywall = true },
                    onApply: { applyingTheme = theme }
                )
            } else if let world = selectedWorld {
                LiveWorldDetailView(
                    world: world,
                    premiumActive: premiumActive,
                    onBack: { selectedWorld = nil },
                    onPremium: { showPaywall = true }
                )
            } else {
                VStack(spacing: 0) {
                    Group {
                        switch tab {
                        case .discover:
                            DiscoverView(
                                onTheme: { selectedTheme = $0 },
                                onPremium: { showPaywall = true }
                            )
                        case .live:
                            LiveWorldsView { selectedWorld = $0 }
                        case .create:
                            CreateThemeView(
                                premiumActive: premiumActive,
                                onPremium: { showPaywall = true },
                                onGeneratedTheme: { selectedTheme = $0 }
                            )
                        case .mine:
                            MyScreenView(
                                onPremium: { showPaywall = true },
                                onPremiumRestored: { premiumActive = true }
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                    VibeTabBar(selected: $tab)
                }
            }
        }
        .tint(.white)
        .onOpenURL { url in
            guard url.scheme == "vibeos", let route = url.host else { return }
            handle(route: route)
        }
        .task {
            AppServices.shared.purchases.checkPremium { premiumActive = $0 }
        }
        .onReceive(NotificationCenter.default.publisher(for: .vibeRoute)) { notification in
            guard let route = notification.userInfo?["route"] as? String else { return }
            handle(route: route)
        }
    }

    private func handle(route: String) {
        selectedTheme = nil
        selectedWorld = nil
        applyingTheme = nil
        showPaywall = false

        switch route.lowercased() {
        case "live", "live-worlds":
            tab = .live
        case "create":
            tab = .create
        case "mine", "my-screen":
            tab = .mine
        case "premium", "offer":
            showPaywall = true
        default:
            tab = .discover
        }

        AppServices.shared.analytics.log(
            "deep_link_open",
            params: ["route": String(route.prefix(40))]
        )
    }
}

private struct OnboardingView: View {
    let onDone: () -> Void
    @State private var page = 0

    private let pages = [
        ("Turn Your Phone\nInto Something Special", "Themes, widgets, icons and Live Worlds — all in one.", "✦"),
        ("Pick Your Vibe", "Dark, glass, cute, cars, nature, luxury and more.", "◈"),
        ("One Look. One Tap.", "Preview the complete setup before you apply it.", "◎")
    ]

    var body: some View {
        let item = pages[page]

        VStack(spacing: 0) {
            Spacer(minLength: 50)

            ZStack {
                RoundedRectangle(cornerRadius: 48, style: .continuous)
                    .fill(
                        RadialGradient(
                            colors: [VibeColors.pink, VibeColors.purple, VibeColors.blue, .black],
                            center: .center,
                            startRadius: 4,
                            endRadius: 170
                        )
                    )
                    .frame(width: 220, height: 220)

                Text(item.2)
                    .font(.system(size: 84, weight: .black))
            }

            Spacer().frame(height: 38)

            Text(item.0)
                .font(.system(size: 34, weight: .black))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)

            Spacer().frame(height: 12)

            Text(item.1)
                .font(.system(size: 16))
                .foregroundStyle(VibeColors.muted)
                .multilineTextAlignment(.center)

            Spacer()

            HStack(spacing: 8) {
                ForEach(0..<pages.count, id: \.self) { index in
                    Capsule()
                        .fill(index == page ? Color.white : .white.opacity(0.22))
                        .frame(width: index == page ? 28 : 8, height: 8)
                }
            }

            Spacer().frame(height: 22)

            VibeButton(title: page == pages.count - 1 ? "Start Exploring" : "Next") {
                if page == pages.count - 1 {
                    onDone()
                } else {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        page += 1
                    }
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 18)
    }
}

private struct VibeTabBar: View {
    @Binding var selected: RootTab

    var body: some View {
        HStack(spacing: 2) {
            ForEach(RootTab.allCases, id: \.self) { tab in
                Button {
                    selected = tab
                    AppServices.shared.analytics.log("tab_open", params: ["tab": tab.rawValue.lowercased()])
                } label: {
                    VStack(spacing: 2) {
                        Text(tab.glyph)
                            .font(.system(size: 20))
                            .foregroundStyle(selected == tab ? VibeColors.pink : VibeColors.muted)
                        Text(tab.rawValue)
                            .font(.system(size: 9, weight: selected == tab ? .bold : .regular))
                            .foregroundStyle(selected == tab ? .white : VibeColors.muted)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 6)
        .padding(.bottom, 4)
        .background(Color.black.opacity(0.92))
    }
}
