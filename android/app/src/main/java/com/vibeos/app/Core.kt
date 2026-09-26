package com.vibeos.app

import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import org.json.JSONObject
import java.net.URL

class VibeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.initializeApp(this) != null) {
            installVibeAppCheck()
            FirebaseRemoteConfig.getInstance().fetchAndActivate()
        }
        if (BuildConfig.REVENUECAT_KEY.isNotBlank()) {
            Purchases.configure(PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_KEY).build())
        }
    }
}

data class Theme(val id: String, val title: String, val category: String, val subtitle: String, val colors: List<Int>, val world: String, val premium: Boolean)

object Catalog {
    fun bundled(context: Context): List<Theme> = parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
    fun parse(json: String): List<Theme> {
        val array = JSONObject(json).getJSONArray("themes")
        return (0 until array.length()).map { n ->
            val item = array.getJSONObject(n)
            val colors = item.getJSONArray("colors")
            Theme(item.getString("id"), item.getString("title"), item.getString("category"), item.getString("subtitle"), (0 until colors.length()).map { android.graphics.Color.parseColor(colors.getString(it)) }, item.getString("world"), item.getBoolean("premium"))
        }
    }
    fun fetch(context: Context, onResult: (List<Theme>) -> Unit) {
        val remote = if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseRemoteConfig.getInstance().getString("catalog_url") else ""
        val endpoint = remote.ifBlank { BuildConfig.CATALOG_URL }
        if (!endpoint.startsWith("https://")) return
        Thread {
            try {
                val connection = URL(endpoint).openConnection().apply { connectTimeout = 6000; readTimeout = 6000 }
                val text = connection.getInputStream().bufferedReader().use { it.readText() }
                val themes = parse(text).takeIf { it.isNotEmpty() } ?: return@Thread
                (context as? android.app.Activity)?.runOnUiThread { onResult(themes) }
            } catch (e: Exception) { Telemetry.error(context, e) }
        }.start()
    }
    fun daily(context: Context, themes: List<Theme>): Theme = themes.firstOrNull {
        it.id == (if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseRemoteConfig.getInstance().getString("daily_drop_theme_id") else "sakura-drift")
    } ?: themes[(java.time.LocalDate.now().dayOfYear % themes.size)]
}

object Telemetry {
    fun event(context: Context, name: String, themeId: String? = null) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseAnalytics.getInstance(context).logEvent(name, Bundle().apply { themeId?.let { putString("theme_id", it) } })
    }
    fun error(context: Context, error: Throwable) {
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseCrashlytics.getInstance().recordException(error)
    }
}

object Offers {
    fun active(context: Context): Pair<String, java.time.Instant>? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        val config = FirebaseRemoteConfig.getInstance()
        val id = config.getString("offer_id")
        val expiry = runCatching { java.time.Instant.parse(config.getString("offer_ends_at")) }.getOrNull()
        return if (id.isNotBlank() && expiry != null && expiry.isAfter(java.time.Instant.now())) id to expiry else null
    }
}
