import WidgetKit
import SwiftUI

struct VibeEntry: TimelineEntry {
    let date: Date
}

struct VibeProvider: TimelineProvider {
    func placeholder(in context: Context) -> VibeEntry {
        VibeEntry(date: Date())
    }

    func getSnapshot(in context: Context, completion: @escaping (VibeEntry) -> Void) {
        completion(VibeEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<VibeEntry>) -> Void) {
        let now = Date()
        let entries = (0..<6).compactMap { hour in
            Calendar.current.date(byAdding: .hour, value: hour, to: now).map(VibeEntry.init)
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

struct VibeWidgetView: View {
    let entry: VibeEntry

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.08, green: 0.12, blue: 0.35), .purple, .pink.opacity(0.8)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            VStack(alignment: .leading, spacing: 4) {
                Text("VibeOS")
                    .font(.caption2.weight(.black))
                    .foregroundStyle(.white.opacity(0.75))
                Text(entry.date, style: .time)
                    .font(.system(size: 27, weight: .light, design: .rounded))
                Spacer(minLength: 0)
                HStack {
                    Text("✦ Midnight Glass")
                        .font(.caption.weight(.bold))
                    Spacer()
                    Text("◉")
                }
            }
            .foregroundStyle(.white)
            .padding(14)
        }
        .containerBackground(for: .widget) {
            Color.black
        }
    }
}

struct VibeThemeWidget: Widget {
    let kind = "VibeThemeWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: VibeProvider()) { entry in
            VibeWidgetView(entry: entry)
        }
        .configurationDisplayName("Vibe Theme")
        .description("A matching clock and theme widget for your current VibeOS look.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main
struct VibeOSWidgetBundle: WidgetBundle {
    var body: some Widget {
        VibeThemeWidget()
    }
}
