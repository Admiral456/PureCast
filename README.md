# PureCast

PureCast is an Android app for sending original photos and videos from a phone to a Smart TV over the local Wi-Fi network.

## Core rule: Direct Play

PureCast does not transcode, recompress, resize, or change the selected media. It serves the original content and asks a compatible DLNA renderer to play that URL. If the TV cannot decode the container, video codec, audio codec, frame rate, or HDR format, playback may fail. PureCast must report that limitation rather than silently lowering quality.

## Current implementation

- Kotlin + Jetpack Compose Android application.
- SSDP discovery for UPnP/DLNA MediaRenderer devices.
- UPnP AVTransport SetAVTransportURI and Play requests.
- Local HTTP server for the selected content URI, without a converted copy.
- HTTP byte-range responses for seeking.
- Android Photo Picker for user-selected photos and videos.
- GitHub Actions workflow for unit tests and a debug APK build.

## Requirements

- Android Studio with Android SDK Platform 37 installed.
- JDK 17 or newer supported by the selected Android Gradle Plugin.
- Phone and TV on the same local network. Guest Wi-Fi and client isolation can block discovery and playback.

## Build

Open this repository in Android Studio and sync the Gradle project, or run:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to app/build/outputs/apk/debug/app-debug.apk.

## First test

1. Connect the phone and DLNA-capable TV to the same Wi-Fi network.
2. Open PureCast and tap Vyhledat televizory.
3. Choose photos or videos with Vybrat fotky a videa.
4. Tap Přehrát beside a discovered TV.

The first milestone supports TVs that expose a UPnP AVTransport MediaRenderer service. DLNA support as a media server/client alone is not enough; the TV must expose a controllable renderer service.

## Quality and compatibility notes

- Direct Play preserves the bytes of the original file sent by PureCast. It does not guarantee that the TV displays every pixel, frame rate, color format, or HDR mode exactly as the source; that depends on the TV decoder and display pipeline.
- A TV may accept the media URL but reject an unsupported format. No automatic conversion is performed.
- Current HTTP server supports a single byte range per request. Multi-range requests are not implemented yet.
- The first app version keeps its media server alive while the app process remains alive. A dedicated foreground playback service is planned for long sessions when the app is backgrounded.
- Android 17 (API 37) introduces runtime-gated local network access for apps targeting API 37+. This initial build targets API 36. Before raising target SDK to 37, implement and request ACCESS_LOCAL_NETWORK.

See docs/ROADMAP.md for milestones and the test matrix.
