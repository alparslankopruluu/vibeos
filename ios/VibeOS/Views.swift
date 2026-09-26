import SwiftUI
import Photos
import PhotosUI

private enum Style {
    static let ink = Color(hex: "#070A16")
    static let glass = Color(hex: "#1A1D34")
    static let accent = Color(hex: "#A9A7FF")
    static let soft = Color(hex: "#A7A9C5")
}

private struct VibeButton: View {
    let title: String
    var quiet = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title).font(.system(size: 14, weight: .bold, design: .rounded)).tracking(1)
                .frame(maxWidth: .infinity).padding(.vertical, 17)
                .background(quiet ? AnyShapeStyle(Style.glass) : AnyShapeStyle(LinearGradient(colors: [.init(hex: "#8D82FC"), .init(hex: "#686BE7")], startPoint: .leading, endPoint: .trailing)))
                .clipShape(RoundedRectangle(cornerRadius: 21))
        }.buttonStyle(.plain)
    }
}
private struct GlassCard<Content: View>: View {
    let content: Content
    var body: some View { content.padding(19).frame(maxWidth: .infinity, alignment: .leading).background(Style.glass).clipShape(RoundedRectangle(cornerRadius: 24)).overlay(RoundedRectangle(cornerRadius: 24).stroke(.white.opacity(0.08))) }
}

private struct WorldArt: View {
    let theme: Theme
    var interactive = false
    @State private var tilt: CGFloat = 0
    @State private var touch: CGFloat = 0
    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0, paused: !interactive)) { time in
            GeometryReader { geometry in
                let size = geometry.size
                let seconds = time.date.timeIntervalSinceReferenceDate
                ZStack {
                    if let artwork = UIImage(named: theme.id) {
                        Image(uiImage: artwork).resizable().scaledToFill().frame(width: size.width, height: size.height).clipped()
                    } else { LinearGradient(colors: theme.palette, startPoint: .topLeading, endPoint: .bottomTrailing) }
                    Canvas { context, canvas in
                        for i in 0..<50 {
                            let x = CGFloat((i * 137) % 103) / 103 * canvas.width + tilt * CGFloat(2 + i % 7)
                            let y = (CGFloat((i * 71) % 97) / 97 * canvas.height + CGFloat(seconds.truncatingRemainder(dividingBy: 100)) * (theme.world == "sakura" ? 5 : 0.2)).truncatingRemainder(dividingBy: canvas.height)
                            let r: CGFloat = theme.world == "space" ? CGFloat(1 + i % 3) : CGFloat(3 + i % 5)
                            context.fill(Path(ellipseIn: CGRect(x: x, y: y, width: r * 2, height: r * 2)), with: .color(theme.palette[2].opacity(0.4 + 0.3 * sin(seconds + Double(i)))))
                        }
                        let orb = CGRect(x: canvas.width * 0.22 + tilt * 5, y: canvas.height * 0.22, width: canvas.width * 0.6, height: canvas.width * 0.6)
                        context.fill(Path(ellipseIn: orb), with: .radialGradient(Gradient(colors: [theme.palette[2].opacity(UIImage(named: theme.id) == nil ? 0.4 + min(touch / 15, 0.2) : 0.08), theme.palette[1].opacity(0.02)]), center: CGPoint(x: orb.midX, y: orb.midY), startRadius: 3, endRadius: orb.width * 0.55))
                    }
                    VStack {
                        Text("9:41").font(.system(size: min(size.width * 0.15, 37), weight: .medium, design: .rounded))
                        Text("MONDAY · 24").font(.system(size: 10, weight: .semibold)).tracking(2).opacity(0.7)
                        Spacer()
                        HStack(spacing: 7) { ForEach(0..<4) { _ in RoundedRectangle(cornerRadius: 9).fill(.white.opacity(0.16)).frame(width: 29, height: 29) } }
                        Text(theme.title.uppercased()).font(.system(size: 9, weight: .medium)).tracking(2).opacity(0.65)
                    }.padding(20)
                }.frame(width: size.width, height: size.height)
                    .contentShape(Rectangle())
                    .gesture(interactive ? DragGesture(minimumDistance: 0).onChanged { value in tilt = (value.location.x / max(size.width, 1) - 0.5) * 10; touch += 0.08 } : nil)
            }
        }.clipShape(RoundedRectangle(cornerRadius: 27))
    }
}

