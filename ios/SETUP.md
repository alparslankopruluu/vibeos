# iOS setup

VibeOS is native SwiftUI. The checked-in project definition uses XcodeGen so the project stays reviewable instead of committing a large generated pbxproj.

1. Install XcodeGen with Homebrew.
2. Run `xcodegen generate` inside the `ios` directory.
3. Open `VibeOS.xcodeproj`.
4. Add your Firebase `GoogleService-Info.plist` to the VibeOS app target. The app still launches without it; Firebase features stay disabled.
5. Put the public RevenueCat Apple API key in `VIBE_REVENUECAT_API_KEY` in `project.yml` before generating the project, or in the generated Info.plist.
6. Enable Push Notifications and In-App Purchase in Signing & Capabilities with your Apple Developer team.
7. Configure the RevenueCat entitlement id as `premium` and provide an annual package in the current offering.
8. Configure Firebase Remote Config keys: `daily_drop_theme_id`, `offer_title`, `offer_subtitle`, `offer_expiry_epoch`.

## Live Worlds on iOS

Live World previews are native SwiftUI Canvas scenes driven by animation, touch and Core Motion. A normal consumer iOS app does not receive a public API to silently replace the user's wallpaper, so VibeOS exports the finished wallpaper to Photos and guides the user through the system wallpaper flow.

The WidgetKit target supplies matching small and medium widgets. The Notification Service Extension supports rich Firebase Cloud Messaging notifications.
