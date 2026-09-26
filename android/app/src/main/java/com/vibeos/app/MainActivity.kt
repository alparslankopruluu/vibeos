package com.vibeos.app

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import android.graphics.BitmapFactory
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Package as RCPackage
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

private val Ink = Color(0xFF070A16)
private val Soft = Color(0xFFA7A9C5)
private val Accent = Color(0xFFA9A7FF)
private val Glass = Color(0xFF1A1D34)

class MainActivity : ComponentActivity() {
    private val askNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Telemetry.event(this, "notification_permission_result", if (granted) "granted" else "denied")
    }
    private var incoming by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.channel(this)
        incoming = intent.getStringExtra("theme_id") ?: intent.data?.lastPathSegment
        setContent { VibeUI(this, incoming, ::requestDaily) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); incoming = intent.getStringExtra("theme_id") ?: intent.data?.lastPathSegment; Telemetry.event(this, "notification_opened", incoming) }
    private fun requestDaily() {
        getSharedPreferences("vibe", 0).edit().putBoolean("daily_notifications", true).apply()
        Notifications.scheduleDaily(this)
        if (Build.VERSION.SDK_INT >= 33) askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun applyWorld(theme: Theme) {
        getSharedPreferences("vibe", 0).edit().putString("world", theme.world).apply()
        Telemetry.event(this, "theme_apply_started", theme.id)
        try {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, WorldWallpaper::class.java)))
            Telemetry.event(this, "theme_applied", theme.id)
        } catch (e: Exception) { Telemetry.error(this, e); Telemetry.event(this, "theme_apply_failed", theme.id) }
    }
    fun applyStatic(theme: Theme, done: (String) -> Unit) {
        try {
            val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
            val artwork = runCatching { assets.open("${theme.id}.png").use { BitmapFactory.decodeStream(it) } }.getOrNull()
            if (artwork != null) AndroidCanvas(bitmap).drawBitmap(artwork, null, android.graphics.Rect(0, 0, 720, 1280), android.graphics.Paint(3))
            else WorldPainter.draw(AndroidCanvas(bitmap), theme.colors, theme.world, 0f, 0f, 0f)
            WallpaperManager.getInstance(this).setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            bitmap.recycle(); Telemetry.event(this, "theme_applied", theme.id); done("Home wallpaper applied")
        } catch (e: Exception) { Telemetry.error(this, e); Telemetry.event(this, "theme_apply_failed", theme.id); done("Could not set wallpaper") }
    }
}

@Composable private fun VibeUI(activity: MainActivity, incoming: String?, enableDaily: () -> Unit) {
    var themes by remember { mutableStateOf(Catalog.bundled(activity)) }
    var tab by remember { mutableStateOf("Discover") }
    var selected by remember { mutableStateOf<Theme?>(null) }
    var showPaywall by remember { mutableStateOf(false) }
    var premium by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val prefs = remember { activity.getSharedPreferences("vibe", 0) }
    var welcomed by remember { mutableStateOf(prefs.getBoolean("welcomed", false)) }
    LaunchedEffect(Unit) {
        Catalog.fetch(activity) { themes = it }
        if (BuildConfig.REVENUECAT_KEY.isNotBlank()) Purchases.sharedInstance.getCustomerInfoWith({ Telemetry.error(activity, it) }) { premium = it.entitlements["premium"]?.isActive == true }
    }
    LaunchedEffect(incoming, themes) { incoming?.let { id -> themes.firstOrNull { it.id == id }?.let { selected = it } } }
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Ink)) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            if (!welcomed) {
                Onboarding { styles -> prefs.edit().putBoolean("welcomed", true).putString("styles", styles.joinToString(",")).apply(); welcomed = true; Telemetry.event(activity, "onboarding_completed") }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            "Discover" -> Discover(themes.sortedByDescending { prefs.getString("styles", "")?.contains(it.category, true) == true }, Catalog.daily(activity, themes), { selected = it; Telemetry.event(activity, "theme_opened", it.id) }, { selected = it; Telemetry.event(activity, "daily_drop_opened", it.id) })
                            "Worlds" -> Worlds(themes, { selected = it; Telemetry.event(activity, "world_opened", it.id) })
                            "Create" -> CreateScreen(activity) { message = it }
                            else -> Mine(themes, prefs.getString("last_theme", null), { selected = it }, { showPaywall = true }, enableDaily)
                        }
                    }
                    Navigation(tab) { tab = it }
                }
            }
            selected?.let { theme -> Detail(theme, premium, { selected = null }, {
                if (theme.premium && !premium) showPaywall = true else { prefs.edit().putString("last_theme", theme.id).apply(); activity.applyWorld(theme) }
            }, { activity.applyStatic(theme) { message = it } }) }
            if (showPaywall) Paywall(activity, { showPaywall = false }, { premium = true; showPaywall = false })
            if (message.isNotBlank()) Box(Modifier.align(Alignment.BottomCenter).padding(26.dp).clip(RoundedCornerShape(18.dp)).background(Glass).clickable { message = "" }.padding(18.dp)) { Label(message, 14.sp, Color.White) }
        }
    }
}

