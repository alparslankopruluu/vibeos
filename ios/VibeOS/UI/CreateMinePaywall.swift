import SwiftUI
import PhotosUI
import UserNotifications

enum LocalThemeGenerator {
    static func generate(prompt: String) -> ThemePack {
        let value = abs(prompt.hashValue)
        let palettes: [(Color, Color)] = [
            (.blue, .purple),
            (.red, .purple),
            (.cyan, .blue),
            (.pink, .orange),
            (.green, .teal)
        ]
        let palette = palettes[value % palettes.count]
        return ThemePack(
            id: "generated_" + String(value),
            name: String(prompt.prefix(28)).isEmpty ? "My Vibe" : String(prompt.prefix(28)),
            subtitle: "Generated from your vibe.",
            tags: ["AI", "Custom", "Personal"],
            a: palette.0,
            b: palette.1,
            premium: true,
            likes: "NEW",
            installs: "1"
        )
    }
}

struct CreateThemeView: View {
    let onPremium: () -> Void

    @State private var prompt = "Rainy Tokyo, dark glass, neon"
    @State private var photo: PhotosPickerItem?
    @State private var generated: ThemePack?
    @State private var generating = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("AI Theme Studio")
                        .font(.system(size: 28, weight: .black))
                    Text("Turn a photo or an idea into a complete setup.")
                        .font(.system(size: 14))
                        .foregroundStyle(VibeColors.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                GlassCard {
                    VStack(alignment: .leading, spacing: 14) {
                        if let generated {
                            ThemePhonePreview(theme: generated)
                                .frame(height: 300)
                        } else {
                            ZStack {
                                LinearGradient(
                                    colors: [Color(red: 0.09, green: 0.10, blue: 0.15), .purple.opacity(0.7), .blue.opacity(0.45)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                                Text(photo == nil ? "＋" : "✓")
                                    .font(.system(size: 62, weight: .ultraLight))
                            }
                            .frame(height: 220)
                            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        }

                        Text(generated == nil ? "Create a theme from any photo" : "Your theme is ready")
                            .font(.system(size: 18, weight: .black))

                        Text(photo == nil ? "Photo → palette → wallpaper → icons → widgets" : "Photo selected. Add a vibe description below.")
                            .font(.system(size: 12))
                            .foregroundStyle(VibeColors.muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                PhotosPicker(selection: $photo, matching: .images) {
                    Text(photo == nil ? "Choose a Photo" : "Change Photo")
                        .font(.system(size: 15, weight: .semibold))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .foregroundStyle(.white)
                        .background(.white.opacity(0.07))
                        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 17).stroke(.white.opacity(0.11)))
                }
                .onChange(of: photo) {
                    if photo != nil {
                        AppServices.shared.analytics.log("ai_source_selected", params: ["source": "photo"])
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Describe your dream theme")
                        .font(.system(size: 14, weight: .bold))

                    TextField("Rainy Tokyo, dark glass, neon", text: $prompt, axis: .vertical)
                        .textFieldStyle(.plain)
                        .font(.system(size: 14))
                        .padding(16)
                        .background(.white.opacity(0.06))
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(.white.opacity(0.10)))
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(["Glass", "Minimal", "Anime", "Nature", "Cute", "Cyber", "Luxury"], id: \.self) { tag in
                            Button {
                                prompt = String((prompt + ", " + tag.lowercased()).prefix(180))
                            } label: {
                                VibePill(text: tag)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                VibeButton(
                    title: generating ? "Creating…" : (generated == nil ? "Generate My Theme" : "Apply Generated Theme"),
                    enabled: !generating
                ) {
                    if generated != nil {
                        onPremium()
                    } else {
                        generating = true
                        AppServices.shared.analytics.log(
                            "ai_theme_generate_start",
                            params: ["source": photo == nil ? "text" : "photo"]
                        )
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.85) {
                            generated = LocalThemeGenerator.generate(prompt: prompt + (photo == nil ? "" : " photo"))
                            generating = false
                            AppServices.shared.analytics.log("ai_theme_generate_complete")
                        }
                    }
                }
            }
            .padding(18)
        }
        .scrollIndicators(.hidden)
    }
}

struct MyScreenView: View {
    let onPremium: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 15) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("My Screen")
                        .font(.system(size: 28, weight: .black))
                    Text("Your themes, streak and personalization.")
                        .font(.system(size: 14))
                        .foregroundStyle(VibeColors.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                GlassCard {
                    HStack(spacing: 12) {
                        Text("🔥").font(.system(size: 34))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("3 day vibe streak")
                                .font(.system(size: 18, weight: .black))
                            Text("Come back tomorrow for a fresh Daily Drop.")
                                .font(.system(size: 12))
                                .foregroundStyle(VibeColors.muted)
                        }
                        Spacer()
                        Text("+25")
                            .font(.system(size: 14, weight: .black))
                            .foregroundStyle(VibeColors.gold)
                    }
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("My Themes")
                        .font(.system(size: 18, weight: .black))

                    HStack(spacing: 12) {
                        ForEach(Catalog.themes.prefix(2)) { theme in
                            VStack(alignment: .leading, spacing: 7) {
                                ZStack {
                                    LinearGradient(
                                        colors: [theme.a, theme.b, .black],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                    Text("9:41")
                                        .font(.system(size: 28, weight: .light))
                                }
                                .frame(height: 160)
                                .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))

                                Text(theme.name)
                                    .font(.system(size: 13, weight: .bold))
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                }

                VibeButton(title: "♛  Upgrade to Premium", action: onPremium)

                VibeSecondaryButton(title: "Enable Daily Drop Notifications") {
                    DailyNotificationScheduler.requestAndSchedule()
                }

                VibeSecondaryButton(title: "Restore Purchases") {
                    AppServices.shared.purchases.restore { restored in
                        AppServices.shared.analytics.log("restore_purchase_result", params: ["restored": restored])
                    }
                }
            }
            .padding(18)
        }
        .scrollIndicators(.hidden)
    }
}

struct PaywallView: View {
    let onBack: () -> Void

