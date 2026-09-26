# Android setup

VibeOS Android is native Kotlin + Jetpack Compose with a custom visual system. Primary surfaces intentionally do not use stock Material button/card styling.

## Toolchain

- JDK 17
- Android Gradle Plugin 9.4.x
- Gradle 9.6
- compileSdk / targetSdk 37
- minSdk 26

AGP 9 uses built-in Kotlin. Do not add the old `org.jetbrains.kotlin.android` plugin back.

## Firebase

For production, place the Firebase Android config at:

```
android/app/google-services.json
```

The file is gitignored. When it exists, the build automatically enables the current Google Services and Crashlytics Gradle plugins so Crashlytics receives proper build metadata/mapping support.

For local UI development or CI without the Firebase file, these optional Gradle properties/environment variables can initialize Firebase programmatically:

```properties
VIBE_FIREBASE_API_KEY=
VIBE_FIREBASE_APP_ID=
VIBE_FIREBASE_PROJECT_ID=
VIBE_FIREBASE_SENDER_ID=
```

The app still launches when Firebase is absent; analytics, push, Remote Config and crash reporting simply remain inactive.

## RevenueCat

Supply the public Android SDK key as:

```properties
VIBE_REVENUECAT_API_KEY=
```

Configure:

- entitlement: `premium`
- current offering: annual package
- store-side free trial / intro offer in Play Console

Never embed a RevenueCat secret key or AI-provider secret in the mobile app.

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
