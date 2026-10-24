package com.example.features.linkmanager

import okhttp3.OkHttpClient
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Registry & Link Resolver for Universal Link Manager
 */
object UniversalLinkRegistry {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val adapters: List<PlatformAdapter> = listOf(
        GoogleDriveAdapter(),
        YouTubeAdapter(),
        InstagramAdapter(),
        FacebookAdapter(),
        GooglePlayAdapter(),
        DirectFileAdapter() // Universal fallback
    )

    private val BLOCKED_HOSTS = listOf(
        "localhost", "127.0.0.1", "0.0.0.0", "::1", "169.254.169.254"
    )

    /**
     * Validates URL security, syntax, and scheme.
     * Returns Pair(isValid, sanitizedURI or errorMsg)
     */
    fun validateLinkSecurity(urlStr: String): Pair<Boolean, String> {
        val trimmed = urlStr.trim()
        if (trimmed.isBlank()) {
            return Pair(false, "URL cannot be empty")
        }

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                return Pair(false, "Only HTTP and HTTPS protocols are supported")
            }

            val host = uri.host?.lowercase() ?: return Pair(false, "Invalid host in URL")
            if (BLOCKED_HOSTS.contains(host) ||
                host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.startsWith("172.16.")
            ) {
                return Pair(false, "Security Error: Localhost and private IP addresses are blocked (SSRF Protection)")
            }

            Pair(true, "URL is valid")
        } catch (e: Exception) {
            Pair(false, "Malformed URL: ${e.localizedMessage}")
        }
    }

    /**
     * Identifies the platform for a validated URI
     */
    fun detectPlatform(uri: URI): SupportedPlatform {
        for (adapter in adapters) {
            if (adapter !is DirectFileAdapter && adapter.canHandle(uri)) {
                return adapter.platform
            }
        }
        return SupportedPlatform.DIRECT_HTTP
    }

    /**
     * Resolves a URL into a structured ResolvedLinkResource
     */
    suspend fun resolve(rawUrl: String): Result<ResolvedLinkResource> {
        val (isValid, validationMsg) = validateLinkSecurity(rawUrl)
        if (!isValid) {
            return Result.failure(IllegalArgumentException(validationMsg))
        }

        val uri = try {
            URI(rawUrl.trim())
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // Find primary matching adapter
        for (adapter in adapters) {
            if (adapter.canHandle(uri)) {
                val res = adapter.resolve(rawUrl.trim(), uri, httpClient)
                if (res.isSuccess) {
                    return res
                }
            }
        }

        return Result.failure(IllegalStateException("No platform adapter could resolve link: $rawUrl"))
    }
}
