package com.vibeos.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeos.app.services.AppServices
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun PaywallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val offer = AppServices.remoteConfig.limitedOffer
    var remaining by remember {
        mutableLongStateOf(max(0L, offer.expiresAtMillis - System.currentTimeMillis()))
    }
    var buying by remember { mutableStateOf(false) }

    LaunchedEffect(offer.id) {
        AppServices.analytics.log("paywall_view", mapOf("offer_id" to offer.id))
    }

    LaunchedEffect(offer.expiresAtMillis) {
        while (remaining > 0) {
            delay(1000)
            remaining = max(0L, offer.expiresAtMillis - System.currentTimeMillis())
        }
    }

    val totalSeconds = remaining / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val countdown = "%02d:%02d:%02d".format(hours, minutes, seconds)

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            CircleAction("×", onBack)
        }

        Spacer(Modifier.height(12.dp))
        Text("♛", fontSize = 50.sp, color = VibeColors.Gold)
        Text("VibeOS Premium", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(
            "Unlimited themes. Live Worlds. AI creation. No ads.",
            color = VibeColors.Muted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        if (offer.expiresAtMillis > System.currentTimeMillis()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(offer.title, color = VibeColors.Pink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(offer.subtitle, color = Color.White.copy(.82f), fontSize = 13.sp)
                Spacer(Modifier.height(9.dp))
                Text(countdown, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(17.dp))
        }

        listOf(
            "All premium themes",
            "All Live Worlds",
            "AI Theme Studio credits",
            "New drops every week",
            "No ads",
            "Early access"
        ).forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                Text("✓", color = VibeColors.Gold, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(10.dp))
                Text(line, color = Color.White.copy(.9f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        GlassCard(Modifier.fillMaxWidth()) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Yearly", color = Color.White, fontWeight = FontWeight.Black)
                    Text("3 days free, then annual", color = VibeColors.Muted, fontSize = 12.sp)
                }
                Text("BEST VALUE", color = VibeColors.Pink, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(12.dp))

        VibeButton(
            if (buying) "Opening Store…" else "Try Premium Free",
            Modifier.fillMaxWidth(),
            enabled = !buying && activity != null
        ) {
            val host = activity ?: return@VibeButton
            buying = true
            AppServices.analytics.log("paywall_cta_tap", mapOf("offer_id" to offer.id))
            AppServices.purchases.purchaseAnnual(host) { success, error ->
                buying = false
                AppServices.analytics.log(
                    "purchase_result",
                    mapOf("success" to success.toString(), "error" to (error ?: ""))
                )
                Toast.makeText(
                    context,
                    if (success) "Premium unlocked" else (error ?: "Purchase failed"),
                    Toast.LENGTH_SHORT
                ).show()
                if (success) onBack()
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Restore Purchase",
            color = VibeColors.Muted,
            fontSize = 12.sp,
            modifier = Modifier.clickable {
                AppServices.purchases.restore { restored ->
                    AppServices.analytics.log(
                        "restore_purchase_result",
                        mapOf("restored" to restored.toString())
                    )
                }
            }
        )
    }
}
