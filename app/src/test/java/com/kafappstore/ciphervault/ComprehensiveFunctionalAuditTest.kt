package com.kafappstore.ciphervault

import com.kafappstore.ciphervault.crypto.CipherEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

/**
 * Deep functional and cryptographic logic audit test suite.
 */
class ComprehensiveFunctionalAuditTest {

    private val validPassword = "TestPassword2026!#"
    private val defaultPepper = CipherEngine.DEFAULT_PEPPER

    @Test
    fun testUnicodeAndPersianTextEncryption() {
        val persianText = """
            این یک متن آزمایشی فوق‌العاده حساس و محرمانه به زبان فارسی است.
            شامل کاراکترهای خاص: «»‌،؛﷼ و ایموجی‌ها: 🔐🛡️⚡💥
            شماره حساب: IR123456789012345678901234
        """.trimIndent()

        val encResult = CipherEngine.encrypt(persianText, validPassword, defaultPepper)
        assertTrue("Persian text encryption should succeed", encResult.isSuccess)
        val ciphertext = encResult.getOrThrow()

        val decResult = CipherEngine.decrypt(ciphertext, validPassword, defaultPepper)
        assertTrue("Persian text decryption should succeed", decResult.isSuccess)
        assertEquals(persianText, decResult.getOrThrow())
    }

    @Test
    fun testLargePayloadTextEncryption() {
        // Test 100,000 characters payload
        val sb = java.lang.StringBuilder()
        for (i in 0 until 1000) {
            sb.append("Chunk #$i: ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+ فارسی;\n")
        }
        val largeText = sb.toString()

        val encResult = CipherEngine.encrypt(largeText, validPassword, defaultPepper)
        assertTrue("Large payload encryption should succeed", encResult.isSuccess)

        val decResult = CipherEngine.decrypt(encResult.getOrThrow(), validPassword, defaultPepper)
        assertTrue("Large payload decryption should succeed", decResult.isSuccess)
        assertEquals(largeText, decResult.getOrThrow())
    }

    @Test
    fun testEmptyAndWhitespaceInputValidation() {
        // Empty text
        val emptyResult = CipherEngine.encrypt("", validPassword, defaultPepper)
        assertFalse("Empty plaintext must fail", emptyResult.isSuccess)

        // Empty password in standalone mode
        val emptyPwStandalone = CipherEngine.encrypt("Hello World", "", pepper = "")
        assertFalse("Empty password in standalone must fail", emptyPwStandalone.isSuccess)

        // Short password in standard mode (< 12)
        val shortPwResult = CipherEngine.encrypt("Hello World", "Short123!", defaultPepper)
        assertFalse("Password < 12 characters must fail", shortPwResult.isSuccess)

        // Password > 64 chars
        val longPwResult = CipherEngine.encrypt("Hello World", "A".repeat(65), defaultPepper)
        assertFalse("Password > 64 characters must fail", longPwResult.isSuccess)
    }

    @Test
    fun testCorruptedCiphertextTamperingResistance() {
        val originalText = "Sensitive banking credentials"
        val encResult = CipherEngine.encrypt(originalText, validPassword, defaultPepper)
        val validBase64 = encResult.getOrThrow()

        // 1. Corrupt Base64 decoding
        val corruptBase64 = validBase64.substring(0, 10) + "???" + validBase64.substring(13)
        val decCorruptBase64 = CipherEngine.decrypt(corruptBase64, validPassword, defaultPepper)
        assertFalse("Corrupted Base64 must be safely rejected", decCorruptBase64.isSuccess)

        // 2. Truncated payload
        val truncated = validBase64.take(30)
        val decTruncated = CipherEngine.decrypt(truncated, validPassword, defaultPepper)
        assertFalse("Truncated payload must fail", decTruncated.isSuccess)

        // 3. Bit-flipping attack (modifying one byte of ciphertext)
        val rawBytes = java.util.Base64.getDecoder().decode(validBase64)
        rawBytes[rawBytes.size - 5] = (rawBytes[rawBytes.size - 5].toInt() xor 0x01).toByte()
        val tamperedBase64 = java.util.Base64.getEncoder().encodeToString(rawBytes)
        val decTampered = CipherEngine.decrypt(tamperedBase64, validPassword, defaultPepper)
        assertFalse("Tampered ciphertext must fail authentication (AEAD MAC check)", decTampered.isSuccess)
    }

    @Test
    fun testWrongPasswordRejection() {
        val originalText = "Top Secret Operation Plans"
        val enc = CipherEngine.encrypt(originalText, validPassword, defaultPepper).getOrThrow()

        val wrongPwResult = CipherEngine.decrypt(enc, "WrongPassword2026!#", defaultPepper)
        assertFalse("Wrong password must fail decryption", wrongPwResult.isSuccess)
    }

