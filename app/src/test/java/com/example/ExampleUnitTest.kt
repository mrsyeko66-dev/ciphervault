package com.example

import com.example.crypto.CipherEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class ExampleUnitTest {

    @Test
    fun testCipherEngine_EncryptAndDecryptSuccess() {
        val originalText = "این یک پیام فوق محرمانه در سامانه CipherVault است. Top Secret Data 123456!"
        val password = "StrongPassword@123"

        val encryptResult = CipherEngine.encrypt(originalText, password)
        assertTrue("Encryption must succeed", encryptResult.isSuccess)

        val base64Ciphertext = encryptResult.getOrThrow()
        assertTrue("Ciphertext should not be empty", base64Ciphertext.isNotEmpty())
        assertNotEquals("Ciphertext must not be plaintext", originalText, base64Ciphertext)

        // Decrypt with correct password
        val decryptResult = CipherEngine.decrypt(base64Ciphertext, password)
        assertTrue("Decryption must succeed with correct password", decryptResult.isSuccess)
        assertEquals("Decrypted text must match original plaintext", originalText, decryptResult.getOrThrow())
    }

    @Test
    fun testCipherEngine_WrongPasswordFails() {
        val originalText = "Critical security memo"
        val password = "ValidPassword#999"
        val wrongPassword = "WrongPassword#111"

        val encryptResult = CipherEngine.encrypt(originalText, password)
        assertTrue(encryptResult.isSuccess)

        val decryptResult = CipherEngine.decrypt(encryptResult.getOrThrow(), wrongPassword)
        assertFalse("Decryption must fail with wrong password", decryptResult.isSuccess)
    }

    @Test
    fun testCipherEngine_PasswordLengthEnforcement() {
        val text = "Sample payload"

        // Too short (< 12)
        val shortPwResult = CipherEngine.encrypt(text, "ShortPass1!")
        assertFalse("Password < 12 characters must fail", shortPwResult.isSuccess)

        // Too long (> 22)
        val longPwResult = CipherEngine.encrypt(text, "SuperLongPasswordExceeding22Chars!")
        assertFalse("Password > 22 characters must fail", longPwResult.isSuccess)

        // Valid length (12 to 22)
        val validPwResult = CipherEngine.encrypt(text, "ValidPass12345#")
        assertTrue("Password between 12 and 22 chars must succeed", validPwResult.isSuccess)
    }

    @Test
    fun testCipherEngine_Base64FormatContainsSaltAndIv() {
        val text = "Testing Header format"
        val password = "MasterKey@2026"

        val encryptResult = CipherEngine.encrypt(text, password)
        assertTrue(encryptResult.isSuccess)

        val decodedBytes = Base64.getDecoder().decode(encryptResult.getOrThrow())
        // Salt is 16 bytes, IV is 12 bytes -> total header is 28 bytes + at least 32 bytes auth tags
        assertTrue("Payload must be at least 60 bytes", decodedBytes.size >= 60)
    }
}