struct RootView: View {
    @EnvironmentObject private var store: VibeStore
    @AppStorage("welcomed") private var welcomed = false
    @State private var tab = 0
    @State private var paywall = false
    @State private var query = ""
    @State private var onboardingStep = 0
    @State private var chosenStyles = Set<String>()
    var body: some View {
        ZStack {
            Style.ink.ignoresSafeArea()
            if !welcomed { onboarding }
            else {
                VStack(spacing: 0) {
                    Group {
                        switch tab {
                        case 0: discover
                        case 1: worlds
                        case 2: create
                        default: mine
                        }
                    }.frame(maxHeight: .infinity)
                    HStack { nav("Discover", "sparkles", 0); nav("Worlds", "circle.hexagongrid", 1); nav("Create", "plus", 2); nav("My Screen", "square.grid.2x2", 3) }
                        .padding(.top, 13).padding(.bottom, 6).background(Color(hex: "#0D1022"))
                }
            }
        }
        .sheet(item: $store.route) { theme in ThemeDetail(theme: theme, paywall: $paywall).environmentObject(store) }
        .fullScreenCover(isPresented: $paywall) { PremiumView().environmentObject(store) }
        .alert("VibeOS", isPresented: Binding(get: { store.message != nil }, set: { if !$0 { store.message = nil } })) { Button("OK") { store.message = nil } } message: { Text(store.message ?? "") }
    }
    private func nav(_ name: String, _ icon: String, _ index: Int) -> some View {
        Button { tab = index } label: { VStack(spacing: 5) { Image(systemName: icon).font(.system(size: 20)); Text(name).font(.system(size: 10)) }.foregroundStyle(tab == index ? Style.accent : Style.soft).frame(maxWidth: .infinity) }.buttonStyle(.plain)
    }
    private var onboarding: some View {
        VStack(alignment: .leading, spacing: 0) {
            if onboardingStep == 0 {
                Text("VIBE / OS").font(.system(size: 13, weight: .bold)).tracking(3).foregroundStyle(Style.accent).frame(maxWidth: .infinity)
                Spacer(minLength: 18)
                if let theme = store.themes.first { WorldArt(theme: theme, interactive: true).frame(width: 230, height: 400).shadow(color: Style.accent.opacity(0.3), radius: 40).frame(maxWidth: .infinity) }
                Spacer(minLength: 22)
                Text("Turn your phone into\nsomething special.").font(.system(size: 36, weight: .bold, design: .rounded))
                Text("Themes, widgets and living worlds — all in one.").foregroundStyle(Style.soft).padding(.top, 10).padding(.bottom, 25)
                VibeButton(title: "NEXT  →") { onboardingStep = 1 }
            } else {
                Text("WHAT'S YOUR VIBE?").font(.system(size: 12, weight: .bold)).tracking(2).foregroundStyle(Style.accent).padding(.top, 28)
                Text("Choose the styles you love.").font(.system(size: 31, weight: .bold, design: .rounded)).padding(.top, 14)
                Text("We'll bring your favorites to the front.").font(.system(size: 14)).foregroundStyle(Style.soft)
                let options = ["Glass", "Dark", "Minimal", "Cute", "Nature", "Cars", "Space", "Luxury"]
                LazyVGrid(columns: [.init(.flexible()), .init(.flexible())], spacing: 12) {
                    ForEach(options, id: \.self) { style in
                        Button { if chosenStyles.contains(style) { chosenStyles.remove(style) } else { chosenStyles.insert(style) } } label: {
                            Text(style + (chosenStyles.contains(style) ? "  ✓" : ""))
                                .font(.system(size: 17, weight: .bold)).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading).padding(16)
                                .background(LinearGradient(colors: chosenStyles.contains(style) ? [Color(hex: "#554AA7"), Color(hex: "#24294D")] : [Style.glass, Color(hex: "#11172B")], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .clipShape(RoundedRectangle(cornerRadius: 20)).overlay(RoundedRectangle(cornerRadius: 20).stroke(chosenStyles.contains(style) ? Style.accent : .white.opacity(0.1)))
                        }.buttonStyle(.plain).frame(height: 105)
                    }
                }.padding(.top, 24)
                Spacer(minLength: 12)
                VibeButton(title: "CONTINUE (\(chosenStyles.count))  →") {
                    UserDefaults.standard.set(Array(chosenStyles), forKey: "favoriteStyles")
                    welcomed = true; store.event("onboarding_completed")
                }
            }
        }.padding(26)
    }
    private var discover: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                HStack { VStack(alignment: .leading, spacing: 9) { Text("VIBE / OS").font(.system(size: 12, weight: .bold)).tracking(3).foregroundStyle(Style.accent); Text("Find your next look.").font(.system(size: 30, weight: .bold, design: .rounded)) }; Spacer(); Image(systemName: "sparkles").font(.system(size: 25)).foregroundStyle(Style.accent) }
                GlassCard(content: TextField("⌕  Search looks, colors, worlds...", text: $query).font(.system(size: 15)).tint(Style.accent))
                if !store.themes.isEmpty {
                    Button { store.route = store.daily; store.event("daily_drop_opened", themeId: store.daily.id) } label: {
                        GlassCard(content: VStack(alignment: .leading, spacing: 7) { Text("✦  TODAY'S DROP").font(.system(size: 11, weight: .bold)).foregroundStyle(Style.accent); Text(store.daily.title).font(.system(size: 22, weight: .bold)); Text("A fresh look for today · Explore now  →").font(.system(size: 13)).foregroundStyle(Style.soft) })
                    }.buttonStyle(.plain)
                }
                HStack { Text("Complete looks").font(.system(size: 23, weight: .bold)); Spacer(); Text("EXPLORE").font(.system(size: 11, weight: .bold)).foregroundStyle(Style.accent) }
                ForEach(store.themes.filter { query.isEmpty || $0.title.localizedCaseInsensitiveContains(query) || $0.category.localizedCaseInsensitiveContains(query) || $0.world.localizedCaseInsensitiveContains(query) }.sorted { left, right in
                    let styles = UserDefaults.standard.stringArray(forKey: "favoriteStyles") ?? []
                    return styles.contains(left.category) && !styles.contains(right.category)
                }) { theme in
                    Button { store.route = theme; store.event("theme_opened", themeId: theme.id) } label: {
                        ZStack(alignment: .bottomLeading) {
                            WorldArt(theme: theme).frame(height: 390)
                            LinearGradient(colors: [.clear, Style.ink], startPoint: .center, endPoint: .bottom).clipShape(RoundedRectangle(cornerRadius: 27))
                            VStack(alignment: .leading, spacing: 4) { Text(theme.category.uppercased() + (theme.premium ? "  ·  PRO" : "  ·  FREE")).font(.system(size: 11, weight: .bold)).foregroundStyle(Style.accent); Text(theme.title).font(.system(size: 27, weight: .bold)); Text(theme.subtitle + "    →").font(.system(size: 13)).foregroundStyle(Style.soft) }.padding(22)
                        }.frame(height: 390)
                    }.buttonStyle(.plain)
                }
            }.padding(22)
        }
    }
    private var worlds: some View {
        ScrollView { VStack(alignment: .leading, spacing: 18) {
            Text("LIVE WORLDS").font(.system(size: 12, weight: .bold)).tracking(2).foregroundStyle(Style.accent)
            Text("Made to move.").font(.system(size: 31, weight: .bold, design: .rounded))
            Text("Touch and tilt to explore. Save a still as wallpaper on iPhone.").font(.system(size: 14)).foregroundStyle(Style.soft)
            ForEach(Array(Dictionary(grouping: store.themes, by: \.world).values.compactMap { $0.first }).sorted { $0.id < $1.id }) { theme in
                Button { store.route = theme; store.event("world_opened", themeId: theme.id) } label: {
                    ZStack(alignment: .bottomLeading) { WorldArt(theme: theme, interactive: true).frame(height: 265); GlassCard(content: VStack(alignment: .leading) { Text(theme.title).font(.system(size: 18, weight: .bold)); Text("Touch to explore  →").font(.system(size: 12)).foregroundStyle(Style.soft) }).padding(14) }
                }.buttonStyle(.plain)
            }
        }.padding(22) }
    }
    @State private var prompt = ""
    @State private var fromPhoto = true
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var photoData: Data?
    private var create: some View {
        ScrollView { VStack(alignment: .leading, spacing: 17) {
            Text("CREATE STUDIO").font(.system(size: 12, weight: .bold)).tracking(2).foregroundStyle(Style.accent)
            Text("Your vision.\nYour screen.").font(.system(size: 34, weight: .bold, design: .rounded))
            Text("Describe a world and generate a matching wallpaper with your connected AI service.").font(.system(size: 14)).foregroundStyle(Style.soft)
            HStack(spacing: 10) {
                VibeButton(title: "FROM PHOTO", quiet: !fromPhoto) { fromPhoto = true }
                VibeButton(title: "FROM TEXT", quiet: fromPhoto) { fromPhoto = false }
            }.padding(.top, 10)
            if fromPhoto {
                if let photoData, let image = UIImage(data: photoData) { Image(uiImage: image).resizable().scaledToFill().frame(height: 205).frame(maxWidth: .infinity).clipped().clipShape(RoundedRectangle(cornerRadius: 23)) }
                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                    Text(photoData == nil ? "UPLOAD A PHOTO  ＋" : "CHANGE PHOTO  ↻")
                        .font(.system(size: 14, weight: .bold)).frame(maxWidth: .infinity).padding(.vertical, 17).background(Style.glass).clipShape(RoundedRectangle(cornerRadius: 21))
                }.buttonStyle(.plain)
            }
            GlassCard(content: VStack(alignment: .leading) { Text("DESCRIBE YOUR LOOK").font(.system(size: 11, weight: .bold)).foregroundStyle(Style.accent); TextField("Rainy Tokyo, violet neon, dark glass...", text: $prompt, axis: .vertical).lineLimit(4...7).font(.system(size: 17)).tint(Style.accent) }).padding(.top, 14)
            Text("A selected photo is sent to the image service for generation. VibeOS does not store the source photo.").font(.system(size: 12)).foregroundStyle(Style.soft)
            VibeButton(title: store.generating ? "CREATING..." : "GENERATE MY LOOK  ✦") {
                if fromPhoto && photoData == nil { store.message = "Choose a photo to inspire your theme." }
                else { Task { await store.create(prompt: prompt, imageBase64: fromPhoto ? photoData?.base64EncodedString() : nil) } }
            }.disabled(store.generating)
            if let url = store.generatedURL {
                AsyncImage(url: url) { image in image.resizable().scaledToFill().frame(height: 340).clipped().clipShape(RoundedRectangle(cornerRadius: 24)) } placeholder: { ProgressView().frame(height: 340) }
                VibeButton(title: "SAVE WALLPAPER TO PHOTOS", quiet: true) { saveWallpaper(url) }
            }
        }.padding(22) }
        .onChange(of: selectedPhoto) { _, item in
            Task {
                guard let data = try? await item?.loadTransferable(type: Data.self), let image = UIImage(data: data) else { return }
                let scale = min(1, 768 / max(image.size.width, image.size.height))
                let target = CGSize(width: image.size.width * scale, height: image.size.height * scale)
                let resized = UIGraphicsImageRenderer(size: target).image { _ in image.draw(in: CGRect(origin: .zero, size: target)) }
                photoData = resized.jpegData(compressionQuality: 0.72)
            }
        }
    }
    private func saveWallpaper(_ url: URL) {
        Task {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                guard let image = UIImage(data: data) else { throw URLError(.cannotDecodeContentData) }
                try await PHPhotoLibrary.shared().performChanges { PHAssetChangeRequest.creationRequestForAsset(from: image) }
                store.message = "Saved to Photos. Select it in iPhone wallpaper settings."
            } catch { store.error(error); store.message = "Could not save wallpaper: \(error.localizedDescription)" }
        }
    }
    private var mine: some View {
        ScrollView { VStack(alignment: .leading, spacing: 17) {
            Text("MY SCREEN").font(.system(size: 12, weight: .bold)).tracking(2).foregroundStyle(Style.accent)
            Text("Make it feel like you.").font(.system(size: 30, weight: .bold, design: .rounded))
            if !store.themes.isEmpty { WorldArt(theme: store.selectedTheme).frame(height: 300); GlassCard(content: VStack(alignment: .leading) { Text("Your selected look").foregroundStyle(Style.soft); Text(store.selectedTheme.title).font(.system(size: 22, weight: .bold)) }) }
            VibeButton(title: "EXPLORE PREMIUM") { paywall = true }
            VibeButton(title: store.notificationsEnabled ? "DAILY REMINDERS ON" : "ENABLE DAILY DROP REMINDERS", quiet: true) { store.enableNotifications() }
            Text("Add the VibeOS widget from the Home Screen widget gallery. iPhone wallpaper changes are completed in Photos or Settings.").font(.system(size: 13)).foregroundStyle(Style.soft)
        }.padding(22) }
    }
}