    @Test
    fun testCustomPepperIsolation() {
        val customPepper1 = "CustomPepperSecret1234567890123"
        val customPepper2 = "DifferentPepperSecret0987654321"
        val text = "Isolated secret data"

        val enc = CipherEngine.encrypt(text, validPassword, customPepper1).getOrThrow()

        // Decrypt with different custom pepper must fail
        val decWithWrongPepper = CipherEngine.decrypt(enc, validPassword, customPepper2)
        assertFalse("Different pepper must fail", decWithWrongPepper.isSuccess)

        // Decrypt with correct custom pepper must succeed
        val decWithRightPepper = CipherEngine.decrypt(enc, validPassword, customPepper1)
        assertTrue("Correct pepper must succeed", decWithRightPepper.isSuccess)
        assertEquals(text, decWithRightPepper.getOrThrow())
    }

    @Test
    fun testStandaloneTextBackwardAndForwardCompatibility() {
        val strongPass = "SuperStrongStandalonePass2026!#"
        val text = "Standalone portable message"

        // Encrypted with pepper = "" (Standalone Mode)
        val enc = CipherEngine.encrypt(text, strongPass, pepper = "").getOrThrow()

        // Decrypt when user app is configured with default pepper: should automatically fallback & succeed!
        val decWithDefault = CipherEngine.decrypt(enc, strongPass, pepper = defaultPepper)
        assertTrue("Default app should successfully decrypt standalone payload", decWithDefault.isSuccess)
        assertEquals(text, decWithDefault.getOrThrow())

        // Decrypt with pepper = "" directly
        val decDirect = CipherEngine.decrypt(enc, strongPass, pepper = "")
        assertTrue("Direct standalone decrypt should succeed", decDirect.isSuccess)
        assertEquals(text, decDirect.getOrThrow())
    }

    @Test
    fun testZeroByteEmptyFileStream() {
        // Edge case: Empty 0-byte file stream
        val emptyBytes = ByteArray(0)
        val encOut = ByteArrayOutputStream()

        val encRes = CipherEngine.encryptStream(
            inputStream = ByteArrayInputStream(emptyBytes),
            outputStream = encOut,
            password = validPassword,
            pepper = defaultPepper,
            originalFileName = "empty.txt",
            originalExtension = "txt",
            totalBytes = 0L
        )
        assertTrue("Zero-byte file encryption should succeed", encRes.isSuccess)

        val encryptedStream = encOut.toByteArray()
        assertTrue("Encrypted zero-byte file must contain stream header and empty chunk", encryptedStream.isNotEmpty())

        val decOut = ByteArrayOutputStream()
        val decRes = CipherEngine.decryptStream(
            inputStream = ByteArrayInputStream(encryptedStream),
            outputStream = decOut,
            password = validPassword,
            pepper = defaultPepper
        )
        assertTrue("Zero-byte file decryption should succeed", decRes.isSuccess)
        assertEquals("empty.txt", decRes.getOrThrow().originalFileName)
        assertEquals("txt", decRes.getOrThrow().originalExtension)
        assertEquals(0, decOut.toByteArray().size)
    }

    @Test
    fun testMultiChunkStreamingAndBoundaryAlignment() {
        // Exact boundary: CHUNK_SIZE = 128KB
        // Test 128KB * 2.5 = 320 KB to test chunk splitting, boundary condition, and remainder
        val totalSize = (CipherEngine.CHUNK_SIZE * 2.5).toInt()
        val random = Random(12345)
        val originalData = ByteArray(totalSize)
        random.nextBytes(originalData)

        val encOut = ByteArrayOutputStream()
        var progressCallCount = 0

        val encRes = CipherEngine.encryptStream(
            inputStream = ByteArrayInputStream(originalData),
            outputStream = encOut,
            password = validPassword,
            pepper = defaultPepper,
            originalFileName = "video_clip.mp4",
            originalExtension = "mp4",
            totalBytes = totalSize.toLong()
        ) { _, _, _ ->
            progressCallCount++
        }
        assertTrue("Multi-chunk encryption should succeed", encRes.isSuccess)
        assertEquals(3L, encRes.getOrThrow().totalChunks) // 128k + 128k + 64k = 3 chunks

        val encryptedStream = encOut.toByteArray()

        // Test peek metadata on multi-chunk file
        val peek = CipherEngine.peekStreamMetadata(ByteArrayInputStream(encryptedStream), validPassword)
        assertTrue(peek.isSuccess)
        assertEquals("video_clip.mp4", peek.getOrThrow().originalFileName)
        assertEquals("mp4", peek.getOrThrow().originalExtension)

        // Decrypt
        val decOut = ByteArrayOutputStream()
        val decRes = CipherEngine.decryptStream(
            inputStream = ByteArrayInputStream(encryptedStream),
            outputStream = decOut,
            password = validPassword,
            pepper = defaultPepper,
            totalBytes = encryptedStream.size.toLong()
        )
        assertTrue("Multi-chunk decryption should succeed", decRes.isSuccess)
        assertEquals("video_clip.mp4", decRes.getOrThrow().originalFileName)
        assertEquals("mp4", decRes.getOrThrow().originalExtension)
        assertArrayEquals("Decrypted multi-chunk data must match original byte-for-byte", originalData, decOut.toByteArray())
    }

