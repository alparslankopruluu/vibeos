# Implementation status

## Built

- Native Compose and SwiftUI discovery, detail, Live Worlds, Create, My Screen, onboarding and premium UI with custom controls.
- Android sensor/touch Live Wallpaper, static wallpaper, widget, daily local notification, FCM routing.
- iOS motion/touch preview, artwork export to Photos, WidgetKit widget, local notification, APNs/FCM routing.
- Bundled offline theme catalog, optional HTTPS catalog and Remote Config daily-drop override.
- Firebase Analytics/Crashlytics events and RevenueCat offerings, purchase, restore and entitlement gating.
- Callable text or photo inspired AI wallpaper generation with anonymous auth, two free generations per day, private Storage object and a temporary signed URL.

## Before production

- Supply platform Firebase files, signing capabilities, APNs, RevenueCat keys/products and the OpenAI server secret.
- Register App Check with Play Integrity and App Attest, verify RevenueCat subscription entitlements on the server, and add a paid AI credit ledger before selling generations.
- Expand the four original bundled artworks into a full curated catalog of icon packs, widget variants and high resolution previews. Third party icons cannot be installed in one tap across stock iOS and Android launchers.
- Configure a real RevenueCat offer placement, eligibility and localized terms. The countdown appears only with a future Remote Config expiry and a matching offering; the store remains authoritative for purchase terms.
- Test on physical devices for wallpaper, widgets, push, purchase and Apple Photos permissions. CI checks compilation but cannot validate store transaction or APNs delivery.
