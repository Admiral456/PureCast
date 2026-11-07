# PureCast

PureCast is an Android app for sending original photos and videos from a phone to a Smart TV over the local Wi-Fi network.

## Core rule: Direct Play

PureCast does not transcode, recompress, resize, or change the selected media. It serves the selected source URI and asks a compatible DLNA renderer or TV browser to play it. If the TV cannot decode the container, video codec, audio codec, frame rate, or HDR format, playback may fail. PureCast must report that limitation rather than silently lowering quality.

## Current implementation — 0.2.0

- Kotlin + Jetpack Compose, Czech UI, light/dark themes.
- SSDP discovery for UPnP/DLNA MediaRenderer devices across common UPnP search targets.
- Secure XML parsing and optional UPnP URLBase handling.
- AVTransport play, pause, resume, stop, seek, playback-state and position queries.
- ConnectionManager GetProtocolInfo parsing with conservative MIME-type compatibility reporting.
- Local HTTP streaming of the original selected URI with byte-range support.
- Browser fallback page for TVs that can open the phone's LAN URL but do not expose a controllable DLNA renderer.
- Foreground media-playback service to keep streaming active when the app is backgrounded or the display turns off.
- Android Photo Picker with persistent URI permission when the provider supports it.
- Technical metadata inspection for image size and video resolution, frame rate, video/audio MIME codecs, bitrate, duration and available HDR transfer metadata.
- Unit tests for HTTP range handling, UPnP time values and DLNA MIME-profile matching.
- GitHub Actions CI for unit tests, debug APK build and artifact upload.

## Requirements

- Android Studio with Android SDK Platform 36 installed.
- JDK 17 or newer supported by the selected Android Gradle Plugin.
- Phone and TV on the same local Wi-Fi network. Guest Wi-Fi, client isolation, VPNs and router multicast filtering can block discovery or playback.

## Build

Open this repository in Android Studio and sync the Gradle project, or run:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Download

The latest successful build is available from [GitHub Actions artifacts](https://github.com/Admiral456/PureCast/actions). Open the newest successful **Android CI** run and download `PureCast-debug-apk`. The ZIP contains `app-debug.apk`.

## Basic use

1. Connect the phone and TV to the same non-guest Wi-Fi network.
2. Open PureCast and tap **Vyhledat televizory**.
3. Select photos and videos with **Vybrat fotky a videa**.
4. Use **DLNA** for a discovered renderer, or **Prohlížeč TV** to prepare a local page to open in the TV's web browser.
5. Use pause/resume/stop/seek controls when the renderer reports support. The playback notification also provides an **Ukončit přenos** action.

## Quality and compatibility notes

- Direct Play preserves the bytes of the original file served by PureCast. It does not guarantee that the TV displays every pixel, frame rate, color format or HDR mode exactly as the source; that depends on the TV decoder and display pipeline.
- DLNA ConnectionManager MIME profiles are only an advertised container/content-type hint. They do not prove support for a particular video/audio codec, frame rate, bitrate, HDR profile or resolution.
- Unsupported or unverified media is not silently converted. The application reports uncertainty or an error instead.
- TV browser playback depends on that browser's own supported media formats. The browser fallback is not equivalent to DLNA remote control.
- The HTTP server currently supports single byte ranges; multipart ranges are not implemented.
- Foreground streaming continues while PureCast is backgrounded as long as Android keeps the foreground service alive. Battery-saver policies and force-stop can still interrupt it.
- This build targets Android API 36. Before raising target SDK to Android 17/API 37, implement and request the runtime `ACCESS_LOCAL_NETWORK` permission.

## Hardware test status

CI verifies compilation, unit tests and APK packaging. It does **not** prove that a physical television accepts playback. The LG 55UJ7507 is listed by LG with DLNA media support, but it still needs a direct test to determine whether its firmware exposes a controllable DLNA MediaRenderer or whether the browser fallback is the usable route.

See [docs/ROADMAP.md](docs/ROADMAP.md) for the next milestones and acceptance tests.
