# PureCast roadmap

## Milestone 0 — Android cross-brand foundation

- [x] Android/Kotlin/Jetpack Compose application; no iOS target.
- [x] Brand-neutral Android sender for Samsung, Xiaomi and Redmi families.
- [x] Sender phone brand/model identification for diagnostics.
- [x] TV brand/platform classification from UPnP descriptions.
- [x] Czech light/dark UI.
- [x] Photo Picker selection and URI permission retention where supported.
- [x] SSDP discovery for UPnP MediaRenderer devices.
- [x] Secure device-description XML parsing and optional URLBase resolution.
- [x] AVTransport SetAVTransportURI, Play, Pause, Stop, Seek and state/position queries.
- [x] ConnectionManager GetProtocolInfo and conservative MIME-profile compatibility evaluation.
- [x] Original-source local HTTP streaming with byte-range support.
- [x] TV browser fallback page with responsive controls and browser MIME support hint.
- [x] Foreground playback service and persistent playback notification.
- [x] Image/video technical metadata inspection.
- [x] Unit tests for byte ranges, UPnP time values, MIME compatibility and brand classification.
- [x] GitHub Actions tests and debug APK build.

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
- [ ] Gallery with thumbnails, albums, sorting, search and multi-select.
- [ ] Shared local group queue for multiple phones, with host/queue permissions.
- [ ] Show sender identity for items submitted to a shared queue.
- [ ] Improve per-device compatibility profiles from actual UPnP protocolInfo and test results.
- [ ] Improve friendly error explanations for Wi-Fi isolation, unsupported format and expired file URI access.

## Milestone 3 — robust streaming and security

- [ ] Add large-file, slow-network and interrupted-transfer stress tests.
- [ ] Add active network-interface selection for Wi-Fi/VPN/multiple network routes.
- [ ] Add explicit media server access revocation and token refresh.
- [ ] Validate all request paths, content ranges and response headers with integration tests.
- [ ] Review LAN exposure and optionally bind to the active Wi-Fi interface rather than all interfaces.
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
