package com.n8n.mobilemanager.data.remote

import java.net.URI

/**
 * Normalizes and validates the base URL before it reaches Retrofit.
 *
 * Retrofit requires an absolute HTTP(S) URL ending in a path-safe form. Keeping
 * this rule in one place prevents the login and settings forms from accepting
 * values that only fail later as a confusing network error.
 */
fun normalizeN8nBaseUrl(rawValue: String): Result<String> {
    val raw = rawValue.trim()
    if (raw.isBlank()) {
        return Result.failure(IllegalArgumentException("URL is required"))
    }

    val candidate = if (SCHEME_PATTERN.containsMatchIn(raw)) raw else "https://$raw"
    val uri = runCatching { URI(candidate) }.getOrElse {
        return Result.failure(IllegalArgumentException("Enter a valid n8n URL"))
    }

    val scheme = uri.scheme?.lowercase()
    if (scheme == null || !SUPPORTED_SCHEMES.contains(scheme) || uri.host.isNullOrBlank()) {
        return Result.failure(IllegalArgumentException("Use an http:// or https:// URL"))
    }
    if (uri.userInfo != null || uri.query != null || uri.fragment != null) {
        return Result.failure(IllegalArgumentException("URL must not contain credentials or query parameters"))
    }

    return Result.success(candidate.trimEnd('/'))
}

private val SCHEME_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
private val SUPPORTED_SCHEMES = setOf("http", "https")
