package com.vibeos.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeos.app.model.LiveWorld
import com.vibeos.app.model.ThemePack
import com.vibeos.app.services.AppServices

enum class RootTab(val label: String, val glyph: String) {
    Discover("Discover", "⌂"),
    Live("Live Worlds", "◉"),
    Create("Create", "✦"),
    Mine("My Screen", "▦")
}

fun vibeColor(value: Long) = Color(value.toULong())

@Composable
fun VibeOSRoot(
    externalRoute: String? = null,
    onApplyLiveWorld: (LiveWorld) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vibeos", 0) }
    var onboarded by rememberSaveable { mutableStateOf(prefs.getBoolean("onboarded", false)) }

    VibeBackdrop {
        if (!onboarded) {
            OnboardingScreen {
                prefs.edit().putBoolean("onboarded", true).apply()
                AppServices.analytics.log("onboarding_complete")
                onboarded = true
            }
        } else {
            MainExperience(externalRoute, onApplyLiveWorld, onRequestNotifications)
        }
    }
}

@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("Turn Your Phone\nInto Something Special", "Themes, widgets, icons and Live Worlds — all in one.", "✦"),
        Triple("Pick Your Vibe", "Dark, glass, cute, cars, nature, luxury and more.", "◈"),
        Triple("One Look. One Tap.", "Preview the complete setup before you apply it.", "◎")
    )
    val item = pages[page]

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(.45f))
        Box(
            Modifier.size(220.dp).clip(RoundedCornerShape(48.dp))
                .background(Brush.radialGradient(listOf(VibeColors.Pink, VibeColors.Purple, VibeColors.Blue, Color(0xFF090A11)))),
            contentAlignment = Alignment.Center
        ) {
            Text(item.third, fontSize = 84.sp, color = Color.White, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(38.dp))
        Text(item.first, color = VibeColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 38.sp)
        Spacer(Modifier.height(12.dp))
        Text(item.second, color = VibeColors.Muted, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 23.sp)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach {
                Box(
                    Modifier.width(if (it == page) 28.dp else 8.dp).height(8.dp).clip(CircleShape)
                        .background(if (it == page) Color.White else Color.White.copy(.22f))
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        VibeButton(if (page == pages.lastIndex) "Start Exploring" else "Next", Modifier.fillMaxWidth()) {
            if (page == pages.lastIndex) onDone() else page++
        }
    }
}

@Composable
private fun MainExperience(
    externalRoute: String?,
    onApplyLiveWorld: (LiveWorld) -> Unit,
    onRequestNotifications: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(RootTab.Discover) }
    var selectedTheme by remember { mutableStateOf<ThemePack?>(null) }
    var selectedWorld by remember { mutableStateOf<LiveWorld?>(null) }
    var showPaywall by remember { mutableStateOf(false) }
    var applyingTheme by remember { mutableStateOf<ThemePack?>(null) }
    var premiumActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppServices.purchases.checkPremium { premiumActive = it }
    }

    LaunchedEffect(externalRoute) {
        when (externalRoute?.lowercase()) {
            "discover" -> {
                showPaywall = false
                selectedTheme = null
                selectedWorld = null
                applyingTheme = null
                tab = RootTab.Discover
            }
            "live", "live-worlds" -> {
                showPaywall = false
                selectedTheme = null
                selectedWorld = null
                applyingTheme = null
                tab = RootTab.Live
            }
            "create" -> {
                showPaywall = false
                selectedTheme = null
                selectedWorld = null
                applyingTheme = null
                tab = RootTab.Create
            }
            "mine", "my-screen" -> {
                showPaywall = false
                selectedTheme = null
                selectedWorld = null
                applyingTheme = null
                tab = RootTab.Mine
            }
            "premium", "offer" -> {
                selectedTheme = null
                selectedWorld = null
                applyingTheme = null
                showPaywall = true
            }
        }
        externalRoute?.let {
            AppServices.analytics.log("deep_link_open", mapOf("route" to it.take(40)))
        }
    }

    val route = when {
        showPaywall -> "paywall"
        applyingTheme != null -> "apply"
        selectedTheme != null -> "theme"
        selectedWorld != null -> "world"
        else -> "root"
    }

    AnimatedContent(targetState = route, label = "route") {
        when (it) {
            "paywall" -> PaywallScreen(
                onBack = { showPaywall = false },
                onPurchased = {
                    premiumActive = true
                    showPaywall = false
                }
            )
            "apply" -> ApplyThemeScreen(applyingTheme!!, { applyingTheme = null }, { applyingTheme = null })
            "theme" -> ThemeDetailScreen(
                theme = selectedTheme!!,
                premiumActive = premiumActive,
                onBack = { selectedTheme = null },
                onPremium = { showPaywall = true },
                onApply = { applyingTheme = selectedTheme }
            )
            "world" -> LiveWorldDetailScreen(
                world = selectedWorld!!,
                premiumActive = premiumActive,
                onBack = { selectedWorld = null },
                onApply = { onApplyLiveWorld(selectedWorld!!) },
                onPremium = { showPaywall = true }
            )
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        RootTab.Discover -> DiscoverScreen({ selectedTheme = it }, { showPaywall = true })
                        RootTab.Live -> LiveWorldsScreen { selectedWorld = it }
                        RootTab.Create -> CreateScreen(
                            premiumActive = premiumActive,
                            onPremium = { showPaywall = true },
                            onGeneratedTheme = { selectedTheme = it }
                        )
                        RootTab.Mine -> MyScreen(
                            onPremium = { showPaywall = true },
                            onNotifications = onRequestNotifications,
                            onPremiumRestored = { premiumActive = true }
                        )
                    }
                }
                VibeBottomBar(tab) {
                    tab = it
                    AppServices.analytics.log("tab_open", mapOf("tab" to it.name.lowercase()))
                }
            }
        }
    }
}

@Composable
private fun VibeBottomBar(selected: RootTab, onSelect: (RootTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF08090E)).padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        RootTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                Modifier.width(86.dp).clip(RoundedCornerShape(18.dp)).clickable { onSelect(tab) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(tab.glyph, color = if (active) VibeColors.Pink else VibeColors.Muted, fontSize = 20.sp)
                Text(tab.label, color = if (active) Color.White else VibeColors.Muted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
