# PureCast roadmap

## 0.31.0 — Release thumbnail cache under memory pressure (in progress)
- Clear cached preview bitmaps when Android reports low or critical memory conditions.
- Clear the same cache on the activity low-memory callback.
- Keep normal navigation cache reuse, with a fixed 8 MiB upper bound between memory-pressure events.
- Add unit coverage for the memory-trim threshold.


## 0.30.0 — Bounded thumbnail cache (in progress)
- Add an 8 MiB LRU cache for display-only gallery thumbnails to avoid decoding the same images again when navigating between tabs.
- Keep the cache strictly bounded by decoded bitmap allocation size; source media bytes and TV streaming remain uncached and unchanged.
- Preserve the conservative video-preview path: use provider thumbnails only, otherwise show a placeholder rather than invoking a video codec.
- Test and document the cache memory budget.


## 0.28.0 — Unified gallery and safer scrolling (in progress)
- Replace the nested gallery LazyColumn with one continuous screen-level list.
- Present picked photos and videos in a responsive-feeling two-column tile layout.
- Separate permission state, import actions, library filters and media tiles into clear sections.
- Put secondary actions in a per-item overflow menu while keeping TV playback as the primary action.
- Let Compose own the lifetime of preview bitmaps rather than manually recycling a bitmap as soon as a lazy item leaves composition.


## 0.27.0 — Conservative gallery previews (in progress)
- Do not invoke MediaMetadataRetriever to generate video frames while browsing the gallery; use Android's provider thumbnail or a neutral video placeholder.
- Serialize thumbnail generation through a single limited-parallelism dispatcher to avoid opening several native decoders at once.
- Bound thumbnail dimensions and handle decoder memory exhaustion without terminating media selection.
- Keep all selected original bytes untouched; preview generation is display-only.


## 0.26.0 — Clear navigation and screen hierarchy (in progress)
- Replace the single long mixed screen with four bottom-navigation destinations: Connect, Gallery, Queue, and Player.
- Keep TV discovery and network status together; isolate media selection from playback controls.
- Add clear empty states and move to the player automatically after starting playback.
- Keep the existing DLNA transport and original-file streaming behavior unchanged.


## 0.25.0 — Safer media import (in progress)
- Do not run technical inspection across every picked photo and video immediately after selection.
- Bound and deduplicate picker results to the 50-item library limit.
- Tolerate Photo Picker and document-provider differences in optional metadata queries.
- Serialize thumbnail decoding and use smaller fallback bitmaps to reduce decoder memory pressure.
- Add deterministic unit tests for import limits and available library capacity.


## Milestone 0 — Android cross-brand foundation

