package com.vibeos.app.model

data class ThemePack(
    val id: String,
    val name: String,
    val subtitle: String,
    val tags: List<String>,
    val accentA: Long,
    val accentB: Long,
    val premium: Boolean = false,
    val likes: String = "12K",
    val installs: String = "84K",
)

data class LiveWorld(
    val id: String,
    val name: String,
    val subtitle: String,
    val emoji: String,
    val accentA: Long,
    val accentB: Long,
    val features: List<String>,
    val premium: Boolean = false,
)

data class LimitedOffer(
    val id: String,
    val title: String,
    val subtitle: String,
    val expiresAtMillis: Long,
)

object Catalog {
    val themes = listOf(
        ThemePack("midnight_glass", "Midnight Glass", "Dreamy dark glass with dynamic depth.", listOf("Dark", "Glass", "Popular"), 0xFF2357FF, 0xFFD826FF),
        ThemePack("sakura_night", "Sakura Night", "Soft pink neon under a midnight sky.", listOf("Cute", "Neon"), 0xFFFF5CB8, 0xFF6C4CFF, premium = true),
        ThemePack("black_velocity", "Black Velocity", "Luxury automotive-inspired dark setup.", listOf("Cars", "Luxury"), 0xFFFF315C, 0xFF3D3BFF, premium = true),
        ThemePack("soft_minimal", "Soft Minimal", "Warm calm surfaces with clean widgets.", listOf("Minimal", "Calm"), 0xFFF0C69E, 0xFF8C6F63)
    )

    val liveWorlds = listOf(
        LiveWorld("ocean", "Ocean Life", "Swim with vibrant marine life.", "🐋", 0xFF00C6FF, 0xFF0068FF, listOf("Motion React", "Touch", "Day & Night", "Particles")),
        LiveWorld("night_drive", "Night Drive", "A rainy city drive that moves with your phone.", "🏎️", 0xFFFF315C, 0xFF6E32FF, listOf("Gyroscope", "Rain", "Parallax"), premium = true),
        LiveWorld("galaxy", "Galaxy Journey", "Explore space in 3D with tilt and touch.", "🪐", 0xFF3D7BFF, 0xFFD237FF, listOf("Motion React", "Touch", "Stars"), premium = true),
        LiveWorld("forest", "Forest Spirit", "A calm living forest with soft light.", "🦌", 0xFF16C784, 0xFF0A6D5B, listOf("Day & Night", "Particles", "Calm mode"))
    )
}
