package com.vibeos.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeos.app.model.Catalog
import com.vibeos.app.model.LiveWorld
import com.vibeos.app.services.AppServices

@Composable
fun LiveWorldsScreen(onWorld: (LiveWorld) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Live Worlds", "Interactive, animated, living wallpapers.") }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Nature", "Space", "Cars", "Animals").forEachIndexed { index, label ->
                    VibePill(label, selected = index == 0)
                }
            }
        }

        items(Catalog.liveWorlds, key = { it.id }) { world ->
            GlassCard(
                Modifier.fillMaxWidth().clickable {
                    AppServices.analytics.log("live_world_open", mapOf("world_id" to world.id))
                    onWorld(world)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(84.dp).clip(RoundedCornerShape(21.dp))
                            .background(Brush.linearGradient(listOf(vibeColor(world.accentA), vibeColor(world.accentB)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(world.emoji, fontSize = 38.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(world.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(world.subtitle, color = VibeColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("▶ Preview", color = VibeColors.Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (world.premium) {
                        Text("PRO", color = VibeColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveWorldDetailScreen(
    world: LiveWorld,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onPremium: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            LiveWorldPreview(world, Modifier.fillMaxSize())
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleAction("‹", onBack)
                Spacer(Modifier.weight(1f))
                CircleAction("♡") { AppServices.analytics.log("live_world_favorite", mapOf("world_id" to world.id)) }
            }

            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(world.name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(world.subtitle, color = Color.White.copy(.78f), fontSize = 14.sp)
                Spacer(Modifier.height(13.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    world.features.forEach { VibePill(it) }
                }
            }
        }

        Column(Modifier.padding(18.dp)) {
            VibeButton(
                if (world.premium) "Unlock & Set Live World" else "Set as Live Wallpaper",
                Modifier.fillMaxWidth()
            ) {
                if (world.premium) {
                    onPremium()
                } else {
                    AppServices.analytics.log("live_world_apply_tap", mapOf("world_id" to world.id))
                    onApply()
                }
            }
        }
    }
}

@Composable
private fun LiveWorldPreview(world: LiveWorld, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "live_world")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "phase"
    )
    var touch by remember { mutableStateOf(Offset(.5f, .5f)) }

    Canvas(
        modifier.pointerInput(Unit) {
            detectTapGestures { point ->
                touch = Offset(point.x / size.width, point.y / size.height)
            }
        }
    ) {
        drawRect(
            Brush.linearGradient(
                listOf(vibeColor(world.accentA), vibeColor(world.accentB), Color(0xFF050711)),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )

        repeat(34) { i ->
            val x = (i * 97f + phase * size.width * (0.15f + (i % 5) * .03f)) % size.width
            val y = (i * 167f + touch.y * 90f + phase * 100f) % size.height
            drawCircle(
                color = Color.White.copy(alpha = .15f + (i % 4) * .07f),
                radius = 2f + (i % 5),
                center = Offset(x + (touch.x - .5f) * (i % 4) * 24f, y)
            )
        }

        drawCircle(
            color = Color.White.copy(.10f),
            radius = 110f + 20f * phase,
            center = Offset(size.width * touch.x, size.height * touch.y)
        )
    }
}