- [x] Android/Kotlin/Jetpack Compose application; no iOS target.
- [x] Brand-neutral Android sender for Samsung, Xiaomi and Redmi families.
- [x] Sender phone brand/model identification for diagnostics.
- [x] TV brand/platform classification from UPnP descriptions.
- [x] Czech light/dark UI.
- [x] Photo Picker selection and URI permission retention where supported.
- [x] Version-aware gallery runtime permissions for Android 12 and older, Android 13, and Android 14+.
- [x] Android 14+ selected-photo and video access, including full, limited, images-only and videos-only states.
- [x] In-app gallery permission rationale, status, gallery loading and shortcut to app permission settings.
- [x] Keep Photo Picker and document picker available when broad gallery access is denied.
- [x] SSDP discovery for UPnP MediaRenderer devices.
- [x] Prefer active Wi-Fi route for SSDP requests and TV-description retrieval.
- [x] Prefer an active Wi-Fi IPv4 address when creating TV playback links.
- [x] Resolve a reachable local IPv4 address from Android SoftAP/hotspot and supported Ethernet/tethering interfaces when no regular Wi-Fi Network is exposed; prefer the hotspot address when both SoftAP and upstream Wi-Fi are active.
- [x] Select Wi-Fi/Ethernet Android Network routes by the destination TV/peer address for DLNA SOAP, SSDP descriptions and shared-queue clients.
- [x] Secure device-description XML parsing and optional URLBase resolution.
- [x] AVTransport SetAVTransportURI, Play, Pause, Stop, Seek and state/position queries.
- [x] Automatically refresh DLNA playback state and timeline, with state-aware pause/resume/stop controls.
- [x] ConnectionManager GetProtocolInfo and conservative MIME-profile compatibility evaluation.
- [x] Original-source local HTTP streaming with byte-range support.
- [x] TV browser fallback page with responsive controls and browser MIME support hint.
- [x] Short, random, URL-safe media tokens for TV playback URLs.
- [x] Foreground playback service and persistent playback notification.
- [x] Image/video technical metadata inspection.
- [x] Progressive metadata inspection and cancellation of stale gallery inspection work.
- [x] Thumbnail previews for images and videos.
- [x] Lazy gallery item rendering to limit simultaneous thumbnail decoding.
- [x] Filename search and photo/video type filters.
- [x] Memory-conscious thumbnail decoding for older Android versions.
- [x] Unit tests for byte ranges, UPnP time values, MIME compatibility, media filters, media tokens and brand classification.
- [x] GitHub Actions tests, debug and release APK builds, with APK package verification.
- [x] APK smoke checks for DLNA commands, streaming paths, shared-queue API routes and playback UI markers.
- [x] Publish exactly one standalone PureCast APK; keep comparison builds internal to CI.
- [x] Add adaptive, round and monochrome PureCast launcher icons, verified inside the APK.
- [x] Add an extension-to-MIME catalog for modern web formats, HEIF/HEIC, AVIF, JPEG XL, TIFF, SVG, JPEG 2000 and camera RAW.
- [x] Add Android document picker for less common image formats alongside Photo Picker.
- [x] Use bounded Android ImageDecoder thumbnails and dimensions for platform-supported formats, with BitmapFactory fallback.
- [x] Normalize MIME types and DLNA protocolInfo parameters before compatibility checks.
- [x] Add actual image load/error feedback and decoded dimensions to the TV-browser player.
- [x] Skip non-image and non-video documents from the document picker.
- [x] Route DLNA SOAP commands and shared-queue client requests over the active Wi-Fi network when available.
- [x] Fail clearly if the SSDP multicast socket cannot bind to the selected Wi-Fi network.
- [x] Bind SSDP discovery to a local hotspot/Ethernet interface when Android exposes no Wi-Fi Network object, and explain that cellular data alone is insufficient.
- [x] Limit UPnP device descriptions, SOAP replies and shared-queue JSON replies to 1 MiB before parsing.
- [x] Verify TV-browser image element wiring in the packaged APK.
- [x] Validate MIME values before using them in HTTP response headers and TV-browser attributes.
- [x] Enforce strict unsigned-decimal syntax for HTTP byte ranges.
- [x] Handle zero and short reads from provider-backed media streams; test exact-length copies, unknown-length copies, offset skipping and premature source termination.
- [x] Reuse bounded 256 KiB per-worker transfer buffers, use a 64 KiB response buffer and request a larger TCP send window for high-bitrate streaming.
- [x] Stress-test synthetic 64 MiB transfers and four concurrent 16 MiB streams without allocating whole media files in memory.
- [x] Show live rolling application-output throughput, bytes sent, active/peak transfers and completed/interrupted media streams in the playback panel; include source-video bitrate when available.

## Milestone 1 — Samsung and LG real-device interoperability

