package com.kafappstore.ciphervault.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class CipherEngineTest {

    @Test
    fun testStreamEncryptAndDecrypt() {
        val random = Random(42)
        // Simulate a 300KB binary file (like a PDF)
        val originalBytes = ByteArray(300 * 1024)
        random.nextBytes(originalBytes)
        // PDF header
        val pdfHeader = "%PDF-1.5\n".toByteArray(Charsets.US_ASCII)
        System.arraycopy(pdfHeader, 0, originalBytes, 0, pdfHeader.size)

        val password = "StrongPassword123!"
        val pepper = CipherEngine.DEFAULT_PEPPER
        val fileName = "test_document.pdf"
        val fileExt = "pdf"

        val encOut = ByteArrayOutputStream()
        val encResult = CipherEngine.encryptStream(
            inputStream = ByteArrayInputStream(originalBytes),
            outputStream = encOut,
            password = password,
            pepper = pepper,
            originalFileName = fileName,
            originalExtension = fileExt,
            totalBytes = originalBytes.size.toLong()
        )
        assertTrue(encResult.isSuccess)

        val encryptedBytes = encOut.toByteArray()
        assertTrue(encryptedBytes.isNotEmpty())

        // Test peekStreamMetadata
        val peekResult = CipherEngine.peekStreamMetadata(
            inputStream = ByteArrayInputStream(encryptedBytes),
            password = password
        )
        assertTrue(peekResult.isSuccess)
        assertEquals("test_document.pdf", peekResult.getOrThrow().originalFileName)
        assertEquals("pdf", peekResult.getOrThrow().originalExtension)

        val decOut = ByteArrayOutputStream()
        val decResult = CipherEngine.decryptStream(
            inputStream = ByteArrayInputStream(encryptedBytes),
            outputStream = decOut,
            password = password,
            pepper = pepper,
            totalBytes = encryptedBytes.size.toLong()
        )
        assertTrue(decResult.isSuccess)
        val meta = decResult.getOrThrow()
        assertEquals("test_document.pdf", meta.originalFileName)
        assertEquals("pdf", meta.originalExtension)

        val decryptedBytes = decOut.toByteArray()
        assertEquals(originalBytes.size, decryptedBytes.size)
        assertArrayEquals(originalBytes, decryptedBytes)
    }

    @Test
    fun testStandaloneStreamEncryptAndDecryptWithoutPepper() {
        val originalBytes = "This is a secret document encrypted without secret key!".toByteArray(Charsets.UTF_8)
        val strongPassword = "SuperStrongPassword2026!#"

        val encOut = ByteArrayOutputStream()
        val encResult = CipherEngine.encryptStream(
            inputStream = ByteArrayInputStream(originalBytes),
            outputStream = encOut,
            password = strongPassword,
            pepper = "", // Standalone mode (No Secret Key)
            originalFileName = "my_report.pdf",
            originalExtension = "pdf"
        )
        assertTrue(encResult.isSuccess)

        val encryptedBytes = encOut.toByteArray()

        // Decrypt with standard pepper configured in settings - should automatically detect standalone mode!
        val decOut = ByteArrayOutputStream()
        val decResult = CipherEngine.decryptStream(
            inputStream = ByteArrayInputStream(encryptedBytes),
            outputStream = decOut,
            password = strongPassword,
            pepper = CipherEngine.DEFAULT_PEPPER
        )
        assertTrue(decResult.isSuccess)
        assertEquals("my_report.pdf", decResult.getOrThrow().originalFileName)
        assertEquals("pdf", decResult.getOrThrow().originalExtension)
        assertArrayEquals(originalBytes, decOut.toByteArray())
    }

    @Test
    fun testStandaloneTextEncryptAndDecrypt() {
        val plaintext = "Top secret standalone text payload."
        val strongPassword = "SecurePass12345!@#"

        val encResult = CipherEngine.encrypt(plaintext, strongPassword, pepper = "")
        assertTrue(encResult.isSuccess)
        val cipherBase64 = encResult.getOrThrow()

        // Decrypt with pepper configured
        val decResult = CipherEngine.decrypt(cipherBase64, strongPassword, pepper = CipherEngine.DEFAULT_PEPPER)
        assertTrue(decResult.isSuccess)
        assertEquals(plaintext, decResult.getOrThrow())
    }

    @Test
    fun testPasswordStrengthEvaluation() {
        // Weak password (short)
        val weak = CipherEngine.evaluatePasswordStrength("Short1!")
        assertEquals(false, weak.first)

        // Missing symbol
        val noSymbol = CipherEngine.evaluatePasswordStrength("Password12345678")
        assertEquals(false, noSymbol.first)

        // Missing uppercase
        val noUpper = CipherEngine.evaluatePasswordStrength("password12345678!@")
        assertEquals(false, noUpper.first)

        // Very strong password
        val strong = CipherEngine.evaluatePasswordStrength("CyberMatrix2026!#Vault")
        assertEquals(true, strong.first)
    }
}
