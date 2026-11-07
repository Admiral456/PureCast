# PureCast roadmap

## Milestone 0 — working Android foundation

- [x] Kotlin/Jetpack Compose project and Czech light/dark UI.
- [x] Photo Picker selection and URI permission retention where supported.
- [x] SSDP discovery for UPnP MediaRenderer devices.
- [x] Secure device-description XML parsing and optional URLBase resolution.
- [x] AVTransport SetAVTransportURI, Play, Pause, Stop, Seek and state/position queries.
- [x] ConnectionManager GetProtocolInfo and conservative MIME-profile compatibility evaluation.
- [x] Original-source local HTTP streaming with byte-range support.
- [x] TV browser fallback page served directly by the phone.
- [x] Foreground playback service and persistent playback notification.
- [x] Image/video technical metadata inspection.
- [x] Unit tests for byte ranges, UPnP time values and MIME compatibility.
- [x] GitHub Actions tests and debug APK build.

## Milestone 1 — real-device interoperability

- [ ] Test discovery and playback on the LG 55UJ7507.
- [ ] Capture actual SSDP replies and UPnP service descriptions from LG firmware.
- [ ] Verify which photo formats the LG browser and/or DLNA renderer accepts.
- [ ] Test H.264/AAC MP4 and supported image formats first, then test selected high-resolution files.
- [ ] Verify pause, seek, stop and background streaming on physical hardware.
- [ ] Add device-specific workarounds only when supported by captured device responses.

## Milestone 2 — richer library and diagnostics

- [ ] Gallery with thumbnails, albums, sorting, search and multi-select.
- [ ] Display full technical details and keep unknown values distinct from unsupported values.
- [ ] Add per-TV capability profiles based on actual UPnP protocolInfo and tested formats.
- [ ] Improve friendly error explanations for Wi-Fi isolation, unsupported format and expired file URI access.
- [ ] Add optional manual IP/URL connection for TV browser fallback.

## Milestone 3 — robust streaming and security

- [ ] Add large-file, slow-network and interrupted-transfer stress tests.
- [ ] Add active network-interface selection for Wi-Fi/VPN/multiple network routes.
- [ ] Add explicit media server access revocation and token refresh.
- [ ] Validate all request paths, content ranges and response headers with integration tests.
- [ ] Review LAN exposure and optionally bind to the active Wi-Fi interface rather than all interfaces.
- [ ] Before targeting Android API 37, declare and request ACCESS_LOCAL_NETWORK.

## Release acceptance checks

1. A TV renderer is discovered on the same local network, or the user receives an actionable explanation.
2. A selected media file is served byte-for-byte; no transcoder is present in the pipeline.
3. HEAD and GET requests return correct MIME type and length where known.
4. Range requests return 206 with a valid Content-Range.
5. Unsupported formats fail visibly rather than silently lowering quality.
6. Streaming continues while the app is backgrounded with its foreground service active.
7. Device-specific support claims are backed by tests on the actual TV model.