private struct ThemeDetail: View {
    let theme: Theme
    @Binding var paywall: Bool
    @EnvironmentObject private var store: VibeStore
    @Environment(\.dismiss) private var dismiss
    @State private var showGuide = false
    var body: some View {
        ZStack { Style.ink.ignoresSafeArea(); ScrollView { VStack(alignment: .leading, spacing: 18) {
            HStack { Button("←  Back") { dismiss() }.buttonStyle(.plain); Spacer(); Text(theme.premium ? "✦  PREMIUM" : "FREE LOOK").font(.system(size: 12, weight: .bold)).foregroundStyle(Style.accent) }
            WorldArt(theme: theme, interactive: true).frame(width: 220, height: 380).frame(maxWidth: .infinity).padding(.vertical, 12)
            Text(theme.title).font(.system(size: 32, weight: .bold, design: .rounded))
            Text(theme.subtitle).foregroundStyle(Style.soft)
            GlassCard(content: VStack(alignment: .leading, spacing: 10) { Text("IN THIS LOOK").font(.system(size: 11, weight: .bold)).foregroundStyle(Style.accent); Text("✓  Matching wallpaper artwork"); Text("✓  Daily widget"); Text("✓  Coordinated colors") })
            VibeButton(title: theme.premium && !store.premium ? "UNLOCK THIS LOOK  ✦" : "APPLY COMPLETE LOOK  →") { if theme.premium && !store.premium { paywall = true } else { showGuide = true } }
            VibeButton(title: "SAVE WALLPAPER TO PHOTOS", quiet: true) {
                if theme.premium && !store.premium { paywall = true }
                else { Task { await saveThemeWallpaper() } }
            }
            Text("On iPhone, save wallpaper artwork to Photos and choose it in Settings. Other apps' icons are installed manually with Shortcuts.").font(.system(size: 12)).foregroundStyle(Style.soft)
        }.padding(22) } }
        .presentationDragIndicator(.visible)
        .sheet(isPresented: $showGuide) { ApplyGuideView(theme: theme, save: { Task { await saveThemeWallpaper() } }, select: { store.select(theme) }).environmentObject(store) }
    }
    private func saveThemeWallpaper() async {
        let size = CGSize(width: 1024, height: 1536)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { context in
            let cg = context.cgContext
            if let artwork = UIImage(named: theme.id) { artwork.draw(in: CGRect(origin: .zero, size: size)); return }
            let colors = theme.colors.map { UIColor(Color(hex: $0)).cgColor } as CFArray
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 0.58, 1]) {
                cg.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: size.width, y: size.height), options: [])
            }
            UIColor(Color(hex: theme.colors[2])).withAlphaComponent(0.27).setFill()
            cg.fillEllipse(in: CGRect(x: 215, y: 350, width: 620, height: 620))
            for i in 0..<70 {
                let x = CGFloat((i * 137) % 103) / 103 * size.width
                let y = CGFloat((i * 71) % 97) / 97 * size.height
                UIColor.white.withAlphaComponent(0.2 + Double(i % 5) * 0.1).setFill()
                cg.fillEllipse(in: CGRect(x: x, y: y, width: CGFloat(2 + i % 5), height: CGFloat(2 + i % 5)))
            }
        }
        do {
            try await PHPhotoLibrary.shared().performChanges { PHAssetChangeRequest.creationRequestForAsset(from: image) }
            store.message = "Saved to Photos. Select it in iPhone wallpaper settings."
            store.event("theme_applied", themeId: theme.id)
        } catch { store.error(error); store.message = "Could not save wallpaper: \(error.localizedDescription)" }
    }
}

