# VibeOS

A native iOS and Android theme app with a coordinated dark glass design. Discover complete looks, preview touch-responsive Live Worlds, apply Android wallpaper, save iPhone wallpapers, use daily widgets, create AI artwork, and unlock premium looks.

## Run

- **Android:** Open [`android/`](android/) in Android Studio (JDK 17, SDK 35). The bundled catalog runs without service credentials.
- **iOS:** Run `xcodegen generate` inside [`ios/`](ios/) and open `VibeOS.xcodeproj` in Xcode 16 or newer.
- **Functions:** Run `npm ci && npm run build` inside [`functions/`](functions/). Connect Firebase, add a server-side image secret, and deploy when ready.

See [setup and platform instructions](docs/SETUP.md). Theme and event contracts and four original catalog wallpapers live in [`shared/`](shared/).

Android Live Worlds run as a real `WallpaperService`; iOS previews move in-app and wallpaper artwork is saved for manual application. All store prices come from RevenueCat offerings, and unavailable purchases stay disabled. Firebase services activate only when platform config files are supplied.
