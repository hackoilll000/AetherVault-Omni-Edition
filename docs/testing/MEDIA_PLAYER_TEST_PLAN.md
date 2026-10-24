# AetherVault Omni Edition — Media Player Test Plan

## 1. Objectives & Scope

The purpose of this test plan is to establish deterministic, regression-proof validation for the Android Media3 / ExoPlayer subsystem in AetherVault Omni Edition.

The media engine is responsible for:
1. High-fidelity audio and video playback for locally cached files.
2. Direct cloud streaming of multi-gigabyte files hosted on Telegram channels using `TelegramStreamProxy`.
3. Seamless partial content seeking via RFC 7233 Byte-Range requests (`206 Partial Content`).
4. Robust lifecycle management and memory cleanup.

---

## 2. Supported Media Matrix

| Format / Container | MIME Type | Video Codecs | Audio Codecs | Local Playback | Stream Proxy Playback |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **MP4** | `video/mp4` | H.264 (AVC), H.265 (HEVC), AV1 | AAC, MP3, Opus | Verified | Verified (Range 206) |
| **MKV / WebM** | `video/x-matroska`, `video/webm` | VP8, VP9, AV1 | Vorbis, Opus | Verified | Verified (Range 206) |
| **MP3** | `audio/mpeg` | N/A | MP3 (CBR & VBR) | Verified | Verified (Range 206) |
| **M4A / AAC** | `audio/mp4`, `audio/aac` | N/A | AAC-LC, HE-AAC | Verified | Verified (Range 206) |
| **FLAC** | `audio/flac` | N/A | FLAC Lossless | Verified | Verified (Range 206) |
| **WAV** | `audio/wav`, `audio/x-wav` | N/A | PCM 16/24-bit | Verified | Verified (Range 206) |
| **OGG** | `audio/ogg` | N/A | Vorbis, Opus | Verified | Verified (Range 206) |

---

## 3. Playback State Machine Test Cases

### TC-MED-01: Cold Startup & Buffering Transition
- **Precondition:** Video file registered with `TelegramStreamProxy.registerStream()`.
- **Procedure:** Initialize `ExoPlayer` with stream URL and observe state flow.
- **Expected Flow:** `Player.STATE_IDLE` $\rightarrow$ `Player.STATE_BUFFERING` $\rightarrow$ `Player.STATE_READY`.
- **Criteria:** Spinner displays during buffering, disappears when ready; duration is resolved $> 0$ ms.

### TC-MED-02: Play / Pause Toggling
- **Procedure:** Tap primary play/pause button rapidly 5 times.
- **Expected Behavior:** Single authoritative state tracked via `Player.Listener.onIsPlayingChanged()`. No orphaned playback threads or audio-video desync.

### TC-MED-03: Scrubbing & Partial Range Seeking
- **Procedure:** Drag the seek slider from 00:10 to 45:00 on a 1 GB video.
- **Expected Behavior:** `exoPlayer.seekTo(positionMs)` fires; proxy server intercepts `Range: bytes=X-Y` request; responds with `HTTP 206 Partial Content`; playback resumes without restarting stream from byte 0.

### TC-MED-04: Network Degradation & Error Recovery
- **Procedure:** Terminate socket connection mid-stream.
- **Expected Behavior:** `onPlayerError(PlaybackException)` triggered; UI displays error recovery banner with "Retry Playback" action; clicking retry re-prepares ExoPlayer without crashing the activity.

### TC-MED-05: Lifecycle Navigation & Resource Release
- **Procedure:** Navigate back to file list while video is playing.
- **Expected Behavior:** `DisposableEffect` executes `exoPlayer.release()`; all internal decoder and audio track resources freed immediately; no audio continues in the background.

---

## 4. Automated Regression Verification

The test suite validates:
- `TelegramStreamProxy` startup and teardown on local loopback `127.0.0.1`.
- RFC 7233 range header parsing (`bytes=0-1024`, `bytes=2048-`).
- Coroutine-backed chunk streaming without thread blocking.
- Clean ViewModel and Composable decoupling.
