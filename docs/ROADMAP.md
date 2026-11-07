# PureCast roadmap

## Milestone 0 — project foundation (current)

- [x] Android/Kotlin/Compose project structure.
- [x] Media Picker integration.
- [x] SSDP discovery for UPnP MediaRenderer devices.
- [x] UPnP AVTransport SetAVTransportURI + Play, including DIDL-Lite metadata.
- [x] Local HTTP media endpoint.
- [x] Single-range HTTP responses for seeking.
- [x] Unit tests for range parsing.
- [x] GitHub Actions debug build workflow.
- [x] GitHub Actions CI: Kotlin compilation, unit tests, and debug APK build passed (run #6, 7 November 2026).
- [ ] Test against the LG 55UJ7507 and at least one other DLNA renderer.

## Milestone 1 — playback reliability

- Add a foreground playback service so streaming continues reliably while PureCast is backgrounded.
- Add explicit stop, pause, resume, and seek controls through AVTransport.
- Retain and revoke URI grants safely when the user reopens the app.
- Improve timeout, socket, and TV SOAP fault diagnostics.
- Handle TV/phone Wi-Fi changes and recover the local server.

## Milestone 2 — media browser and compatibility

- Build a gallery with albums, thumbnails, sorting, and multi-select.
- Read media metadata: resolution, frame rate, video/audio codecs, bitrate, HDR, duration, and file size.
- Show compatibility as Direct Play, incompatible, or unknown. Never label an unverified file as compatible.
- Add support profiles and tests for LG webOS, Samsung Tizen, Android TV/Google TV, and other DLNA renderers.

## Milestone 3 — robustness and security

- Add an expiring capability token per stream and verify request methods and paths strictly.
- Add IPv4/IPv6 and network-interface selection for phones with VPNs or multiple network routes.
- Add single- and multi-range test coverage, large-file stress tests, and interrupted-transfer recovery.
- Review cleartext LAN transport and ensure the media endpoint is reachable only on the active private network.
- Before targeting Android API 37, declare and request ACCESS_LOCAL_NETWORK.

## Release acceptance checks

1. Discovery finds a DLNA MediaRenderer on the same Wi-Fi.
2. Device description is parsed with external XML entities disabled.
3. A selected file is served byte-for-byte; no transcoder is present in the pipeline.
4. HEAD and GET requests return correct MIME type and length where known.
5. Range requests return 206 with a valid Content-Range.
6. TV reports unsupported formats visibly; PureCast does not silently transcode.
7. The app remains stable for large files and long playback sessions.
