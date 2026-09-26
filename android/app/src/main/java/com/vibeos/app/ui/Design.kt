package com.vibeos.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VibeColors {
    val Ink = Color(0xFF07080D)
    val Panel = Color(0xFF11131A)
    val Text = Color(0xFFF7F7FB)
    val Muted = Color(0xFF9A9DAC)
    val Cyan = Color(0xFF42D8FF)
    val Blue = Color(0xFF4278FF)
    val Purple = Color(0xFF8B48FF)
    val Pink = Color(0xFFF238DF)
    val Green = Color(0xFF45E6AA)
    val Gold = Color(0xFFFFC95A)
}

@Composable
fun VibeBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF05060A), Color(0xFF0A0B12), Color(0xFF07080D)))
        ),
        content = content
    )
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, radius: Int = 24, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .09f), Color.White.copy(alpha = .035f))))
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(radius.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun VibeButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(VibeColors.Cyan, VibeColors.Blue, VibeColors.Purple, VibeColors.Pink))
                else Brush.horizontalGradient(listOf(Color(0xFF313440), Color(0xFF252833)))
            )
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VibeSecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = .07f))
            .border(1.dp, Color.White.copy(alpha = .11f), RoundedCornerShape(17.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = VibeColors.Text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun VibePill(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.White else Color.White.copy(alpha = .07f))
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Color.Black else Color.White.copy(alpha = .82f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = VibeColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Black)
        subtitle?.let { Text(it, color = VibeColors.Muted, fontSize = 14.sp) }
    }
}