- [ ] Test discovery and playback on Samsung Tizen Smart TVs from more than one model year.
- [ ] Test TV discovery and Direct Play while the sender phone provides its own mobile-data-backed hotspot on Samsung, Xiaomi and Redmi devices.
- [ ] Test simultaneous upstream Wi-Fi plus active SoftAP, including correct route selection for the TV and shared-queue peers.
- [ ] Test discovery and playback on LG 55UJ7507.
- [ ] Capture real SSDP replies and UPnP service descriptions from Samsung and LG firmware.
- [ ] Verify Samsung and LG browser fallback with JPEG and H.264/AAC MP4.
- [ ] Verify supported HEVC/HDR and high-FPS sample files only against each model's published decoder spec.
- [ ] Verify pause, seek, stop and background streaming on physical hardware.
- [ ] Add model-specific workarounds only when supported by captured device responses or repeatable tests.

## Milestone 2 — multi-phone and library features

- [x] Allow multiple Android phone brands to run the same APK.
- [x] Allow shared-queue hosting on a reachable phone-hotspot/local IPv4 interface.
- [x] Gallery with image/video thumbnails, filename search and media-type filters.
- [x] Local playback queue with add/remove, reorder, start-from-beginning, previous and next controls.
- [x] Queue navigation stops at the first/last item and is covered by unit tests.
- [x] Album filtering from Android MediaStore bucket metadata when available, with stale selections cleared after imports and last-item removal.
- [x] Sort media by newest available modification date, name, largest size and smallest size.
- [x] Shared local group queue synchronized between Android phones on the same local Wi-Fi or a reachable host-phone hotspot, with host-only playback/reorder/remove permissions.
- [x] Shared queue previous/next navigation and stop-at-boundaries behavior.
- [x] Show sender identity for items submitted to a shared queue.
- [x] Pair participants using a short invite code and issue per-participant high-entropy access tokens.
- [x] Strictly parse shared-queue HTTP request lines and headers; reject duplicate headers, chunked/encoded bodies, invalid Content-Length values and unexpected GET bodies, with bounded zero-read-safe body handling and unit tests.
- [x] Rate-limit incorrect invite codes with bounded per-IP tracking, expiry and unit tests; cap shared sessions at 16 participants.
- [x] Validate shared media URLs as private IPv4 PureCast media-token endpoints.
- [ ] Improve per-device compatibility profiles from actual UPnP protocolInfo and test results.
- [ ] Improve friendly error explanations for Wi-Fi isolation, unsupported format and expired file URI access.

## Milestone 3 — robust streaming and security

- [x] Prefer Wi-Fi rather than mobile/VPN routes for TV discovery and local playback URLs when available.
- [ ] Add large-file, slow-network and interrupted-transfer stress tests.
- [ ] Add explicit media server access revocation and token refresh.
- [x] Revoke a participant token and remove their submitted items when they explicitly leave the group.
- [ ] Add QR-code pairing and persistent sessions.
- [x] Allow the host to remove participants, revoke their tokens and remove their submitted queue items.
- [ ] Add integration tests for the shared-queue HTTP server/client and multi-phone media streaming.
- [x] Keep shared playback controls visible when the currently playing item was added by another phone.
- [x] Keep the host's foreground service active while its shared queue session is connected.
- [x] Strictly validate local media HTTP request lines and headers; reject duplicate/ambiguous Range headers, chunked/encoded requests and request bodies, with bounded parsing and unit tests.
- [ ] Validate all response paths, content ranges and response headers with socket-level integration tests.
- [x] Bind the shared queue coordinator to the active Wi-Fi IPv4 interface rather than all interfaces.
- [ ] Before targeting Android API 37, declare and request ACCESS_LOCAL_NETWORK.

## Release acceptance checks

1. Samsung Galaxy, Xiaomi and Redmi phones run the same Android APK.
2. A Samsung or LG TV is discovered when it exposes a controllable DLNA renderer, or the user receives a clear browser fallback route.
3. A selected media file is served byte-for-byte; no transcoder is present in the pipeline.
4. HEAD and GET requests return correct MIME type and length where known.
5. Range requests return 206 with a valid Content-Range.
6. Unsupported formats fail visibly rather than silently lowering quality.
7. Streaming continues while the app is backgrounded with its foreground service active.
8. Device-specific support claims are backed by tests on the actual TV model and firmware.
