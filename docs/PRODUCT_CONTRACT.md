# VibeOS product contract

This file is the shared contract for native iOS and Android implementations.

## Navigation

1. Onboarding
2. Discover
3. Theme detail
4. Apply / customize
5. Live Worlds
6. AI Theme Studio
7. My Screen
8. Premium

The bottom navigation remains **Discover / Live Worlds / Create / My Screen** on both platforms.

## Visual rules

- Dark-first.
- Edge-to-edge cinematic previews.
- 18–30 pt continuous corner radii.
- Glass cards: translucent white surface + subtle 1 px white border.
- Primary CTA: cyan → blue → purple → pink gradient.
- Gold is reserved for Premium/value signals.
- No stock Material button appearance on Android.
- No default blue SwiftUI CTA styling on iOS.
- Theme previews should look like a complete phone setup, not wallpaper thumbnails.

## Theme package

Every remote theme should be able to describe:

- id / title / subtitle
- category + tags
- Home and Lock wallpaper assets
- icon pack
- widget presets
- optional Live World companion
- premium/free state
- rollout state
- preview media
- light/dark variants

## Shared lifecycle events

Core events must keep the same semantic names across platforms:

- `app_open`
- `onboarding_complete`
- `tab_open`
- `theme_open`
- `theme_customize_tap`
- `wallpaper_apply_success` / `wallpaper_export_success`
- `live_world_open`
- `live_world_apply_tap` / `live_world_export_tap`
- `ai_source_selected`
- `ai_theme_generate_start`
- `ai_theme_generate_complete`
- `paywall_cta_tap`
- `purchase_result`
- `restore_purchase_result`
- `notification_permission_result`
- `daily_drop_notification_scheduled`
- `push_received`
- `push_open`
- `deep_link_open`

Do not log raw user photos, AI prompt contents, FCM tokens, email addresses, or other user content as analytics parameters.

## Platform capability rule

Android can provide a real live wallpaper through `WallpaperService`.

iOS Live Worlds remain interactive inside VibeOS and export an approved wallpaper asset to Photos. The product must not claim that VibeOS can silently bypass Apple's wallpaper controls.


## Limited-offer integrity

A countdown is rendered only when `offer_expiry_epoch` is a real future Unix timestamp supplied by Remote Config. The local fallback never fabricates a resetting countdown.
