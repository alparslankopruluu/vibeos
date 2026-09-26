import SwiftUI

struct DiscoverView: View {
    let onTheme: (ThemePack) -> Void
    let onPremium: () -> Void
    @State private var category = "For You"

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 18) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("VibeOS")
                            .font(.system(size: 30, weight: .black))
                        Text("Make your phone feel yours.")
                            .font(.system(size: 13))
                            .foregroundStyle(VibeColors.muted)
                    }

                    Spacer()

                    Button(action: onPremium) {
                        Text("♛")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(VibeColors.gold)
                            .frame(width: 44, height: 44)
                            .background(VibeColors.gold.opacity(0.13))
                            .clipShape(Circle())
                            .overlay(Circle().stroke(VibeColors.gold.opacity(0.35)))
                    }
                    .buttonStyle(.plain)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(["For You", "Trending", "New", "Popular"], id: \.self) { label in
                            Button {
                                category = label
                            } label: {
                                VibePill(text: label, selected: category == label)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                DailyDropCard(onTap: onPremium)

                ForEach(Catalog.themes) { theme in
                    ThemeHeroCard(theme: theme) {
                        AppServices.shared.analytics.log("theme_open", params: ["theme_id": theme.id])
                        onTheme(theme)
                    }
                }
            }
            .padding(18)
        }
        .scrollIndicators(.hidden)
    }
}

private struct DailyDropCard: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            GlassCard {
                HStack(spacing: 13) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 17, style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [Color.pink, Color.purple],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 54, height: 54)
                        Text("✦")
                            .font(.system(size: 24, weight: .bold))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("DAILY DROP")
                            .font(.system(size: 11, weight: .black))
                            .foregroundStyle(VibeColors.pink)
                        Text("Sakura Night")
                            .font(.system(size: 17, weight: .black))
                        Text("Free today • refreshes in 24h")
                            .font(.system(size: 12))
                            .foregroundStyle(VibeColors.muted)
                    }

                    Spacer()
                    Text("→")
                        .font(.system(size: 22))
                }
            }
        }
        .buttonStyle(.plain)
    }
}

private struct ThemeHeroCard: View {
    let theme: ThemePack
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 14) {
                ThemePhonePreview(theme: theme)
                    .frame(height: 420)

                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 3) {
                        HStack(spacing: 7) {
                            Text(theme.name)
                                .font(.system(size: 20, weight: .black))
                            if theme.premium {
                                Text("PRO")
                                    .font(.system(size: 10, weight: .black))
                                    .foregroundStyle(VibeColors.gold)
                            }
                        }
                        Text(theme.subtitle)
                            .font(.system(size: 13))
                            .foregroundStyle(VibeColors.muted)
                            .multilineTextAlignment(.leading)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 3) {
                        Text("♥ " + theme.likes)
                            .font(.system(size: 12))
                        Text("↓ " + theme.installs)
                            .font(.system(size: 11))
                            .foregroundStyle(VibeColors.muted)
                    }
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 7) {
                        ForEach(theme.tags, id: \.self) { tag in
                            VibePill(text: "#" + tag)
                        }
                    }
                }

                VibeButton(title: "Apply This Look", action: onTap)
            }
            .padding(14)
            .background(.white.opacity(0.045))
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 30).stroke(.white.opacity(0.09)))
        }
        .buttonStyle(.plain)
    }
}

struct ThemePhonePreview: View {
    let theme: ThemePack

