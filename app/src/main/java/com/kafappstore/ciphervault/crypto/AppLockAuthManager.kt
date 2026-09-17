package com.kafappstore.ciphervault.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hardware-backed / PBKDF2 Password and PIN Authenticator.
 * Computes salted hashes to verify user passcodes without storing plaintext credentials.
 */
object AppLockAuthManager {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hashPasscode(passcode: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$saltB64:$hashB64"
    }

    fun verifyPasscode(passcode: String, storedHashToken: String): Boolean {
        if (storedHashToken.isBlank()) return false
        val parts = storedHashToken.split(":")
        if (parts.size != 2) return false

        return try {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)

            val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val actualHash = skf.generateSecret(spec).encoded

            if (expectedHash.size != actualHash.size) return false
            var diff = 0
            for (i in expectedHash.indices) {
                diff = diff or (expectedHash[i].toInt() xor actualHash[i].toInt())
            }
            diff == 0
        } catch (_: Exception) {
            false
        }
    }
}
