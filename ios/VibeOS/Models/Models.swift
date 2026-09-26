import SwiftUI

struct ThemePack: Identifiable, Hashable {
    let id: String
    let name: String
    let subtitle: String
    let tags: [String]
    let a: Color
    let b: Color
    var premium = false
    var likes = "12K"
    var installs = "84K"
}

struct LiveWorld: Identifiable, Hashable {
    let id: String
    let name: String
    let subtitle: String
    let emoji: String
    let a: Color
    let b: Color
    let features: [String]
    var premium = false
}

struct LimitedOffer {
    let id: String
    let title: String
    let subtitle: String
    let expiry: Date
}

enum Catalog {
    static let themes: [ThemePack] = [
        .init(id: "midnight_glass", name: "Midnight Glass", subtitle: "Dreamy dark glass with dynamic depth.", tags: ["Dark","Glass","Popular"], a: .blue, b: .purple),
        .init(id: "sakura_night", name: "Sakura Night", subtitle: "Soft pink neon under a midnight sky.", tags: ["Cute","Neon"], a: .pink, b: .purple, premium: true),
        .init(id: "black_velocity", name: "Black Velocity", subtitle: "Luxury automotive-inspired dark setup.", tags: ["Cars","Luxury"], a: .red, b: .indigo, premium: true),
        .init(id: "soft_minimal", name: "Soft Minimal", subtitle: "Warm calm surfaces with clean widgets.", tags: ["Minimal","Calm"], a: Color(red: .91, green: .73, blue: .56), b: Color(red: .48, green: .37, blue: .34))
    ]

    static let liveWorlds: [LiveWorld] = [
        .init(id: "ocean", name: "Ocean Life", subtitle: "Swim with vibrant marine life.", emoji: "🐋", a: .cyan, b: .blue, features: ["Motion React","Touch","Day & Night","Particles"]),
        .init(id: "night_drive", name: "Night Drive", subtitle: "A rainy city drive that moves with your phone.", emoji: "🏎️", a: .red, b: .purple, features: ["Motion React","Rain","Parallax"], premium: true),
        .init(id: "galaxy", name: "Galaxy Journey", subtitle: "Explore space in depth with tilt and touch.", emoji: "🪐", a: .blue, b: .pink, features: ["Motion React","Touch","Stars"], premium: true),
        .init(id: "forest", name: "Forest Spirit", subtitle: "A calm living forest with soft light.", emoji: "🦌", a: .green, b: .teal, features: ["Day & Night","Particles","Calm mode"])
    ]
}