@Composable private fun Label(value: String, size: TextUnit = 16.sp, color: Color = Color.White, bold: Boolean = false, modifier: Modifier = Modifier) = Text(value, modifier, color = color, fontSize = size, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, lineHeight = size * 1.23f)
@Composable private fun Pill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(if (filled) Brush.horizontalGradient(listOf(Color(0xFF8D82FC), Color(0xFF686BE7))) else Brush.horizontalGradient(listOf(Glass, Glass))).clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 16.dp), Alignment.Center) { Label(text, 14.sp, Color.White, true) }
}
@Composable private fun Section(title: String, accessory: String = "") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Label(title, 23.sp, bold = true); Label(accessory, 12.sp, Accent) } }
@Composable private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Column(modifier.clip(RoundedCornerShape(24.dp)).background(Glass).border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(24.dp)).padding(18.dp), content = content)

@Composable private fun Art(theme: Theme, modifier: Modifier = Modifier, interactive: Boolean = false) {
    var touch by remember { mutableFloatStateOf(0f) }
    var tilt by remember { mutableFloatStateOf(0f) }
    val a = Color(theme.colors[0]); val b = Color(theme.colors[1])
    val context = LocalContext.current
    val artwork = remember(theme.id) { runCatching { context.assets.open("${theme.id}.png").use { BitmapFactory.decodeStream(it) } }.getOrNull() }
    Box(modifier.clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(a, b))).pointerInput(interactive) {
        if (interactive) detectDragGestures { change, _ -> touch += .4f; tilt = (change.position.x / size.width - .5f) * 8f }
    }) {
        if (artwork != null) {
            Image(artwork.asImageBitmap(), theme.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                repeat(35) { i ->
                    val x = ((i * 137 % 103) / 103f * size.width + tilt * (2 + i % 7)).coerceIn(0f, size.width)
                    val y = (i * 71 % 97) / 97f * size.height
                    drawCircle(Color.White.copy(alpha = .25f + (i % 4) * .1f), radius = (1 + i % 3).dp.toPx(), center = Offset(x, y))
                }
            }
        } else androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { native -> WorldPainter.draw(native.nativeCanvas, theme.colors, theme.world, touch, tilt, touch) }
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Label("9:41", 30.sp, bold = true)
            Label("MONDAY · 24", 10.sp, Color.White.copy(alpha = .7f))
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(4) { i -> Box(Modifier.size(29.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = if (i == 0) .27f else .14f))) } }
            Spacer(Modifier.height(8.dp)); Label(theme.title.uppercase(), 9.sp, Color.White.copy(alpha = .7f))
        }
    }
}

