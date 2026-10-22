# Android Keystore & Gradle Signing Management

**Security Policy**: Never commit production release `.keystore` or `.jks` files to Git.

---

## Production Release Signing Configuration

Set the following environment variables in CI/CD (e.g. GitHub Secrets):

* `RELEASE_KEY_ALIAS`: Your Android Keystore key alias.
* `RELEASE_KEY_PASSWORD`: Key password.
* `RELEASE_STORE_PASSWORD`: Keystore store password.
* `RELEASE_STORE_FILE_BASE64`: Base64 encoded `.keystore` or `.jks` file.

`app/build.gradle.kts` dynamically inspects these environment variables when assembling release builds.