    @ObservedObject private var offers = AppServices.shared.offers
    @State private var now = Date()
    @State private var buying = false
    @State private var status = ""

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        let offer = offers.limitedOffer
        let remaining = max(0, offer.expiry.timeIntervalSince(now))
        let hours = Int(remaining) / 3600
        let minutes = (Int(remaining) % 3600) / 60
        let seconds = Int(remaining) % 60
        let countdown = String(format: "%02d:%02d:%02d", hours, minutes, seconds)

        VStack(spacing: 0) {
            HStack {
                Spacer()
                CircleIcon(text: "×", action: onBack)
            }

            Spacer().frame(height: 10)

            Text("♛")
                .font(.system(size: 50))
                .foregroundStyle(VibeColors.gold)

            Text("VibeOS Premium")
                .font(.system(size: 30, weight: .black))

            Text("Unlimited themes. Live Worlds. AI creation. No ads.")
                .font(.system(size: 14))
                .foregroundStyle(VibeColors.muted)
                .multilineTextAlignment(.center)

            Spacer().frame(height: 20)

            GlassCard {
                VStack(alignment: .leading, spacing: 6) {
                    Text(offer.title)
                        .font(.system(size: 17, weight: .black))
                        .foregroundStyle(VibeColors.pink)
                    Text(offer.subtitle)
                        .font(.system(size: 13))
                        .foregroundStyle(.white.opacity(0.82))
                    Text(countdown)
                        .font(.system(size: 26, weight: .black))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            Spacer().frame(height: 16)

            VStack(spacing: 13) {
                ForEach([
                    "All premium themes",
                    "All Live Worlds",
                    "AI Theme Studio credits",
                    "New drops every week",
                    "No ads",
                    "Early access"
                ], id: \.self) { item in
                    HStack(spacing: 10) {
                        Text("✓")
                            .fontWeight(.black)
                            .foregroundStyle(VibeColors.gold)
                        Text(item)
                            .font(.system(size: 14))
                        Spacer()
                    }
                }
            }

            Spacer()

            GlassCard {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Yearly")
                            .font(.system(size: 16, weight: .black))
                        Text("3 days free, then annual")
                            .font(.system(size: 12))
                            .foregroundStyle(VibeColors.muted)
                    }
                    Spacer()
                    Text("BEST VALUE")
                        .font(.system(size: 10, weight: .black))
                        .foregroundStyle(VibeColors.pink)
                }
            }

            if !status.isEmpty {
                Text(status)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(VibeColors.muted)
                    .padding(.top, 8)
            }

            Spacer().frame(height: 12)

            VibeButton(title: buying ? "Opening Store…" : "Try Premium Free", enabled: !buying) {
                buying = true
                AppServices.shared.analytics.log("paywall_cta_tap", params: ["offer_id": offer.id])
                AppServices.shared.purchases.purchaseAnnual { success, error in
                    DispatchQueue.main.async {
                        buying = false
                        AppServices.shared.analytics.log(
                            "purchase_result",
                            params: ["success": success, "error": error ?? ""]
                        )
                        if success {
                            onBack()
                        } else {
                            status = error ?? "Purchase failed"
                        }
                    }
                }
            }

            Button("Restore Purchase") {
                AppServices.shared.purchases.restore { restored in
                    AppServices.shared.analytics.log("restore_purchase_result", params: ["restored": restored])
                }
            }
            .font(.system(size: 12))
            .foregroundStyle(VibeColors.muted)
            .buttonStyle(.plain)
            .padding(.top, 10)
        }
        .padding(18)
        .onReceive(timer) { now = $0 }
    }
}
