# Contributing to Tele Drive / AetherVault Omni Edition

Thank you for considering contributing to Tele Drive / AetherVault Omni Edition!

## Code of Conduct
This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to `developer@aethervault.local`.

## How Can I Contribute?

### Reporting Bugs
Bugs are tracked as GitHub Issues. Before creating a bug report, please check existing issues to ensure it hasn't been reported yet.

When creating a bug report, please include:
- A clear, descriptive title.
- Steps to reproduce the issue.
- Expected behavior vs actual behavior.
- Device information (Android version, device model).

### Submitting Pull Requests
1. Fork the repository.
2. Create a topic branch from `main`: `git checkout -b feature/my-cool-feature`
3. Make your changes adhering to standard Kotlin & Jetpack Compose best practices.
4. Ensure the app builds cleanly with `gradle assembleDebug`.
5. Write clear, concise commit messages.
6. Push to your fork and submit a Pull Request.

## Development Setup
1. Clone the repository: `git clone https://github.com/hackoilll000/AetherVault-Omni-Edition.git`
2. Open the project in Android Studio (Iguana / Jellyfish or newer).
3. Ensure JDK 17 is configured in Android Studio.
4. Sync Gradle and run the app on an Android emulator or physical device.

## Design Guidelines
- All UI components should follow **Google Material Design 3 (Material You)** specifications.
- Maintain responsive, adaptive layouts using Jetpack Compose.
- Do not alter core AES-256-GCM encryption or Telegram Bot API pipeline logic without thorough security review.

## License
By contributing, you agree that your contributions will be licensed under the AetherVault Source-Available Personal & Educational License v1.0 (see `LICENSE`).