@Composable private fun Navigation(active: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF0D1022)).padding(vertical = 12.dp, horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
        listOf("Discover" to "✦", "Worlds" to "◉", "Create" to "＋", "My Screen" to "▣").forEach { (name, symbol) ->
            Column(Modifier.clickable { onSelect(name) }.padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Label(symbol, 22.sp, if (active == name) Accent else Soft); Label(name, 10.sp, if (active == name) Color.White else Soft)
            }
        }
    }
}
@Composable private fun Onboarding(done: (Set<String>) -> Unit) {
    val theme = remember { Catalog.parse("""{"themes":[{"id":"a","title":"Midnight Glass","category":"Glass","subtitle":"","colors":["#090E23","#293A8F","#8A9FFF"],"world":"space","premium":false}]}""").first() }
    var step by remember { mutableIntStateOf(0) }; var chosen by remember { mutableStateOf(setOf<String>()) }
    if (step == 0) Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp)); Label("VIBE / OS", 13.sp, Accent, true); Spacer(Modifier.height(28.dp))
        Art(theme, Modifier.weight(1f).width(230.dp).shadow(28.dp, RoundedCornerShape(26.dp)), true)
        Spacer(Modifier.height(34.dp)); Label("Turn your phone into\nsomething special.", 34.sp, bold = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); Label("Themes, widgets and living worlds — all in one.", 15.sp, Soft)
        Spacer(Modifier.height(26.dp)); Pill("NEXT  →", { step = 1 }, Modifier.fillMaxWidth()); Spacer(Modifier.height(16.dp))
    } else Column(Modifier.fillMaxSize().padding(26.dp)) {
        Spacer(Modifier.height(26.dp)); Label("WHAT'S YOUR VIBE?", 12.sp, Accent, true); Spacer(Modifier.height(14.dp))
        Label("Choose the styles you love.", 31.sp, bold = true); Label("We'll bring your favorites to the front.", 14.sp, Soft)
        Spacer(Modifier.height(24.dp))
        val options = listOf("Glass", "Dark", "Minimal", "Cute", "Nature", "Cars", "Space", "Luxury")
        options.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { style -> Box(Modifier.weight(1f).height(108.dp).padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(if (style in chosen) listOf(Color(0xFF554AA7), Color(0xFF24294D)) else listOf(Glass, Color(0xFF11172B))))
                .border(1.dp, if (style in chosen) Accent else Color.White.copy(alpha = .1f), RoundedCornerShape(20.dp))
                .clickable { chosen = if (style in chosen) chosen - style else chosen + style }.padding(16.dp)) {
                Label(style + if (style in chosen) "  ✓" else "", 17.sp, bold = true, modifier = Modifier.align(Alignment.BottomStart))
            } }
        } }
        Spacer(Modifier.weight(1f)); Pill("CONTINUE (${chosen.size})  →", { done(chosen) }, Modifier.fillMaxWidth())
    }
}
@Composable private fun Discover(themes: List<Theme>, daily: Theme, onOpen: (Theme) -> Unit, openDaily: (Theme) -> Unit) {
    var query by remember { mutableStateOf("") }
    val visible = themes.filter { query.isBlank() || it.title.contains(query, true) || it.category.contains(query, true) || it.world.contains(query, true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Label("VIBE / OS", 13.sp, Accent, true); Spacer(Modifier.height(10.dp)); Label("Find your next look.", 29.sp, bold = true) }; Label("✦", 28.sp, Accent) } }
        item { GlassCard(Modifier.fillMaxWidth()) { BasicTextField(query, { query = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.White, fontSize = 15.sp), singleLine = true, decorationBox = { inner -> Box { if (query.isBlank()) Label("⌕  Search looks, colors, worlds...", 15.sp, Soft); inner() } }) } }
        item { GlassCard(Modifier.fillMaxWidth().clickable { openDaily(daily) }) { Label("✦  TODAY'S DROP", 11.sp, Accent, true); Spacer(Modifier.height(8.dp)); Label(daily.title, 22.sp, bold = true); Label("A fresh look for today · Explore now  →", 13.sp, Soft) } }
        item { Section("Complete looks", "SWIPE TO EXPLORE") }
        items(visible) { theme ->
            Box(Modifier.fillMaxWidth().height(385.dp).clickable { onOpen(theme) }) {
                Art(theme, Modifier.fillMaxSize()); Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Ink))).padding(22.dp)) {
                    Column { Label(theme.category.uppercase() + if (theme.premium) "  ·  PRO" else "  ·  FREE", 11.sp, Accent, true); Spacer(Modifier.height(5.dp)); Label(theme.title, 26.sp, bold = true); Label(theme.subtitle + "    →", 13.sp, Soft) }
                }
            }
        }
    }
}
@Composable private fun Worlds(themes: List<Theme>, onOpen: (Theme) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Label("LIVE WORLDS", 12.sp, Accent, true); Spacer(Modifier.height(9.dp)); Label("Made to move.", 31.sp, bold = true); Label("Touch, tilt and explore. Live wallpaper is available on Android.", 14.sp, Soft) }
        items(themes.distinctBy { it.world }) { theme -> Box(Modifier.fillMaxWidth().height(270.dp).clickable { onOpen(theme) }) { Art(theme, Modifier.fillMaxSize(), true); GlassCard(Modifier.align(Alignment.BottomStart).padding(14.dp)) { Label(theme.title, 18.sp, bold = true); Label("Touch & tilt to interact  →", 12.sp, Soft) } } }
    }
}
@Composable private fun Detail(theme: Theme, premium: Boolean, close: () -> Unit, apply: () -> Unit, static: () -> Unit) {
    var guide by remember(theme.id) { mutableStateOf(false) }
    if (guide) { ApplyGuide(theme, { guide = false }, static, apply); return }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Pill("←", close, filled = false); Label(if (theme.premium) "✦  PREMIUM" else "FREE LOOK", 12.sp, Accent, true) }
        Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Art(theme, Modifier.height(375.dp).width(215.dp), true) }
        Spacer(Modifier.height(24.dp)); Label(theme.title, 32.sp, bold = true); Label(theme.subtitle, 15.sp, Soft); Spacer(Modifier.height(22.dp))
        GlassCard(Modifier.fillMaxWidth()) { Label("IN THIS LOOK", 11.sp, Accent, true); Spacer(Modifier.height(10.dp)); Label("✓  Live World wallpaper", 15.sp); Spacer(Modifier.height(8.dp)); Label("✓  Matching daily widget", 15.sp); Spacer(Modifier.height(8.dp)); Label("✓  Coordinated colors", 15.sp) }
        Spacer(Modifier.height(18.dp)); Pill(if (theme.premium && !premium) "UNLOCK THIS LOOK  ✦" else "APPLY COMPLETE LOOK  →", { if (theme.premium && !premium) apply() else guide = true }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); Pill("SET STATIC HOME WALLPAPER", static, Modifier.fillMaxWidth(), false)
        Spacer(Modifier.height(12.dp)); Label("Add the VibeOS widget from your Home Screen. Other app icons depend on your launcher.", 12.sp, Soft)
    }
}
@Composable private fun ApplyGuide(theme: Theme, close: () -> Unit, static: () -> Unit, live: () -> Unit) {
    var completed by remember(theme.id) { mutableStateOf(setOf<Int>()) }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(22.dp)) {
        Pill("←  LOOK PREVIEW", close, filled = false); Spacer(Modifier.height(30.dp))
        Label("APPLY YOUR THEME", 12.sp, Accent, true); Spacer(Modifier.height(12.dp))
        Label("Your screen, step by step.", 29.sp, bold = true)
        Label("Android confirms live wallpaper in a system screen. Other steps depend on your launcher.", 14.sp, Soft)
        Spacer(Modifier.height(25.dp))
        listOf("Set Home Wallpaper" to "Apply the matching static artwork", "Set Lock Screen" to "Choose the image in system wallpaper settings", "Add Widget" to "Long press Home → Widgets → VibeOS", "App Icons" to "Your launcher controls third party icons", "Live World" to "Open Android's live wallpaper preview").forEachIndexed { index, item ->
            GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable {
                if (index == 0) static()
                if (index == 4) live()
                completed = completed + index
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(if (index in completed) Accent else Color(0xFF34375C)), Alignment.Center) { Label(if (index in completed) "✓" else "${index + 1}", 16.sp, bold = true) }
                    Spacer(Modifier.width(12.dp)); Column { Label(item.first, 16.sp, bold = true); Label(item.second, 12.sp, Soft) }
                }
            }
        }
        Spacer(Modifier.height(12.dp)); Pill("OPEN LIVE WALLPAPER  →", live, Modifier.fillMaxWidth())
    }
}
@Composable private fun Mine(themes: List<Theme>, lastId: String?, onOpen: (Theme) -> Unit, paywall: () -> Unit, enableDaily: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Label("MY SCREEN", 12.sp, Accent, true); Spacer(Modifier.height(8.dp)); Label("Make it feel like you.", 30.sp, bold = true); Spacer(Modifier.height(24.dp))
        val current = themes.firstOrNull { it.id == lastId } ?: themes.first()
        Art(current, Modifier.fillMaxWidth().height(300.dp).clickable { onOpen(current) }); Spacer(Modifier.height(20.dp))
        GlassCard(Modifier.fillMaxWidth()) { Label("Your current look", 12.sp, Soft); Spacer(Modifier.height(6.dp)); Label(current.title, 21.sp, bold = true) }
        Spacer(Modifier.height(14.dp)); Pill("EXPLORE PREMIUM", paywall, Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)); Pill("ENABLE DAILY DROP REMINDERS", enableDaily, Modifier.fillMaxWidth(), false)
        Spacer(Modifier.height(16.dp)); Label("Widgets: long press your Home Screen, select Widgets, then VibeOS.", 13.sp, Soft)
    }
}
@Composable private fun CreateScreen(activity: MainActivity, report: (String) -> Unit) {
    var prompt by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; var generated by remember { mutableStateOf<Bitmap?>(null) }
    var fromPhoto by remember { mutableStateOf(true) }; var photo by remember { mutableStateOf<Bitmap?>(null) }; var photoBase64 by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) try {
            val source = activity.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: error("Could not read image")
            val scale = minOf(1f, 768f / maxOf(source.width, source.height))
            val small = Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
            val bytes = ByteArrayOutputStream().also { small.compress(Bitmap.CompressFormat.JPEG, 72, it) }.toByteArray()
            photo = small; photoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) { Telemetry.error(activity, e); report("Could not read this photo") }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Label("CREATE STUDIO", 12.sp, Accent, true); Spacer(Modifier.height(8.dp)); Label("Your vision.\nYour screen.", 33.sp, bold = true)
        Spacer(Modifier.height(12.dp)); Label("Describe a world and generate a matching wallpaper with your connected AI service.", 14.sp, Soft)
        Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("FROM PHOTO", { fromPhoto = true }, Modifier.weight(1f), fromPhoto)
            Pill("FROM TEXT", { fromPhoto = false }, Modifier.weight(1f), !fromPhoto)
        }
        if (fromPhoto) {
            Spacer(Modifier.height(15.dp)); photo?.let { Image(it.asImageBitmap(), "Selected photo", Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop) }
            Pill(if (photo == null) "UPLOAD A PHOTO  ＋" else "CHANGE PHOTO  ↻", { picker.launch("image/*") }, Modifier.fillMaxWidth(), false)
        }
        Spacer(Modifier.height(24.dp)); GlassCard(Modifier.fillMaxWidth()) {
            Label("DESCRIBE YOUR LOOK", 11.sp, Accent, true); Spacer(Modifier.height(13.dp))
            BasicTextField(prompt, { prompt = it }, Modifier.fillMaxWidth().heightIn(min = 100.dp), textStyle = TextStyle(color = Color.White, fontSize = 17.sp), decorationBox = { inner -> Box { if (prompt.isBlank()) Label("Rainy Tokyo, violet neon, dark glass...", 17.sp, Soft); inner() } })
        }
        Spacer(Modifier.height(12.dp)); Label("A selected photo is sent to the image service for generation. The source photo is not stored by VibeOS.", 12.sp, Soft)
        Spacer(Modifier.height(24.dp)); Pill(if (busy) "CREATING..." else "GENERATE MY LOOK  ✦", {
            val description = prompt.ifBlank { if (fromPhoto && photoBase64 != null) "Create a coordinated theme from my photo" else "" }
            if (busy || description.length < 8 || (fromPhoto && photoBase64 == null)) { report("Add a photo and describe your look"); return@Pill }
            if (FirebaseApp.getApps(activity).isEmpty()) { report("Connect Firebase to use AI Studio"); return@Pill }
            busy = true; Telemetry.event(activity, "create_started")
            fun generate() {
                FirebaseFunctions.getInstance().getHttpsCallable("generateTheme").call(buildMap<String, Any> { put("prompt", description); if (fromPhoto) photoBase64?.let { put("imageBase64", it) } }).addOnCompleteListener { task ->
                    if (!task.isSuccessful) { busy = false; report(task.exception?.localizedMessage ?: "Generation failed"); Telemetry.event(activity, "create_failed"); return@addOnCompleteListener }
                    val url = (task.result?.data as? Map<*, *>)?.get("wallpaperUrl") as? String
                    if (url == null) { busy = false; report("No wallpaper returned"); return@addOnCompleteListener }
                    Thread {
                        try {
                            val bitmap = URL(url).openStream().use { BitmapFactory.decodeStream(it) } ?: error("Invalid image")
                            activity.runOnUiThread { generated = bitmap; busy = false; Telemetry.event(activity, "create_completed") }
                        } catch (e: Exception) { Telemetry.error(activity, e); activity.runOnUiThread { busy = false; report("Could not load this wallpaper") } }
                    }.start()
                }
            }
            if (FirebaseAuth.getInstance().currentUser != null) generate()
            else FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener { if (it.isSuccessful) generate() else { busy = false; report("Sign-in unavailable") } }
        }, Modifier.fillMaxWidth())
        generated?.let { bitmap ->
            Spacer(Modifier.height(18.dp)); Image(bitmap.asImageBitmap(), "Generated wallpaper", Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(12.dp)); Pill("SET AS HOME WALLPAPER", {
                try { WallpaperManager.getInstance(activity).setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM); report("Wallpaper applied") }
                catch (e: Exception) { Telemetry.error(activity, e); report("Could not apply wallpaper") }
            }, Modifier.fillMaxWidth(), false)
        }
    }
}
@Composable private fun Paywall(activity: MainActivity, close: () -> Unit, success: () -> Unit) {
    var packages by remember { mutableStateOf<List<RCPackage>>(emptyList()) }; var chosen by remember { mutableIntStateOf(0) }; var error by remember { mutableStateOf("") }
    val offer = remember { Offers.active(activity) }; var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(offer) { while (offer != null) { seconds = ((offer.second.toEpochMilli() - System.currentTimeMillis()) / 1000).coerceAtLeast(0); kotlinx.coroutines.delay(1000) } }
    LaunchedEffect(Unit) {
        Telemetry.event(activity, "paywall_viewed")
        if (BuildConfig.REVENUECAT_KEY.isNotBlank()) Purchases.sharedInstance.getOfferingsWith({ error = "Plans are unavailable right now" }) { offerings ->
            val selected = offer?.let { offerings[it.first] }
            packages = (selected ?: offerings.current)?.availablePackages.orEmpty()
            if (selected != null) Telemetry.event(activity, "offer_viewed")
        }
    }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(26.dp)) {
        Pill("✕", close, filled = false); Spacer(Modifier.height(38.dp)); Label("✦  VIBEOS PREMIUM", 13.sp, Accent, true)
        Spacer(Modifier.height(20.dp)); Label("Every look.\nEvery world.", 39.sp, bold = true)
        if (offer != null && seconds > 0 && packages.isNotEmpty()) { Spacer(Modifier.height(12.dp)); Label("LIMITED OFFER  ·  ${seconds / 3600}h ${(seconds % 3600) / 60}m remaining", 13.sp, Accent, true) }
        Spacer(Modifier.height(12.dp)); Label("Unlock all complete looks, live worlds and new drops as they arrive.", 16.sp, Soft)
        Spacer(Modifier.height(30.dp)); listOf("All premium themes", "Every Live World", "New drops and collections").forEach { Label("✓  $it", 17.sp); Spacer(Modifier.height(14.dp)) }
        Spacer(Modifier.height(18.dp)); if (packages.isEmpty()) Label("Store plans will appear here when configured.", 14.sp, Soft)
        packages.forEachIndexed { index, item -> Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, if (chosen == index) Accent else Soft.copy(alpha = .3f), RoundedCornerShape(18.dp)).clickable { chosen = index }.padding(18.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Label(item.storeProduct.title, 15.sp, bold = true); Label(item.storeProduct.price.formatted, 15.sp, Accent) } } }
        Spacer(Modifier.height(16.dp)); Pill("CONTINUE", {
            packages.getOrNull(chosen)?.let { pack ->
                Telemetry.event(activity, "purchase_started")
                Purchases.sharedInstance.purchaseWith(PurchaseParams.Builder(activity, pack).build(), onError = { err, cancelled -> if (!cancelled) { error = err.message; Telemetry.event(activity, "purchase_failed") } }, onSuccess = { _, info -> if (info.entitlements["premium"]?.isActive == true) { Telemetry.event(activity, "purchase_completed"); success() } })
            }
        }, Modifier.fillMaxWidth()); if (error.isNotBlank()) Label(error, 13.sp, Soft)
        Spacer(Modifier.height(12.dp)); Label("The store confirms the actual price and any eligible trial before purchase.", 12.sp, Soft, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp)); Text("Restore purchases", Modifier.fillMaxWidth().clickable { if (BuildConfig.REVENUECAT_KEY.isNotBlank()) Purchases.sharedInstance.restorePurchasesWith({ error = it.message }) { if (it.entitlements["premium"]?.isActive == true) success() } }, color = Accent, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
