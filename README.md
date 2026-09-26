# VibeOS

**VibeOS** is a native iOS + Android personalization app built around one product promise:

> See a look. Apply the whole look.

It combines coordinated wallpapers, widgets, icons, Live Worlds, AI-assisted theme creation, Daily Drops and Premium in one dark-glass experience.

## Native apps

| Platform | Stack | Live Worlds |
| --- | --- | --- |
| Android | Kotlin + Jetpack Compose | Real `WallpaperService`, touch-reactive procedural engine |
| iOS | SwiftUI + WidgetKit | Core Motion + touch interactive preview, Photos wallpaper export |

Both apps share the same product/navigation contract while respecting platform capabilities.

## Implemented product flow

- premium custom onboarding
- Discover / Trending / New / Popular surfaces
- Daily Drop retention card
- complete Theme detail and setup flow
- custom dark glass design system
- Live Worlds discovery + interactive previews
- Android live wallpaper engine
- iOS motion/touch Live World preview
- AI Theme Studio photo/text flow with safe local fallback generation
- My Screen collection surface
- real daily open streak
- WidgetKit + Android home-screen widget
- Firebase Analytics event plumbing
- Firebase Crashlytics integration path
- FCM / APNs notification plumbing
- rich push Notification Service Extension on iOS
- push/deep-link routing
- Firebase Remote Config offers + Daily Drop
- RevenueCat purchase/restore
- limited-time offers that appear only when a real future expiry is configured
- daily local engagement notifications
- native CI for Android and iOS

## Repository

```
android/                  Native Android app
ios/                      Native iOS app + extensions
docs/PRODUCT_CONTRACT.md  Shared UX / analytics / platform contract
.github/workflows/ci.yml  Native build checks
```

## Design rule

Android deliberately does **not** expose stock Material primary controls in the product experience. Buttons, cards, pills, previews and bottom navigation are VibeOS components so Android and iOS feel like the same premium product rather than two default-template apps.

## External configuration

No production keys are committed.

- Android: see `android/SETUP.md`
- iOS: see `ios/SETUP.md`

You still need to supply your own Firebase app configuration, RevenueCat public SDK keys/products, signing teams/keystores and final store assets.

## AI Theme Studio

The current mobile code contains a deterministic on-device fallback so the UI and conversion funnel work without shipping an AI provider secret in the app. Production image/theme generation should be called through a server-side endpoint; never embed a FAL/OpenAI/etc. secret directly in either mobile binary.

## Platform truthfulness

Android can set a real live wallpaper through `WallpaperService`.

On iOS, VibeOS does not pretend to bypass system wallpaper controls. It renders the interactive experience inside the app and exports the approved wallpaper to Photos for the user to apply through iOS.
