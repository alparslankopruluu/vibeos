# iOS setup

VibeOS is native SwiftUI. The checked-in project definition uses XcodeGen so the project stays reviewable instead of committing a large generated pbxproj.

1. Install XcodeGen with Homebrew.
2. Run `xcodegen generate` inside the `ios` directory.
3. Open `VibeOS.xcodeproj`.
4. Add your Firebase `GoogleService-Info.plist` to `ios/VibeOS/` and the **VibeOS** app target. The app still launches without it; Firebase features stay disabled.
5. Set the public RevenueCat Apple SDK key in the `VIBE_REVENUECAT_API_KEY` build setting. Do not commit private backend secrets.
6. Enable Push Notifications and In-App Purchase in Signing & Capabilities with your Apple Developer team.
7. Configure the RevenueCat entitlement as `premium` and provide an annual package in the current offering.
8. Configure Firebase Remote Config keys used by both platforms: `daily_drop_theme_id`, `offer_title`, `offer_subtitle`, `offer_expiry_epoch`.

When the Firebase plist exists, the generated Xcode project also runs Firebase's Crashlytics dSYM upload script after a successful app build.

## iOS Live Worlds

The interactive scene runs natively inside VibeOS using Core Motion + SwiftUI Canvas. Standard consumer iOS apps cannot silently replace the user's wallpaper. VibeOS saves the finished wallpaper to Photos and guides the user through the system wallpaper flow instead of pretending a private API exists.

The WidgetKit target supplies matching small and medium widgets. The Notification Service Extension supports rich Firebase Cloud Messaging notifications.
