package com.vibeos.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeos.app.model.Catalog
import com.vibeos.app.model.ThemePack
import com.vibeos.app.services.AppServices
import com.vibeos.app.services.ThemeInstaller

@Composable
fun DiscoverScreen(onTheme: (ThemePack) -> Unit, onPremium: () -> Unit) {
    val categories = listOf("For You", "Trending", "New", "Popular")
    var category by remember { mutableStateOf("For You") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("VibeOS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("Make your phone feel yours.", color = VibeColors.Muted, fontSize = 13.sp)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(VibeColors.Gold.copy(.13f))
                        .border(1.dp, VibeColors.Gold.copy(.35f), CircleShape).clickable { onPremium() },
                    contentAlignment = Alignment.Center
                ) { Text("♛", color = VibeColors.Gold, fontSize = 20.sp) }
            }
        }

        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { label ->
                    VibePill(label, selected = label == category) { category = label }
                }
            }
        }

        item { DailyDropCard(onPremium) }

        items(Catalog.themes, key = { it.id }) { theme ->
            ThemeHeroCard(theme) {
                AppServices.analytics.log("theme_open", mapOf("theme_id" to theme.id))
                onTheme(theme)
            }
        }
    }
}

@Composable
private fun DailyDropCard(onTap: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(17.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF4FB2), Color(0xFF7A45FF)))),
                contentAlignment = Alignment.Center
            ) { Text("✦", color = Color.White, fontSize = 24.sp) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("DAILY DROP", color = VibeColors.Pink, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text("Sakura Night", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("Free today • refreshes in 24h", color = VibeColors.Muted, fontSize = 12.sp)
            }
            Text("→", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable { onTap() })
        }
    }
}

@Composable
private fun ThemeHeroCard(theme: ThemePack, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(.045f))
            .border(1.dp, Color.White.copy(.09f), RoundedCornerShape(30.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp)
    ) {
        ThemePhonePreview(theme, Modifier.fillMaxWidth().height(420.dp))
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(theme.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    if (theme.premium) {
                        Spacer(Modifier.width(7.dp))
                        Text("PRO", color = VibeColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(theme.subtitle, color = VibeColors.Muted, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("♥ " + theme.likes, color = Color.White, fontSize = 12.sp)
                Text("↓ " + theme.installs, color = VibeColors.Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            theme.tags.take(3).forEach { VibePill("#" + it) }
        }
        Spacer(Modifier.height(12.dp))
        VibeButton("Apply This Look", Modifier.fillMaxWidth(), onClick = onClick)
    }
}

@Composable
fun ThemePhonePreview(theme: ThemePack, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    listOf(vibeColor(theme.accentB), vibeColor(theme.accentA), Color(0xFF05070F)),
                    radius = 850f
                )
            )
            .padding(26.dp)
    ) {
        Box(
            Modifier.width(205.dp).fillMaxHeight().align(Alignment.Center)
                .clip(RoundedCornerShape(34.dp))
                .background(Color.Black.copy(.72f))
                .border(1.dp, Color.White.copy(.24f), RoundedCornerShape(34.dp))
                .padding(15.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("9:41", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
                Text("MON, AUG 12", color = Color.White.copy(.68f), fontSize = 9.sp)
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("✦", "◉", "⌁", "◎").forEach { glyph ->
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(.10f))
                                .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(glyph, color = Color.White) }
                    }
                }
            }
        }
        Text(
            "NEW",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart).clip(CircleShape).background(VibeColors.Pink)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ThemeDetailScreen(
    theme: ThemePack,
    premiumActive: Boolean,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onApply: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TopBack(theme.name, onBack) }
        item { ThemePhonePreview(theme, Modifier.fillMaxWidth().height(390.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                VibePill("Lock", true)
                VibePill("Home")
                VibePill("Widgets")
                VibePill("Icons")
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Includes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Wallpapers (Home + Lock)",
                    "86 coordinated app icons",
                    "24 widgets (S, M, L)",
                    "Control center style",
                    "Live World companion",
                    "Charging animation"
                ).forEach { line ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Text("✓", color = VibeColors.Green, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(10.dp))
                        Text(line, color = Color.White.copy(.88f), fontSize = 14.sp)
                    }
                }
            }
        }
        item {
            VibeButton("Apply Complete Theme", Modifier.fillMaxWidth()) {
                if (theme.premium && !premiumActive) onPremium() else onApply()
            }
            Spacer(Modifier.height(9.dp))
            VibeSecondaryButton("Customize", Modifier.fillMaxWidth()) {
                AppServices.analytics.log("theme_customize_tap", mapOf("theme_id" to theme.id))
            }
        }
    }
}

@Composable
fun ApplyThemeScreen(theme: ThemePack, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    var wallpaperDone by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopBack("Apply Theme", onBack)
        Text("Transform your phone step by step.", color = VibeColors.Muted)
        GlassCard(Modifier.fillMaxWidth()) {
            ApplyStep(1, "Set Home Wallpaper", wallpaperDone)
            ApplyStep(2, "Set Lock Screen", false)
            ApplyStep(3, "Install icon shortcuts", false)
            ApplyStep(4, "Add widgets", false)
            ApplyStep(5, "Enable Live World", false)
        }
        Spacer(Modifier.weight(1f))
        VibeButton(if (wallpaperDone) "Continue Setup" else "Apply Wallpaper", Modifier.fillMaxWidth()) {
            if (!wallpaperDone) {
                val result = ThemeInstaller.applyGeneratedWallpaper(context, theme)
                wallpaperDone = result.isSuccess
                AppServices.analytics.log(
                    if (result.isSuccess) "wallpaper_apply_success" else "wallpaper_apply_failed",
                    mapOf("theme_id" to theme.id)
                )
                if (result.isFailure) {
                    Toast.makeText(context, "Wallpaper could not be applied", Toast.LENGTH_SHORT).show()
                }
            } else onDone()
        }
    }
}

@Composable
private fun ApplyStep(number: Int, title: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                .background(if (done) VibeColors.Green.copy(.16f) else Color.White.copy(.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (done) "✓" else number.toString(), color = if (done) VibeColors.Green else Color.White, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(if (done) "Done" else "○", color = if (done) VibeColors.Green else VibeColors.Muted)
    }
}

@Composable
fun TopBack(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CircleAction("‹", onBack)
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text("♡", color = Color.White, fontSize = 22.sp)
    }
}

@Composable
fun CircleAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(.35f))
            .border(1.dp, Color.White.copy(.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 22.sp) }
}
