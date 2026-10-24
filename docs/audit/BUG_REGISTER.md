# AetherVault Omni Edition — Master Bug Register

## Classification Legend
- **Severity:** Critical | High | Medium | Low
- **Priority:** P0 (Blocker) | P1 (Next Repair) | P2 (Scheduled) | P3 (Backlog)
- **Status:** VERIFIED_FIXED | CONFIRMED_STATIC | POTENTIAL | REJECTED

---

### BUG-001: Media Player Non-Functional Static Mockup
- **Subsystem:** `features/media/MediaPlayerScreen.kt`
- **Severity:** High | **Priority:** P0
- **Evidence:** `MediaPlayerScreen.kt` lines 60–125 rendered a static movie/audio icon with a hardcoded 120,000 ms dummy duration slider. No playback service or decoder existed.
- **Reproduction Steps:**
  1. Open Drive or Photos.
  2. Tap any `.mp4` or `.mp3` file.
  3. UI showed static icon and un-scrubbable slider with silence.
- **Expected Behavior:** Hardware/software accelerated playback of video and audio streams via Android Media3 ExoPlayer.
- **Actual Behavior:** Completely silent, non-functional UI mockup.
- **Root Cause:** Incomplete prototype code left behind from initial UI scaffolding.
- **Fix Implemented:** Replaced with `androidx.media3:media3-exoplayer:1.4.1` and `PlayerView`, custom M3 player overlay, seek slider, and duration polling.
- **Verification Status:** **VERIFIED_FIXED**

---

### BUG-002: Lack of RFC 7233 Byte-Range Streaming for Telegram Media
- **Subsystem:** `core/media/TelegramStreamProxy.kt`
- **Severity:** High | **Priority:** P0
- **Evidence:** Attempting to play remote media required downloading the complete 2 GB payload into flash storage before playback could start.
- **Reproduction Steps:**
  1. Store large video file in Telegram channel.
  2. Attempt to stream via standard HTTP GET.
  3. Video player stalled indefinitely awaiting full download completion.
- **Expected Behavior:** Instant playback startup within 500ms using HTTP `Range: bytes=X-Y` requests.
- **Actual Behavior:** Video player timed out or exhausted local device disk space.
- **Root Cause:** Standard Telegram Bot API `getFile` downloads entire files sequentially; no local stream bridge translated ExoPlayer range requests.
- **Fix Implemented:** Created `TelegramStreamProxy` on `127.0.0.1` serving `HTTP 206 Partial Content` with `Content-Range` headers mapped to Telegram chunks and local random-access caches.
- **Verification Status:** **VERIFIED_FIXED**

---

### BUG-003: RemoteFetchWorker Mock Execution (Fake 15 MB)
- **Subsystem:** `features/remotefetch/RemoteFetchWorker.kt`
- **Severity:** High | **Priority:** P0
- **Evidence:** `processJob()` in `RemoteFetchWorker.kt` immediately set job status to `COMPLETED` with hardcoded `15 * 1024 * 1024L` bytes without making network requests.
- **Reproduction Steps:**
  1. Open Remote Fetch screen.
  2. Enter valid direct URL (e.g. `https://example.com/file.zip`).
  3. Job instantly marked complete, but zero bytes uploaded to Telegram channel `-1004348906769`.
- **Expected Behavior:** Real OkHttp streaming download piped into Telegram chunked multipart uploads.
- **Actual Behavior:** Fake instant completion.
- **Root Cause:** Stub code not yet connected to OkHttp streaming pipe.
- **Fix Implemented:** Real OkHttp chunked streaming engine with 60s+ timeouts, 19MB parts, and Telegram document ingestion.
- **Verification Status:** **VERIFIED_FIXED**

---

### BUG-004: Telegram Channel ID Supergroup Prefix Inconsistency
- **Subsystem:** `core/telegram/TelegramMultipartUploader.kt`
- **Severity:** High | **Priority:** P0
- **Evidence:** Providing negative channel IDs (e.g. `-1004348906769`) caused `formatChatId()` to strip or misalign signs, triggering `HTTP 400 Bad Request: chat not found`.
- **Reproduction Steps:**
  1. Enter standard supergroup ID `-1004348906769`.
  2. Bot attempted to post to invalid target ID.
- **Expected Behavior:** Channel ID formatted consistently as `-1004348906769`.
- **Actual Behavior:** Telegram API returned 400 error.
- **Root Cause:** String logic in `formatChatId` failed when negative numbers were supplied as Longs or Strings.
- **Fix Implemented:** Refactored `formatChatId` to rigorously normalize `-100...` supergroups.
- **Verification Status:** **VERIFIED_FIXED**

---

### BUG-005: Floating FAB Overlap on TeleDrop and Player Bottom Bars
- **Subsystem:** `MainActivity.kt` & `TeleDropScreen.kt`
- **Severity:** Medium | **Priority:** P1
- **Evidence:** `M3StackedFabs` rendered unconditionally when `subScreen == SubScreen.NONE`, floating over TeleDrop's send button and input dock.
- **Reproduction Steps:**
  1. Open app and tap "TeleDrop" tab.
  2. Bottom right squircle FAB was visually layered over the send button.
- **Expected Behavior:** Floating action buttons only appear on the files explorer tab; TeleDrop uses an integrated, spaced bottom bar.
- **Actual Behavior:** Touch targets collided, preventing clicks on the Send button.
- **Root Cause:** Missing tab scoping condition on `M3StackedFabs`.
- **Fix Implemented:** Scoped FAB to `activeTab == MainNavTab.DRIVE`, and refactored `TeleDropScreen.kt` bottom bar into `Arrangement.spacedBy(8.dp)` with dedicated slots.
- **Verification Status:** **VERIFIED_FIXED**

---

### BUG-006: WASM & Web Blob Downloads Lost in Browser Sandbox
- **Subsystem:** `features/workstation/WasmWebWorkstationView.kt`
- **Severity:** Medium | **Priority:** P1
- **Evidence:** Browser-based WASM applications triggering `URL.createObjectURL(blob)` or `<a download>` had no native reception handler, dropping artifacts silently.
- **Reproduction Steps:**
  1. Open WASM tool in workstation.
  2. Generate or download binary artifact.
  3. File was lost in volatile WebView memory.
- **Expected Behavior:** Native capture to disk cache and immediate forwarding to Telegram upload queue.
- **Actual Behavior:** Download failed silently with no user feedback.
- **Root Cause:** Missing `DownloadListener` and JavaScript-to-Native bridge.
- **Fix Implemented:** Implemented `WasmWebWorkstationView` with `WasmBlobNativeBridge` under `window.AetherVaultNative`.
- **Verification Status:** **VERIFIED_FIXED**

---

### BUG-007: GitHub Actions CI Build Execution Failure
- **Subsystem:** `.github/workflows/build-and-release.yml`
- **Severity:** Medium | **Priority:** P1
- **Evidence:** CI step executed `chmod +x gradlew` and `gradle assembleDebug` on environments without pre-installed Gradle or wrapper scripts.
- **Reproduction Steps:**
  1. Trigger GitHub Actions workflow on fresh runner.
  2. Step failed with command not found or invalid Gradle version.
- **Expected Behavior:** Deterministic JDK 17 + Gradle setup with caching.
- **Actual Behavior:** Build failed during early initialization.
- **Root Cause:** Incomplete Gradle distribution provisioning in GitHub Actions workflow.
- **Fix Implemented:** Configured `gradle/actions/setup-gradle@v4` with pinned Gradle compatibility.
- **Verification Status:** **VERIFIED_FIXED**
