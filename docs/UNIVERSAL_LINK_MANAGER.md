# Universal Link Manager Architecture — AetherVault Omni Edition

## Overview

The **Universal Link Manager** extends AetherVault Omni Edition with a unified multi-platform media, file download, streaming, and Telegram cloud transfer engine. It accepts supported URLs from multiple platforms, validates and classifies the target resource, retrieves metadata, detects real technical capabilities, and guides the user through compliant and resilient workflows:

1. **Stream / Preview**: Instant streaming via **AndroidX Media3 (ExoPlayer)** with byte-range requests for direct media URLs, and verified native app intents / embeds for protected platforms.
2. **Local Storage Download**: Resilient streaming to user-selected device storage via Android's **Storage Access Framework (`CreateDocument`)** or app downloads directory, with chunk buffering and cancellation.
3. **Telegram Cloud Upload**: Multi-chunk (19 MB chunks to adhere to Telegram Bot API constraints) upload pipeline to private Telegram supergroup channels (`-100...`), with dual-phase progress telemetry and explicit channel confirmation.
4. **Platform Capability Boundaries**: Explicit enforcement and disclosure of API, DRM, and Terms of Service policies for Google Drive, YouTube, Instagram, Facebook, and Google Play Store without fake progress or fragile scrapers.

---

## Architectural Components

### 1. Model Layer (`UniversalLinkModels.kt`)
- `PlatformType`: Direct HTTP/S, Google Drive, YouTube, Instagram, Facebook, Google Play Store, Unknown.
- `ContentType`: Video, Audio, Image, Document, Archive, Application Package, Restricted Stream, Webpage, Generic File.
- `ContentCapability`: Direct Download & Transfer, Direct Stream Only, Official App or Embed Only, Authorized Export Required, Store App Details Only, Unsupported Webpage.
- `ResourceMetadata`: Carries canonical URL, platform type, content type, capability, MIME type, size, suggested filename, authorization requirement flags, and official policy notices.
- `UniversalTransferTask` & `TransferStage`: Tracks lifecycle across `RESOLVING_METADATA`, `FETCHING_SOURCE`, `UPLOADING_TO_TELEGRAM`, `SAVING_TO_LOCAL`, `COMPLETED`, `FAILED`, and `CANCELLED`.

### 2. Platform Adapter Layer (`PlatformAdapters.kt`)
- **`DirectFileAdapter`**: Performs lightweight HTTP `HEAD` / Range queries to detect MIME type, byte size, and filename from `Content-Disposition`, and detects HLS (`.m3u8`) / DASH (`.mpd`) live streams.
- **`GoogleDriveAdapter`**: Identifies Google Drive file IDs, folder IDs, and Docs/Sheets/Slides export URLs. Directs users to Google Drive OAuth export or direct download if publicly shared.
- **`YouTubeAdapter`**: Queries the official YouTube oEmbed API for video title and author. Complies strictly with YouTube Developer ToS and Google Play policies by routing playback to the official YouTube player / app intent rather than attempting unauthorized stream extraction.
- **`InstagramAdapter`**: Identifies Instagram posts, reels, and stories. Discloses Meta Graph API requirements and routes to the official Instagram app via deep-link intents.
- **`FacebookAdapter`**: Identifies Facebook videos, reels, and shares. Discloses Meta Graph API requirements and routes to the official Facebook app or browser.
- **`GooglePlayAdapter`**: Identifies Google Play Store packages, generates `market://details?id=...` and browser links, and explains Play Store dynamic APK delivery constraints.

### 3. Registry & Security Layer (`UniversalLinkRegistry.kt`)
- **SSRF Protection**: Blocks loopback (`127.0.0.1`, `localhost`), link-local (`169.254.x.x`), and RFC 1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) to protect internal networks.
- **Scheme Validation**: Whitelists `http` and `https` protocols, discarding dangerous schemes like `file://`, `content://`, `javascript:`, or `ftp://`.
- **Adapter Resolution**: Matches incoming URLs against registered platform adapters in order of specificity.

### 4. Coordinator & Transfer Pipeline (`UniversalLinkCoordinator.kt`)
- **Direct Stream to SAF**: Pipes remote HTTP streams into Android `OutputStream` via 64 KB buffers, tracking transferred bytes and speed (B/s), with coroutine cancellation.
- **Direct Stream to Telegram**: Streams chunks (up to 19 MB each) into temporary local cache files and uploads them sequentially to Telegram's `sendDocument` API with `-100...` supergroup formatting. Temporary cache files are immediately wiped post-transfer.
- **Diagnostics Integration**: Logs all link resolution and transfer operations into `DiagnosticManager` for full visibility in the In-App Debug Center.

### 5. UI Layer (`UniversalLinkManagerScreen.kt`)
- **Real-time URL Validation**: Input field with live protocol and SSRF badge indicators.
- **Resource Card**: Displays platform badge, detected MIME type, formatted size, and primary capability.
- **Action Buttons**:
  - `Stream in Media3` (Direct video/audio)
  - `Download to Storage` (SAF document picker)
  - `Transfer to Telegram` (with confirmation dialog showing destination channel and chunking strategy)
  - `Open Official App` (Deep-link intent)
  - `Technical Details` (Dialog outlining exact technical boundaries and policies)
- **Transfer Dashboard**: Live cards showing transfer phase, progress percentage, transferred MB, transfer speed, and cancel button.

---

## Integration with Android System & Existing Features

1. **Android Share Intent (`ACTION_SEND`)**:
   `AndroidManifest.xml` declares an `<intent-filter>` for `android.intent.action.SEND` with `text/plain`. URLs shared from any app (Chrome, YouTube, Telegram, Drive) open directly in `UniversalLinkManagerScreen`.
2. **Media Player Integration**:
   Direct streams from `UniversalLinkManagerScreen` feed directly into `MediaPlayerScreen` powered by AndroidX Media3 ExoPlayer with byte-range support.
3. **Navigation Drawer**:
   Accessible anytime via the drawer as **Universal Link Manager**.
