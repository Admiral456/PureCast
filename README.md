# PureCast

PureCast is an Android application for sending original photos and videos from phones to Smart TVs over the local Wi-Fi network.

## Platform scope

- Android only. No iOS application or iOS build is planned in the current scope.
- Target phone families: Samsung Galaxy, Xiaomi and Redmi.
- The same Android APK runs on all three families. Brand detection is used for diagnostics; the transfer implementation is manufacturer-neutral.
- Other compatible Android phones are not blocked.
- Multiple Android phones can contribute to a temporary shared queue on the same Wi-Fi network. One phone hosts the session and controls DLNA playback; each sender keeps and streams its own original media. Shared sessions cap membership at 16 phones, rate-limit incorrect invite codes, and bound rate-limit tracking memory. Shared queue client requests use the selected Wi-Fi route and bounded JSON response reads; the server accepts only strictly framed, fixed-length JSON requests and rejects ambiguous HTTP bodies.

## Core rule: Direct Play

PureCast does not transcode, recompress, resize, or change the selected media. It serves the selected source URI and asks a compatible DLNA renderer or TV browser to play it. If the TV cannot decode the container, video codec, audio codec, frame rate, or HDR format, playback may fail. PureCast must report that limitation rather than silently lowering quality.

## Current implementation — 0.20.0

- Kotlin + Jetpack Compose, Czech UI, light/dark themes, custom adaptive PureCast launcher icon.
- Sender device detection for Samsung, Xiaomi, Redmi and generic Android.
- TV brand/platform detection from UPnP manufacturer, model and friendly-name information.
- SSDP discovery for UPnP/DLNA MediaRenderer devices across common UPnP search targets, preferring the active Wi-Fi route when multiple networks or VPNs are present.
- Secure XML parsing and optional UPnP URLBase handling.
- AVTransport play, pause, resume, stop, seek, playback-state and position queries.
- Automatic DLNA playback-state and timeline refresh every three seconds, with pause/resume/stop controls enabled according to the TV-reported state.
- ConnectionManager GetProtocolInfo parsing with conservative MIME-type compatibility reporting; DLNA SOAP commands use the active Wi-Fi network when available.
- Local HTTP streaming of the original selected URI with byte-range support, Wi-Fi-interface binding, bounded client concurrency (8 workers plus a bounded queue), validated MIME response headers and active-connection cleanup. Large transfers use reusable 256 KiB per-worker copy buffers, a 64 KiB response buffer and a best-effort 512 KiB TCP send-buffer hint. Copy loops handle short or temporarily zero-length reads and verify exact range lengths without buffering entire media files.
- Strict shared-queue HTTP request parsing: validates request lines and header syntax, rejects duplicate headers, chunked or encoded bodies, requires a valid fixed Content-Length and JSON content type for POST, caps request sizes, and handles zero-length bulk reads without hanging.
- TV browser fallback page with responsive playback UI, video MIME hints and actual image load/error feedback including decoded dimensions. The image element and its MIME feedback are checked in the packaged APK.
- Foreground media-playback service to keep streaming active when the app is backgrounded or the display turns off.
- Android Photo Picker plus a multi-select Android document picker for image/video files and less common formats, with persistent URI permission where supported. Non-media documents are skipped and cannot be sent to the TV or added to playback queues.
- Optional full gallery access using Android runtime permissions, requested only after the user taps the gallery access control.
- Permission-aware MediaStore gallery browsing with full, selected-only, images-only, videos-only and denied states; Android 14+ selected-photo access is supported.
- A clear in-app permission explanation, a live permission status label, a gallery refresh action and a shortcut to Android app settings. The Photo Picker and document picker remain usable when gallery access is denied.
- Image MIME recognition by extension for JPG/JPEG, PNG/APNG, GIF, WebP, HEIC/HEIF, AVIF, JPEG XL, TIFF, BMP, SVG, JPEG 2000, OpenEXR, DDS, KTX/KTX2, FITS, QOI, and camera RAW formats.
- Original image MIME types are retained for DLNA and TV-browser requests; known extensions take precedence over generic or incorrect provider MIME labels. Provider MIME parameters are stripped before HTTP and DLNA metadata are generated.
- Modern image thumbnails and image dimensions use Android ImageDecoder on API 28+ with bounded decode sizes, falling back to BitmapFactory if the platform's modern decoder fails; older Android versions use BitmapFactory for formats available on that system.
- Image and video thumbnail previews, lazily loaded as gallery items become visible, with memory-conscious handling for older Android versions.
- Gallery search by filename and photo/video filters.
- Media library sorting by newest available modification date, filename and file size, with unknown metadata kept at the end.
- Album filters based on Android MediaStore bucket names when the content provider exposes them; importing new media resets a stale album filter, and removing the last item from a selected album clears that filter. Document-picker items without album metadata remain available under all albums.
- Progressive technical metadata inspection in small batches, with cancellation when a new selection replaces the previous one.
- Technical metadata inspection for image size and video resolution, frame rate, video/audio MIME codecs, bitrate, duration and available HDR transfer metadata.
- Unit tests for HTTP range handling, resilient exact/unknown-length media stream copying (including zero and short reads), synthetic sustained transfers of 64 MiB and four parallel 16 MiB streams, strict shared-queue HTTP framing and body limits, bounded network response reads, invite-code rate limiting and expiry, UPnP time values, DLNA MIME profiles and sender/receiver brand classification.
- GitHub Actions CI builds and verifies debug, R8-optimized and unminified release variants internally.
- CI checks that DLNA commands, local media routes and shared-queue API endpoints survive release packaging.
- CI publishes exactly one installable artifact: `PureCast-Android`, containing a single standalone `PureCast.apk`.
- The published APK uses the unminified release configuration as a conservative full-size distribution build. Comparison variants are internal CI checks, not separate user downloads.

