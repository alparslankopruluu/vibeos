# Android setup

VibeOS Android is native Kotlin + Jetpack Compose with a custom visual system. Primary surfaces intentionally do not use stock Material button/card styling.

## Toolchain

- JDK 17
- Android Gradle Plugin 9.4.x
- Gradle 9.6
- compileSdk / targetSdk 37
- minSdk 26

AGP 9 uses built-in Kotlin. Do not add the old `org.jetbrains.kotlin.android` plugin back.

## Runtime configuration

Put these values in untracked `~/.gradle/gradle.properties`, local `gradle.properties`, or CI environment variables:

```properties
VIBE_FIREBASE_API_KEY=
VIBE_FIREBASE_APP_ID=
VIBE_FIREBASE_PROJECT_ID=
VIBE_FIREBASE_SENDER_ID=
VIBE_REVENUECAT_API_KEY=
```

No secret is committed. Without these values the UI still launches; Firebase/RevenueCat network features gracefully stay disabled.

For a production Play release, also add the normal Firebase Android app to the Firebase project and configure Crashlytics mapping upload in your release pipeline.

## RevenueCat

- Entitlement: `premium`
- Current offering: include an annual package
- The paywall reads the actual store price from RevenueCat during purchase; visual copy must remain consistent with App Store / Play Console pricing.

## Remote Config

Both platforms share:

- `daily_drop_theme_id`
- `offer_title`
- `offer_subtitle`
- `offer_expiry_epoch` — Unix timestamp in **seconds**

## Daily engagement

When the user explicitly enables Daily Drop notifications, Android schedules a unique WorkManager job targeting 19:00 local time. Remote push notifications remain separate and are delivered through FCM.

## Live Worlds

Android uses a real `WallpaperService`; the preview and wallpaper engine are separate so richer GPU/rendering implementations can replace the current procedural scene without changing product navigation.
