package com.vibeos.app.services

import com.vibeos.app.model.ThemePack
import kotlin.math.absoluteValue

object ThemeGenerationService {
    /**
     * Deterministic on-device fallback. Production can replace this adapter with a backend
     * that returns the same ThemePack contract after image/prompt generation.
     */
    fun generate(prompt: String): ThemePack {
        val seed = prompt.hashCode().absoluteValue
        val palettes = listOf(
            0xFF2357FFL to 0xFFD826FFL,
            0xFFFF315CL to 0xFF6E32FFL,
            0xFF00C6FFL to 0xFF0068FFL,
            0xFFFF5CB8L to 0xFFFF9F6EL,
            0xFF16C784L to 0xFF0A6D5BL
        )
        val palette = palettes[seed % palettes.size]
        return ThemePack(
            id = "generated_" + seed,
            name = prompt.take(28).ifBlank { "My Vibe" },
            subtitle = "Generated from your vibe.",
            tags = listOf("AI", "Custom", "Personal"),
            accentA = palette.first,
            accentB = palette.second,
            premium = true,
            likes = "NEW",
            installs = "1"
        )
    }
}