    @Test
    fun testPersianFileNameInStreamHeader() {
        val originalData = "محتوای محرمانه فایل آزمایشی".toByteArray(Charsets.UTF_8)
        val persianFileName = "گزارش مالی نهایی ۱۳۹۹.xlsx"
        val extension = "xlsx"

        val encOut = ByteArrayOutputStream()
        val encRes = CipherEngine.encryptStream(
            inputStream = ByteArrayInputStream(originalData),
            outputStream = encOut,
            password = validPassword,
            pepper = defaultPepper,
            originalFileName = persianFileName,
            originalExtension = extension
        )
        assertTrue(encRes.isSuccess)

        val peek = CipherEngine.peekStreamMetadata(ByteArrayInputStream(encOut.toByteArray()), validPassword)
        assertTrue(peek.isSuccess)
        assertEquals(persianFileName, peek.getOrThrow().originalFileName)
        assertEquals(extension, peek.getOrThrow().originalExtension)
    }

    @Test
    fun testKeyDerivationDistinctness() {
        val salt = ByteArray(16) { it.toByte() }
        val aesKey = CipherEngine.deriveKey(validPassword, salt)
        val chachaKey = CipherEngine.deriveChachaKey(validPassword, defaultPepper, salt)
        val standaloneChachaKey = CipherEngine.deriveChachaKey(validPassword, "", salt)

        assertEquals(32, aesKey.size)
        assertEquals(32, chachaKey.size)
        assertEquals(32, standaloneChachaKey.size)

        // AES key and ChaCha key must be completely different
        assertFalse("AES key and ChaCha key must be distinct", aesKey.contentEquals(chachaKey))
        assertFalse("Standalone ChaCha key must be distinct from AES key", aesKey.contentEquals(standaloneChachaKey))
        assertFalse("Standalone ChaCha key must be distinct from peppered ChaCha key", chachaKey.contentEquals(standaloneChachaKey))
    }

    @Test
    fun testPasswordWhitespaceSensitivity() {
        val basePassword = "MySecurePassword2026!#"
        val passwordWithSpaces = "  MySecurePassword2026!#  "

        val enc = CipherEngine.encrypt("Sensitive Document", basePassword, defaultPepper).getOrThrow()

        // Decrypt with untrimmed spaced password fails
        val decWithSpaces = CipherEngine.decrypt(enc, passwordWithSpaces, defaultPepper)
        assertFalse("Decryption with untrimmed spaces must fail", decWithSpaces.isSuccess)

        // Decrypt with trimmed password succeeds
        val decTrimmed = CipherEngine.decrypt(enc, passwordWithSpaces.trim(), defaultPepper)
        assertTrue("Decryption with trimmed password must succeed", decTrimmed.isSuccess)
        assertEquals("Sensitive Document", decTrimmed.getOrThrow())
    }

    @Test
    fun testStreamingAbortAndCancellation() {
        val largeData = ByteArray(5 * 1024 * 1024) // 5MB simulated file
        Random(1234).nextBytes(largeData)

        val inStream = ByteArrayInputStream(largeData)
        val outStream = ByteArrayOutputStream()

        var chunksProcessed = 0
        var shouldCancel = false

        val encResult = CipherEngine.encryptStream(
            inputStream = inStream,
            outputStream = outStream,
            password = validPassword,
            pepper = defaultPepper,
            originalFileName = "large_test.bin",
            originalExtension = "bin",
            totalBytes = largeData.size.toLong(),
            isCancelled = {
                chunksProcessed++
                if (chunksProcessed >= 2) {
                    shouldCancel = true
                }
                shouldCancel
            }
        )

        assertFalse("Operation should fail/abort when cancelled", encResult.isSuccess)
        assertTrue(
            "Exception should indicate cancellation",
            encResult.exceptionOrNull() is java.util.concurrent.CancellationException
        )
    }
}
