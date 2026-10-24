# Support & Community

## Official Channels

- **YouTube Channel:** [TRB FireOps (@trbfireops)](https://youtube.com/@trbfireops?si=LFRe4g7RiicR2EN0) — Official video walkthroughs, bot configuration guides, and architecture updates.
- **GitHub Issues:** [Issue Tracker](https://github.com/hackoilll000/AetherVault-Omni-Edition/issues) — File bugs and technical proposals.
- **Orion Store Listing:** [Orion Store](https://rookieenough.github.io/Orion-Data/redirect.html?id=aethervault-omni-edition&icon=https%3A%2F%2Fraw.githubusercontent.com%2Fhackoilll000%2FAetherVault-Omni-Edition%2Fmain%2Fassets%2Fapp_icon.png) — Store presence and update notifications.

---

## Frequently Asked Questions (FAQ)

### How do I configure my Telegram Bot Token?
1. Open `@BotFather` in Telegram and run `/newbot`.
2. Copy the HTTP API token provided by BotFather.
3. Open AetherVault Onboarding Wizard or Settings $\rightarrow$ Telegram Swarm.
4. Paste the token and specify your destination private channel (e.g. `-1004348906769`).
5. Ensure your Bot is added as an **Administrator** with "Post Messages" permission in the private channel.

### Can I run AAA games directly from Telegram?
No. Telegram provides passive file hosting and sequential chunk delivery, not remote GPU/CPU compute. AetherVault downloads, verifies, decrypts, and stages artifacts for local execution; it does not replace an interactive cloud gaming hypervisor.

### How does Media Streaming work without downloading the full video?
AetherVault starts an embedded local HTTP proxy server (`TelegramStreamProxy` on `127.0.0.1`) that translates ExoPlayer `Range: bytes=X-Y` requests into `HTTP 206 Partial Content`. This allows instantaneous seeking and scrubbing without waiting for 2 GB files to finish downloading.
