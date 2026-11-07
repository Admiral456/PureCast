# PureCast roadmap

## Milestone 0 — Android cross-brand foundation

- [x] Android/Kotlin/Jetpack Compose application; no iOS target.
- [x] Brand-neutral Android sender for Samsung, Xiaomi and Redmi families.
- [x] Sender phone brand/model identification for diagnostics.
- [x] TV brand/platform classification from UPnP descriptions.
- [x] Czech light/dark UI.
- [x] Photo Picker selection and URI permission retention where supported.
- [x] SSDP discovery for UPnP MediaRenderer devices.
- [x] Prefer active Wi-Fi route for SSDP requests and TV-description retrieval.
- [x] Prefer an active Wi-Fi IPv4 address when creating TV playback links.
- [x] Secure device-description XML parsing and optional URLBase resolution.
- [x] AVTransport SetAVTransportURI, Play, Pause, Stop, Seek and state/position queries.
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
- [x] GitHub Actions tests, debug APK build and R8-optimized release APK build.

## Milestone 1 — Samsung and LG real-device interoperability

- [ ] Test discovery and playback on Samsung Tizen Smart TVs from more than one model year.
- [ ] Test discovery and playback on LG 55UJ7507.
- [ ] Capture real SSDP replies and UPnP service descriptions from Samsung and LG firmware.
- [ ] Verify Samsung and LG browser fallback with JPEG and H.264/AAC MP4.
- [ ] Verify supported HEVC/HDR and high-FPS sample files only against each model's published decoder spec.
- [ ] Verify pause, seek, stop and background streaming on physical hardware.
- [ ] Add model-specific workarounds only when supported by captured device responses or repeatable tests.

## Milestone 2 — multi-phone and library features

- [x] Allow multiple Android phone brands to run the same APK.
- [x] Gallery with image/video thumbnails, filename search and media-type filters.
- [x] Local playback queue with add/remove, reorder, start-from-beginning, previous and next controls.
- [x] Queue navigation stops at the first/last item and is covered by unit tests.
- [ ] Albums, date-based sorting and additional library organization.
- [x] Shared local group queue synchronized between Android phones on the same Wi-Fi, with host-only playback/reorder/remove permissions.
- [x] Shared queue previous/next navigation and stop-at-boundaries behavior.
- [x] Show sender identity for items submitted to a shared queue.
- [x] Pair participants using a short invite code and issue per-participant high-entropy access tokens.
- [x] Validate shared media URLs as private IPv4 PureCast media-token endpoints.
- [ ] Improve per-device compatibility profiles from actual UPnP protocolInfo and test results.
- [ ] Improve friendly error explanations for Wi-Fi isolation, unsupported format and expired file URI access.

## Milestone 3 — robust streaming and security

- [x] Prefer Wi-Fi rather than mobile/VPN routes for TV discovery and local playback URLs when available.
- [ ] Add large-file, slow-network and interrupted-transfer stress tests.
- [ ] Add explicit media server access revocation and token refresh.
- [x] Revoke a participant token and remove their submitted items when they explicitly leave the group.
- [ ] Add QR-code pairing, persistent sessions and host-initiated participant removal.
- [ ] Add integration tests for the shared-queue HTTP server/client and multi-phone media streaming.
- [ ] Validate all request paths, content ranges and response headers with integration tests.
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
