package com.vibeos.app.services

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import com.vibeos.app.BuildConfig
import com.vibeos.app.model.LimitedOffer

object AppServices {
    lateinit var analytics: AnalyticsService
        private set
    lateinit var purchases: PurchaseManager
        private set
    lateinit var remoteConfig: RemoteConfigService
        private set
    lateinit var engagement: EngagementTracker
        private set

    fun initialize(context: Context) {
        val firebaseReady = FirebaseBootstrap.initialize(context)
        analytics = AnalyticsService(context, firebaseReady)
        remoteConfig = RemoteConfigService(firebaseReady)
        purchases = PurchaseManager(context)
        engagement = EngagementTracker(context)

        val streak = engagement.recordOpen()
        remoteConfig.refresh()
        analytics.log("app_open", mapOf("streak" to streak.toString()))
    }
}

private object FirebaseBootstrap {
    fun initialize(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) return true
        if (BuildConfig.FIREBASE_APP_ID.isBlank() || BuildConfig.FIREBASE_PROJECT_ID.isBlank()) return false

        return runCatching {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context, options)
            true
        }.getOrDefault(false)
    }
}

class AnalyticsService(private val context: Context, private val firebaseReady: Boolean) {
    fun log(name: String, params: Map<String, String> = emptyMap()) {
        if (!firebaseReady) return
        runCatching {
            val bundle = Bundle().apply {
                params.forEach { (key, value) -> putString(key, value) }
            }
            FirebaseAnalytics.getInstance(context).logEvent(name.take(40), bundle)
        }
    }

    fun nonFatal(t: Throwable, key: String = "unknown") {
        if (!firebaseReady) return
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("vibe_context", key)
                recordException(t)
            }
        }
    }
}

class RemoteConfigService(private val firebaseReady: Boolean) {
    @Volatile
    var dailyDropThemeId: String = "sakura_night"
        private set

    @Volatile
    var limitedOffer: LimitedOffer = fallbackOffer()
        private set

    fun refresh() {
        if (!firebaseReady) return

        runCatching {
            val rc = FirebaseRemoteConfig.getInstance()
            rc.setDefaultsAsync(
                mapOf(
                    "daily_drop_theme_id" to "sakura_night",
                    "offer_title" to "Limited-time Premium offer",
                    "offer_subtitle" to "Special annual plan",
                    "offer_expiry_epoch" to 0L
                )
            )

            rc.fetchAndActivate().addOnCompleteListener {
                dailyDropThemeId = rc.getString("daily_drop_theme_id")
                    .ifBlank { "sakura_night" }

                val nowSeconds = System.currentTimeMillis() / 1000L
                val expirySeconds = rc.getLong("offer_expiry_epoch")

                limitedOffer = if (expirySeconds > nowSeconds) {
                    LimitedOffer(
                        id = "remote_offer",
                        title = rc.getString("offer_title")
                            .ifBlank { "Limited-time Premium offer" },
                        subtitle = rc.getString("offer_subtitle")
                            .ifBlank { "Special annual plan" },
                        expiresAtMillis = expirySeconds * 1000L
                    )
                } else {
                    fallbackOffer()
                }
            }
        }.onFailure {
            runCatching { analytics.nonFatal(it, "remote_config_fetch") }
        }
    }

    private companion object {
        fun fallbackOffer() = LimitedOffer(
            id = "standard",
            title = "VibeOS Premium",
            subtitle = "Unlock the complete VibeOS experience.",
            expiresAtMillis = 0L
        )
    }
}

class PurchaseManager(context: Context) {
    val configured: Boolean = BuildConfig.REVENUECAT_API_KEY.isNotBlank()

    init {
        if (configured) {
            runCatching {
                Purchases.logLevel = LogLevel.INFO
                Purchases.configure(
                    PurchasesConfiguration.Builder(context, BuildConfig.REVENUECAT_API_KEY).build()
                )
            }
        }
    }

    fun checkPremium(onResult: (Boolean) -> Unit) {
        if (!configured) {
            onResult(false)
            return
        }

        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { onResult(false) },
            onSuccess = { info ->
                onResult(info.entitlements["premium"]?.isActive == true)
            }
        )
    }

    fun purchaseAnnual(activity: Activity, onResult: (Boolean, String?) -> Unit) {
        if (!configured) {
            onResult(false, "RevenueCat key is not configured")
            return
        }

        Purchases.sharedInstance.getOfferingsWith(
            onError = { onResult(false, it.message) },
            onSuccess = { offerings ->
                val packageToBuy = offerings.current?.annual
                if (packageToBuy == null) {
                    onResult(false, "Annual package is missing in the current offering")
                    return@getOfferingsWith
                }

                val params = PurchaseParams.Builder(activity, packageToBuy).build()
                Purchases.sharedInstance.purchaseWith(
                    purchaseParams = params,
                    onError = { error, cancelled ->
                        onResult(false, if (cancelled) "cancelled" else error.message)
                    },
                    onSuccess = { _, info ->
                        onResult(info.entitlements["premium"]?.isActive == true, null)
                    }
                )
            }
        )
    }

    fun restore(onResult: (Boolean) -> Unit) {
        if (!configured) {
            onResult(false)
            return
        }

        Purchases.sharedInstance.restorePurchasesWith(
            onError = { onResult(false) },
            onSuccess = { onResult(it.entitlements["premium"]?.isActive == true) }
        )
    }
}
