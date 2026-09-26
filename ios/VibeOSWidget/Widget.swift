import WidgetKit
import SwiftUI

struct DropEntry: TimelineEntry { let date: Date; let title: String }
struct DropProvider: TimelineProvider {
    func placeholder(in context: Context) -> DropEntry { DropEntry(date: .now, title: "Sakura Drift") }
    func getSnapshot(in context: Context, completion: @escaping (DropEntry) -> Void) { completion(placeholder(in: context)) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<DropEntry>) -> Void) {
        let selected = UserDefaults(suiteName: "group.com.vibeos.app")?.string(forKey: "selectedTheme")
        let names = ["midnight-glass": "Midnight Glass", "sakura-drift": "Sakura Drift", "abyss": "Abyss", "neon-run": "Neon Run", "desert-hour": "Desert Hour", "orbit": "Orbit"]
        let title = names[selected ?? ""] ?? "Sakura Drift"
        completion(Timeline(entries: [DropEntry(date: .now, title: title)], policy: .after(Calendar.current.date(byAdding: .day, value: 1, to: .now)!)))
    }
}
struct DropWidget: Widget {
    let kind = "VibeOSDaily"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DropProvider()) { entry in
            VStack(alignment: .leading, spacing: 10) {
                Text("✦  VIBEOS · YOUR LOOK").font(.system(size: 10, weight: .bold)).foregroundStyle(Color(hex: "#B8BBFF"))
                Text(entry.title).font(.system(size: 22, weight: .bold, design: .rounded)).foregroundStyle(.white)
            }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading).padding(17)
                .containerBackground(LinearGradient(colors: [Color(hex: "#151D45"), Color(hex: "#381A55")], startPoint: .topLeading, endPoint: .bottomTrailing), for: .widget)
                .widgetURL(URL(string: "vibeos://theme/\(entry.title.lowercased().replacingOccurrences(of: " ", with: "-"))"))
        }.configurationDisplayName("Your VibeOS look").description("Your selected screen style.").supportedFamilies([.systemSmall, .systemMedium])
    }
}
private extension Color { init(hex: String) { let n = UInt64(hex.dropFirst(), radix: 16) ?? 0; self.init(red: Double((n >> 16) & 255) / 255, green: Double((n >> 8) & 255) / 255, blue: Double(n & 255) / 255) } }
@main struct VibeWidgets: WidgetBundle { var body: some Widget { DropWidget() } }
