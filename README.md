# PureCast

PureCast is an Android application for sending original photos and videos from phones to Smart TVs over the local Wi-Fi network.

## Platform scope

- Android only. No iOS application or iOS build is planned in the current scope.
- Target phone families: Samsung Galaxy, Xiaomi and Redmi.
- The same Android APK runs on all three families. Brand detection is used for diagnostics; the transfer implementation is manufacturer-neutral.
- Other compatible Android phones are not blocked.
- Multiple phones can independently send media to the same TV. A normal single DLNA renderer plays one current item; a new sender can replace it. A shared group queue is a future feature; the current playback queue is local to each sender and is not synchronized between phones.

## Core rule: Direct Play

PureCast does not transcode, recompress, resize, or change the selected media. It serves the selected source URI and asks a compatible DLNA renderer or TV browser to play it. If the TV cannot decode the container, video codec, audio codec, frame rate, or HDR format, playback may fail. PureCast must report that limitation rather than silently lowering quality.

## Current implementation — 0.5.0

- Kotlin + Jetpack Compose, Czech UI, light/dark themes.
- Sender device detection for Samsung, Xiaomi, Redmi and generic Android.
- TV brand/platform detection from UPnP manufacturer, model and friendly-name information.
- SSDP discovery for UPnP/DLNA MediaRenderer devices across common UPnP search targets, preferring the active Wi-Fi route when multiple networks or VPNs are present.
- Secure XML parsing and optional UPnP URLBase handling.
- AVTransport play, pause, resume, stop, seek, playback-state and position queries.
- ConnectionManager GetProtocolInfo parsing with conservative MIME-type compatibility reporting.
- Local HTTP streaming of the original selected URI with byte-range support and Wi-Fi-first address selection.
- TV browser fallback page with responsive playback UI and a basic HTML5 MIME support hint.
- Foreground media-playback service to keep streaming active when the app is backgrounded or the display turns off.
- Android Photo Picker with persistent URI permission when the provider supports it.
- Image and video thumbnail previews, lazily loaded as gallery items become visible, with memory-conscious handling for older Android versions.
- Gallery search by filename and photo/video filters.
- Progressive technical metadata inspection in small batches, with cancellation when a new selection replaces the previous one.
- Technical metadata inspection for image size and video resolution, frame rate, video/audio MIME codecs, bitrate, duration and available HDR transfer metadata.
- Unit tests for HTTP range handling, UPnP time values, DLNA MIME profiles and sender/receiver brand classification.
- GitHub Actions CI for unit tests, debug APK build and artifact upload.

## Receiver interoperability

- Samsung Smart TVs: try DLNA/UPnP AVTransport when exposed; otherwise use the TV web-browser route when supported by the model and browser.
- LG webOS TVs: try DLNA/UPnP AVTransport when exposed; otherwise use the TV web-browser route.
- Other Smart TVs: use standard DLNA/UPnP or browser playback when available.

Samsung Smart TVs from 2016 onward use the Tizen platform, but codec and media support differs between model groups and years. A detected Samsung brand is not a blanket guarantee that every media type or control command will work.

## Requirements

- Android Studio with Android SDK Platform 36 installed.
- JDK 17 or newer supported by the selected Android Gradle Plugin.
- Phone and TV on the same non-guest Wi-Fi network. Client isolation, VPNs and router multicast filtering can block discovery or playback.

## Build

Open this repository in Android Studio and sync the Gradle project, or run:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Download

The latest successful build is available from [GitHub Actions artifacts](https://github.com/Admiral456/PureCast/actions). Open the newest successful **Android CI** run and download `PureCast-debug-apk`. The ZIP contains `app-debug.apk`.

## Quality and compatibility notes

- Direct Play preserves the bytes of the original file served by PureCast. It does not guarantee that the TV displays every pixel, frame rate, color format or HDR mode exactly as the source; that depends on the TV decoder and display pipeline.
- DLNA ConnectionManager MIME profiles are only an advertised container/content-type hint. They do not prove support for a particular video/audio codec, frame rate, bitrate, HDR profile or resolution.
- The browser playback page uses the TV browser's own media engine. A browser MIME hint is not proof that a particular codec/bitrate/HDR combination will play.
- Unsupported or unverified media is not silently converted.
- The HTTP server currently supports single byte ranges; multipart ranges are not implemented.
- Foreground streaming continues while Android keeps the foreground service alive. Battery-saver policies and force-stop can still interrupt it.
- This build targets Android API 36. Before raising target SDK to Android 17/API 37, implement and request the runtime `ACCESS_LOCAL_NETWORK` permission.

## Hardware test status

CI verifies compilation, unit tests and APK packaging. It does **not** prove that a physical Samsung or LG television accepts playback. Samsung publishes model-group-specific decoder specifications, so exact model and firmware testing is required.

See [docs/DEVICE-COMPATIBILITY.md](docs/DEVICE-COMPATIBILITY.md), [docs/MULTI-PHONE.md](docs/MULTI-PHONE.md) and [docs/ROADMAP.md](docs/ROADMAP.md).