    var body: some View {
        ZStack {
            RadialGradient(
                colors: [theme.b, theme.a, Color(red: 0.02, green: 0.03, blue: 0.06)],
                center: .center,
                startRadius: 20,
                endRadius: 320
            )

            VStack(spacing: 0) {
                Text("9:41")
                    .font(.system(size: 36, weight: .light))
                Text("MON, AUG 12")
                    .font(.system(size: 9))
                    .foregroundStyle(.white.opacity(0.68))

                Spacer()

                HStack {
                    ForEach(["✦", "◉", "⌁", "◎"], id: \.self) { glyph in
                        Text(glyph)
                            .frame(width: 38, height: 38)
                            .background(.white.opacity(0.10))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(.white.opacity(0.16)))
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .padding(15)
            .frame(width: 205)
            .background(.black.opacity(0.72))
            .clipShape(RoundedRectangle(cornerRadius: 34, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 34).stroke(.white.opacity(0.24)))
            .padding(.vertical, 26)

            VStack {
                HStack {
                    Text("NEW")
                        .font(.system(size: 10, weight: .black))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(VibeColors.pink)
                        .clipShape(Capsule())
                    Spacer()
                }
                Spacer()
            }
            .padding(14)
        }
        .foregroundStyle(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct ThemeDetailView: View {
    let theme: ThemePack
    let premiumActive: Bool
    let onBack: () -> Void
    let onPremium: () -> Void
    let onApply: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                TopBar(title: theme.name, onBack: onBack)

                ThemePhonePreview(theme: theme)
                    .frame(height: 390)

                HStack(spacing: 9) {
                    VibePill(text: "Lock", selected: true)
                    VibePill(text: "Home")
                    VibePill(text: "Widgets")
                    VibePill(text: "Icons")
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Includes")
                            .font(.system(size: 18, weight: .black))

                        ForEach([
                            "Wallpapers (Home + Lock)",
                            "86 coordinated app icons",
                            "24 widgets (S, M, L)",
                            "Control center style",
                            "Live World companion",
                            "Charging animation"
                        ], id: \.self) { line in
                            HStack(spacing: 10) {
                                Text("✓")
                                    .foregroundStyle(VibeColors.green)
                                    .fontWeight(.black)
                                Text(line)
                                    .font(.system(size: 14))
                                    .foregroundStyle(.white.opacity(0.88))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                VibeButton(title: "Apply Complete Theme") {
                    (theme.premium && !premiumActive) ? onPremium() : onApply()
                }

                VibeSecondaryButton(title: "Customize") {
                    AppServices.shared.analytics.log("theme_customize_tap", params: ["theme_id": theme.id])
                }
            }
            .padding(18)
        }
        .scrollIndicators(.hidden)
    }
}

struct ApplyThemeView: View {
    let theme: ThemePack
    let onDone: () -> Void
    @State private var saved = false
    @State private var message = ""

    var body: some View {
        VStack(spacing: 16) {
            TopBar(title: "Apply Theme", onBack: onDone)

            Text("iPhone keeps wallpaper changes under your control. VibeOS saves the finished wallpaper, then you set it from Photos.")
                .font(.system(size: 13))
                .foregroundStyle(VibeColors.muted)
                .frame(maxWidth: .infinity, alignment: .leading)

            GlassCard {
                VStack(spacing: 4) {
                    ApplyRow(number: 1, title: "Save Home + Lock wallpaper", done: saved)
                    ApplyRow(number: 2, title: "Add VibeOS widgets", done: false)
                    ApplyRow(number: 3, title: "Apply icon shortcuts", done: false)
                    ApplyRow(number: 4, title: "Choose matching controls", done: false)
                }
            }

            if !message.isEmpty {
                Text(message)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(saved ? VibeColors.green : VibeColors.pink)
            }

            Spacer()

            VibeButton(title: saved ? "Continue Setup" : "Save Wallpaper") {
                if saved {
                    onDone()
                    return
                }
                WallpaperExporter.save(theme: theme) { result in
                    DispatchQueue.main.async {
                        switch result {
                        case .success:
                            saved = true
                            message = "Saved to Photos. Open it and choose Use as Wallpaper."
                            AppServices.shared.analytics.log("wallpaper_export_success", params: ["theme_id": theme.id])
                        case .failure(let error):
                            message = error.localizedDescription
                            AppServices.shared.analytics.record(error, context: "wallpaper_export")
                        }
                    }
                }
            }
        }
        .padding(18)
    }
}

private struct ApplyRow: View {
    let number: Int
    let title: String
    let done: Bool

    var body: some View {
        HStack(spacing: 12) {
            Text(done ? "✓" : String(number))
                .font(.system(size: 14, weight: .black))
                .foregroundStyle(done ? VibeColors.green : .white)
                .frame(width: 38, height: 38)
                .background(done ? VibeColors.green.opacity(0.16) : .white.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            Text(title)
                .font(.system(size: 14, weight: .semibold))

            Spacer()
            Text(done ? "Done" : "○")
                .foregroundStyle(done ? VibeColors.green : VibeColors.muted)
        }
        .padding(.vertical, 6)
    }
}

struct TopBar: View {
    let title: String
    let onBack: () -> Void

    var body: some View {
        HStack {
            CircleIcon(text: "‹", action: onBack)
            Text(title)
                .font(.system(size: 20, weight: .black))
            Spacer()
            Text("♡")
                .font(.system(size: 22))
        }
    }
}

struct CircleIcon: View {
    let text: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 22))
                .frame(width: 42, height: 42)
                .background(.black.opacity(0.35))
                .clipShape(Circle())
                .overlay(Circle().stroke(.white.opacity(0.12)))
        }
        .buttonStyle(.plain)
    }
}