## Shared queue requirements and limitations

- All phones and the TV must use the same trusted, non-guest Wi-Fi network without client/AP isolation.
- The host app must remain running. A sender phone must remain available while its queued item is being played; force-stop, process termination or a network change can break its media URL.
- Shared media URLs expire after six hours. Re-add an item if its link expires.
- The shared session is in memory only. Closing the host app or stopping its process ends the group; reconnecting after a process restart is not supported yet.
- The LAN control API uses HTTP without TLS. Pair only with people you trust on a private network; do not use the feature on public or untrusted Wi-Fi.
- Shared playback is currently controlled by the host through DLNA. TV-browser playback is not remotely controlled by the shared queue.
- The host can remove a participant at any time; the shared-queue API token is revoked and their queued items are removed. If their item is playing, PureCast attempts to stop it on the TV. Previously issued direct media URLs cannot yet be revoked remotely and may remain valid until their six-hour expiry.

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

CI creates several internal test builds, but copies only the chosen full-size release build into `app/build/outputs/apk/PureCast.apk` and publishes that one standalone APK. It is signed with a temporary debug certificate for sideload testing; this is not a Play Store signing key.

## Download

The latest successful build is available from [GitHub Actions artifacts](https://github.com/Admiral456/PureCast/actions). Download only `PureCast-Android`. It contains the standalone `PureCast.apk`; no companion APK, data package or separate receiver installation is required.

## Why the optimized APK is small

PureCast does not bundle a video transcoder, codec framework, sample photos, sample videos, downloaded media or large image assets. It serves original files from Android content URIs and asks the TV to decode them. The release APK is therefore mostly app code and a small resource table. R8 removes unused parts of the AndroidX/Compose dependency graph and remains an internal comparison build. The one user-facing APK uses the complete unminified release configuration. CI verifies critical feature markers in both release packages. A small APK is not proof of missing functionality, and adding dummy files to increase its size would not make the app more complete.

## High-bitrate video and 4K/120 fps

PureCast transports the original encoded file; it does not decode, re-encode or generate extra frames. A "4K/120 fps" label alone does not define the required network speed: the actual file bitrate, codec/profile, bit depth, HDR format and audio track all matter. The TV must also decode that exact format and frame rate.

For demanding files, use a strong local Wi-Fi connection or wired LAN where possible, keep the phone and TV on the same non-guest network, and avoid client isolation. A Wi-Fi link-rate indicator is not a measurement of sustained application throughput. The phone must be able to read the original file and sustain a send rate above its encoded bitrate; weak signal, interference, busy storage or TV decoder limits can still cause buffering or playback failure.

CI now exercises large synthetic copies and four concurrent streams, but that does not certify a real 4K/120 fps end-to-end setup. Actual validation needs a known high-bitrate sample, the target phone, router and exact TV model/firmware. PureCast deliberately reports compatibility limits rather than lowering quality through transcoding.

## Quality and compatibility notes

- Direct Play preserves the bytes of the original file served by PureCast. It does not guarantee that the TV displays every pixel, frame rate, color format or HDR mode exactly as the source; that depends on the TV decoder and display pipeline.
- DLNA ConnectionManager MIME profiles are only an advertised container/content-type hint. They do not prove support for a particular video/audio codec, frame rate, bitrate, HDR profile or resolution.
- The browser playback page uses the TV browser's own media engine. A browser MIME hint is not proof that a particular codec/bitrate/HDR combination will play.
- UPnP device descriptions, SOAP replies and shared-queue JSON responses are limited to 1 MiB before parsing. Oversized or malformed control-plane responses fail visibly. JPEG XL, SVG, camera RAW, OpenEXR, DDS/KTX, FITS and other less common formats may be passed through without a local thumbnail; display still depends on the TV decoder/browser.
- The HTTP server currently supports single byte ranges; multipart ranges are not implemented.
- Foreground streaming continues while Android keeps the foreground service alive. Battery-saver policies and force-stop can still interrupt it.
- This build targets Android API 36. Before raising target SDK to Android 17/API 37, implement and request the runtime `ACCESS_LOCAL_NETWORK` permission.

## Hardware test status

CI verifies compilation, unit tests and APK packaging. It does **not** prove that a physical Samsung or LG television accepts playback. Samsung publishes model-group-specific decoder specifications, so exact model and firmware testing is required. Android's image decoder support does not guarantee support on the TV.

See [docs/APK-SIZE-AUDIT.md](docs/APK-SIZE-AUDIT.md) for the measured package-size analysis, [docs/DEVICE-COMPATIBILITY.md](docs/DEVICE-COMPATIBILITY.md), [docs/MULTI-PHONE.md](docs/MULTI-PHONE.md) and [docs/ROADMAP.md](docs/ROADMAP.md).