private struct ApplyGuideView: View {
    let theme: Theme
    let save: () -> Void
    let select: () -> Void
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: VibeStore
    var body: some View {
        ZStack { Style.ink.ignoresSafeArea(); ScrollView { VStack(alignment: .leading, spacing: 18) {
            Button("←  Look preview") { dismiss() }.buttonStyle(.plain)
            Text("APPLY YOUR THEME").font(.system(size: 12, weight: .bold)).tracking(2).foregroundStyle(Style.accent).padding(.top, 20)
            Text("Your screen, step by step.").font(.system(size: 29, weight: .bold, design: .rounded))
            Text("Complete these steps in iPhone's Photos, Settings and Home Screen.").font(.system(size: 14)).foregroundStyle(Style.soft)
            ForEach(Array([("Save wallpaper", "Export artwork to Photos"), ("Choose Home & Lock", "Select the image in wallpaper settings"), ("Add VibeOS widget", "Long press Home → Add Widget"), ("Customize icons", "Use Shortcuts for other apps")].enumerated()), id: \.offset) { index, step in
                GlassCard(content: HStack(spacing: 13) {
                    Text("\(index + 1)").font(.system(size: 16, weight: .bold)).frame(width: 36, height: 36).background(Style.accent).clipShape(RoundedRectangle(cornerRadius: 12))
                    VStack(alignment: .leading) { Text(step.0).font(.system(size: 16, weight: .bold)); Text(step.1).font(.system(size: 12)).foregroundStyle(Style.soft) }
                })
            }
            VibeButton(title: "SAVE WALLPAPER  →") { save(); select() }
            VibeButton(title: "SELECT LOOK FOR WIDGET", quiet: true) { select(); dismiss() }
        }.padding(22) } }.presentationDragIndicator(.visible)
    }
}

