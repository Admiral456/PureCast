# PureCast

PureCast is an Android application for sending original photos and videos from phones to Smart TVs over the local Wi-Fi network.

## Platform scope

- Android only. No iOS application or iOS build is planned in the current scope.
- Target phone families: Samsung Galaxy, Xiaomi and Redmi.
- The same Android APK runs on all three families. Brand detection is used for diagnostics; the transfer implementation is manufacturer-neutral.
- Other compatible Android phones are not blocked.
- Multiple Android phones can contribute to a temporary shared queue on the same Wi-Fi network. One phone hosts the session and controls DLNA playback; each sender keeps and streams its own original media.

## Core rule: Direct Play

PureCast does not transcode, recompress, resize, or change the selected media. It serves the selected source URI and asks a compatible DLNA renderer or TV browser to play it. If the TV cannot decode the container, video codec, audio codec, frame rate, or HDR format, playback may fail. PureCast must report that limitation rather than silently lowering quality.

## Current implementation — 0.8.0

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
- GitHub Actions CI builds debug, R8-optimized release and unminified release APK variants.
- CI checks that DLNA commands, local media routes, shared-queue API endpoints and playback UI strings remain in both release packages after packaging.
- CI uploads R8 mapping/usage/kept-class reports so removed code can be reviewed.
- The unminified release variant uses the same source and features but disables R8 and resource shrinking. It exists for size comparison and diagnosis, not because the optimized build intentionally omits features.

## Shared queue requirements and limitations

- All phones and the TV must use the same trusted, non-guest Wi-Fi network without client/AP isolation.
- The host app must remain running. A sender phone must remain available while its queued item is being played; force-stop, process termination or a network change can break its media URL.
- Shared media URLs expire after six hours. Re-add an item if its link expires.
- The shared session is in memory only. Closing the host app or stopping its process ends the group; reconnecting after a process restart is not supported yet.
- The LAN control API uses HTTP without TLS. Pair only with people you trust on a private network; do not use the feature on public or untrusted Wi-Fi.
- Shared playback is currently controlled by the host through DLNA. TV-browser playback is not remotely controlled by the shared queue.

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
gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:assembleUnminified
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The R8-optimized release APK is written to `app/build/outputs/apk/release/app-release.apk`. The full-size, unminified release APK is written to `app/build/outputs/apk/unminified/app-unminified.apk`. CI signs both release variants with its temporary debug certificate for sideload testing; this is not a Play Store signing key.

## Download

The latest successful build is available from [GitHub Actions artifacts](https://github.com/Admiral456/PureCast/actions). Download `PureCast-release-apk` for the recommended R8-optimized installation build, `PureCast-full-apk` for the same source with shrinking disabled, or `PureCast-debug-apk` for the debug build. `PureCast-r8-audit` contains the class mapping, kept-class list and removed-code report.

## Why the optimized APK is small

PureCast does not bundle a video transcoder, codec framework, sample photos, sample videos, downloaded media or large image assets. It serves original files from Android content URIs and asks the TV to decode them. The release APK is therefore mostly app code and a small resource table. R8 removes unused parts of the AndroidX/Compose dependency graph; the unminified release is used for the single user-facing APK while R8 remains an internal comparison. CI verifies critical feature markers in both release packages. A small APK is not proof of missing functionality, and adding dummy files to increase its size would not make the app more complete.

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

See [docs/APK-SIZE-AUDIT.md](docs/APK-SIZE-AUDIT.md) for the measured package-size analysis, [docs/DEVICE-COMPATIBILITY.md](docs/DEVICE-COMPATIBILITY.md), [docs/MULTI-PHONE.md](docs/MULTI-PHONE.md) and [docs/ROADMAP.md](docs/ROADMAP.md).
