# VibeOS setup

## Android

Open `android/` in Android Studio with JDK 17 and Android SDK 35. Build a debug app without credentials using the bundled catalog. Add `android/app/google-services.json` to activate Firebase, then enable Analytics, Crashlytics, Cloud Messaging, Remote Config and App Check (Play Integrity). Register the debug App Check token for local builds. Add to `android/local.properties`:

```properties
REVENUECAT_ANDROID_KEY=goog_...
VIBEOS_CATALOG_URL=https://your-cdn.example/catalog.json
```

Create a RevenueCat entitlement named `premium`, attach it to a current offering, and publish products in Google Play. Android 13+ asks for notification permission only after the user opts into daily reminders.

## iOS

Install XcodeGen (`brew install xcodegen`), run `cd ios && xcodegen generate`, and open `VibeOS.xcodeproj`. Change bundle IDs and App Group `group.com.vibeos.app` for your team, enable Push Notifications, App Attest and App Groups in Apple Developer. Register App Check for iOS and its debug token for simulator builds. Add `ios/VibeOS/GoogleService-Info.plist` and set the values in `ios/Config.xcconfig`:

```text
REVENUECAT_IOS_KEY = appl_...
VIBEOS_CATALOG_URL = https:/$()/your-cdn.example/catalog.json
```

Xcconfig treats `//` as a comment, hence `$()` separates the slashes. Set the iOS RevenueCat entitlement to `premium` and configure App Store Connect products. Enable APNs in Firebase and upload an APNs authentication key. The widget and main target need the same App Group.

## Catalog and offers

Publish the structure in `shared/catalog.json` on an HTTPS endpoint. The `catalog_url` and `daily_drop_theme_id` Firebase Remote Config parameters override local defaults. The catalog loader validates JSON, uses a bundled fallback, and keeps the app usable offline. Never put private API credentials in the app or catalog.

RevenueCat's current offering supplies localized prices. Configure a trial or discount in the store and offering; the app does not invent a countdown or promise a trial when the account is ineligible. Notification payloads can specify `type`, `theme_id`, and a `vibeos://theme/<id>` route. AI Studio calls the authenticated Firebase `generateTheme` function. Enable Anonymous Auth, deploy Firestore and Storage rules, set the `OPENAI_API_KEY` secret, and deploy `functions/`. The server permits two free creations per Firebase UID per UTC day, saves the image privately, and returns a 24-hour signed download URL. The callable enforces App Check. Register Play Integrity/App Attest and the debug tokens, and grant the function service account the App Check Token Verifier role. Add server-side subscription verification before public AI credit monetization. No API key is stored in the mobile apps.

## Platform limits

Android applies the Live World via the system wallpaper confirmation screen. The static home wallpaper applies directly. Widget placement and third party launcher icons require user action. iOS saves a generated wallpaper to Photos for the user to select in Settings; live motion runs in the app preview. iOS cannot change the device wallpaper or other apps' icons programmatically.