private struct PremiumView: View {
    @EnvironmentObject private var store: VibeStore
    @Environment(\.dismiss) private var dismiss
    @State private var chosen = 0
    var body: some View {
        ZStack { Style.ink.ignoresSafeArea(); ScrollView { VStack(alignment: .leading, spacing: 20) {
            Button("✕  Close") { dismiss() }.buttonStyle(.plain)
            Spacer(minLength: 20)
            Text("✦  VIBEOS PREMIUM").font(.system(size: 13, weight: .bold)).tracking(2).foregroundStyle(Style.accent)
            Text("Every look.\nEvery world.").font(.system(size: 41, weight: .bold, design: .rounded))
            if let end = store.offerEnd, !store.packages.isEmpty {
                TimelineView(.periodic(from: .now, by: 1)) { timeline in
                    let seconds = max(0, Int(end.timeIntervalSince(timeline.date)))
                    if seconds > 0 { Text("LIMITED OFFER  ·  \(seconds / 3600)h \((seconds % 3600) / 60)m remaining").font(.system(size: 13, weight: .bold)).foregroundStyle(Style.accent) }
                }
            }
            Text("Unlock all complete looks, interactive previews and new drops as they arrive.").foregroundStyle(Style.soft)
            ForEach(["All premium themes", "Every Live World preview", "New drops and collections"], id: \.self) { Text("✓  " + $0).font(.system(size: 17)) }
            if store.packages.isEmpty { Text("Store plans will appear here when configured.").foregroundStyle(Style.soft) }
            ForEach(store.packages.indices, id: \.self) { index in
                Button { chosen = index } label: { HStack { Text(store.packages[index].storeProduct.localizedTitle); Spacer(); Text(store.packages[index].storeProduct.localizedPriceString) }.padding(18).background(Style.glass).clipShape(RoundedRectangle(cornerRadius: 18)).overlay(RoundedRectangle(cornerRadius: 18).stroke(chosen == index ? Style.accent : .white.opacity(0.1))) }.buttonStyle(.plain)
            }
            VibeButton(title: "CONTINUE") { if store.packages.indices.contains(chosen) { store.purchase(store.packages[chosen]) } }.disabled(store.packages.isEmpty)
            Text("The App Store confirms the actual price and any eligible trial before purchase.").font(.system(size: 12)).foregroundStyle(Style.soft)
            Button("Restore purchases") { store.restore() }.font(.system(size: 13)).foregroundStyle(Style.accent).frame(maxWidth: .infinity)
        }.padding(26) } }
        .onAppear { store.event("paywall_viewed"); store.fetchPackages() }
        .onChange(of: store.premium) { _, value in if value { dismiss() } }
    }
}
