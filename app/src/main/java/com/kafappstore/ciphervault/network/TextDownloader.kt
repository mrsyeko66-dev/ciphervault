package com.kafappstore.ciphervault.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object TextDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val MAX_DOWNLOAD_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB limit

    suspend fun downloadTextFromUrl(rawUrl: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("URL cannot be empty."))
        }

        // Strict HTTPS requirement
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(SecurityException("Only secure HTTPS protocol is supported."))
        }

        try {
            val request = Request.Builder()
                .url(trimmed)
                .header("User-Agent", "CipherVault-Android/1.0")
                .header("Accept", "text/plain, text/*, application/json, application/xml, text/markdown")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Server error: Response code ${response.code}")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IOException("Received empty content from URL."))

                val contentType = body.contentType()?.toString()?.lowercase() ?: ""
                // Reject binary file types (audio, video, image, zip, exe, octet-stream without text)
                val binaryIndicators = listOf(
                    "image/", "audio/", "video/", "application/zip", "application/pdf",
                    "application/octet-stream", "application/vnd.android.package-archive"
                )
                if (binaryIndicators.any { contentType.startsWith(it) }) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Selected resource is a binary file ($contentType). Only text resources are supported.")
                    )
                }

                val contentLength = body.contentLength()
                if (contentLength > MAX_DOWNLOAD_SIZE_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("File size (${contentLength / 1024} KB) exceeds maximum limit (5 MB).")
                    )
                }

                val bytes = body.bytes()
                if (bytes.size > MAX_DOWNLOAD_SIZE_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Text content exceeds 5 MB limit.")
                    )
                }

                // Check if content looks like UTF-8 text (contains no NUL bytes in initial scan)
                val checkLimit = minOf(bytes.size, 1024)
                for (i in 0 until checkLimit) {
                    if (bytes[i] == 0.toByte()) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Downloaded file contains binary data. Only valid text files are allowed.")
                        )
                    }
                }

                val content = String(bytes, Charsets.UTF_8)
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(IOException("Server connection error: ${e.localizedMessage ?: "No Internet connection"}", e))
        }
    }
}
