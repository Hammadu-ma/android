# Alif Med — Native Kotlin WebView App

A native Android app (Kotlin + Jetpack Compose, no Flutter, no React Native) wrapping **alifmeta.vercel.app**.

## Why this feels different from a typical "Kotlin WebView wrapper"
Most hand-rolled Kotlin WebView tutorials use old-style XML layouts and a bare `WebView`, which is why they often feel janky. This project instead uses:

- **Jetpack Compose** for the UI shell (modern, smooth, hardware-accelerated by default)
- **Android's native splash screen API** (`androidx.core:core-splashscreen`) — the same system-level splash Android 12+ apps use, not a fake in-app screen
- **A JS bridge that hooks your site's client-side router** (`history.pushState`/`popstate`) so navigating between pages *inside* the SPA also triggers a native crossfade — not just full page reloads
- **WebView's ServiceWorkerController** — if `alifmeta.vercel.app` registers a service worker (typical for a modern Vite/React app), the WebView lets it actually run, giving you real offline caching driven by your own site rather than a guess bolted on from the app side
- A thin native progress bar + crossfade transition on every navigation, full load or in-app route change
- Pull-to-refresh, offline detection with auto-recovery, and back button that navigates WebView history first

## Expected size
Single architecture (arm64 only, which is virtually every phone made since ~2017), no bundled framework runtime: expect roughly **3–5 MB**. This is the realistic floor without dropping the WebView UI entirely.

## Build it (Codemagic, no local install needed)
1. Push this folder to a GitHub repo
2. Connect it in Codemagic
3. It auto-detects `codemagic.yaml` — pick the **"Alif Med Native - Android"** workflow (switch from the default UI workflow if that's what shows first)
4. Start build → download the `.apk` from Artifacts when it finishes

## Build it locally instead
```
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (signed with a debug key here for testing — see below before publishing).

## Changing the URL
```kotlin
// app/src/main/kotlin/com/alifmed/app/WebViewScreen.kt
private const val APP_URL = "https://alifmeta.vercel.app"
```

## Before publishing to the Play Store
This currently signs release builds with the debug key (fine for installing on your own phone / sharing informally). For the Play Store, generate a real signing key and update the `signingConfig` in `app/build.gradle.kts`.

## If offline caching doesn't seem to work
It depends on `alifmeta.vercel.app` having a registered service worker with a caching strategy — that's what actually makes pages available offline; the app enables the *capability*, but the caching behavior itself is controlled by your site's own service worker (or lack of one). If your site doesn't have one yet, that's a change on the Vercel/Vite side, not something fixable purely from the Android app.
