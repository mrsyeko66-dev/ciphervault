package com.kafappstore.ciphervault.crypto

import android.util.Base64
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * CipherVault Multi-Stage Cascading Cryptographic Engine
 *
 * Algorithm Sequence:
 * Stage 1: AES-256-GCM with key derived from user password
 * Stage 2: ChaCha20-Poly1305 with key derived from (password + Pepper)
 * Stage 3: XOR stream transformation with key derived from SHA-256
 *
 * Key Management:
 * - PBKDF2 with HMAC-SHA256, 600,000 iterations
 * - 16-byte cryptographically secure random Salt
 * - 12-byte cryptographically secure random IV / Nonce
 * - 32-character embedded secret Pepper
 *
 * Output Format:
 * Base64 string containing:
 * - Bytes 0..15  : Salt (16 bytes)
 * - Bytes 16..27 : IV (12 bytes)
 * - Bytes 28..end: Multi-layer ciphertext
 */
object CipherEngine {

    const val DEFAULT_PEPPER = "CV_#9kL!mQ89zW2@pX5&vN7*rT1^yB4%"
    private const val PBKDF2_ITERATIONS = 600_000
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val KEY_SIZE_BITS = 256
    private const val KEY_SIZE_BYTES = 32

    private val secureRandom = SecureRandom()

    /**
     * Evaluates password strength for Standalone Mode (without secret key).
     * Requirements: length >= 14, uppercase, lowercase, number, symbol.
     */
    fun evaluatePasswordStrength(password: String): Triple<Boolean, Int, List<String>> {
        val missing = mutableListOf<String>()
        val hasLength = password.length >= 14
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        if (!hasLength) missing.add("Minimum 14 characters (current: ${password.length})")
        if (!hasUpper) missing.add("At least one uppercase letter (A-Z)")
        if (!hasLower) missing.add("At least one lowercase letter (a-z)")
        if (!hasDigit) missing.add("At least one digit (0-9)")
        if (!hasSpecial) missing.add("At least one symbol / special character (!@#$)")

        var score = 0
        if (hasLength) score++
        if (hasUpper && hasLower) score++
        if (hasDigit) score++
        if (hasSpecial) score++

        val isVeryStrong = hasLength && hasUpper && hasLower && hasDigit && hasSpecial
        return Triple(isVeryStrong, score, missing)
    }

    /**
     * Derives a 256-bit key using PBKDF2-HMAC-SHA256 with 600,000 iterations.
     */
    fun deriveKey(secret: String, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): ByteArray {
        val generator = PKCS5S2ParametersGenerator(SHA256Digest())
        val passwordBytes = secret.toByteArray(Charsets.UTF_8)
        try {
            generator.init(passwordBytes, salt, iterations)
            val keyParam = generator.generateDerivedParameters(KEY_SIZE_BITS) as KeyParameter
            return keyParam.key.clone()
        } finally {
            passwordBytes.fill(0)
        }
    }

    /**
     * Derives ChaCha20 key. In Standalone Mode (empty pepper), uses an internal cryptographic salt-expansion
     * domain separator so AES and ChaCha keys are cryptographically distinct.
     */
    fun deriveChachaKey(password: String, pepper: String, salt: ByteArray): ByteArray {
        val effectiveSecret = if (pepper.isEmpty()) {
            "$password#CVLT_STANDALONE_NO_PEPPER"
        } else {
            password + pepper
        }
        return deriveKey(effectiveSecret, salt)
    }

    /**
     * Stage 1: AES-256-GCM Encryption
     */
    private fun encryptAesGcm(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(plaintext)
    }

    /**
     * Stage 1: AES-256-GCM Decryption
     */
    private fun decryptAesGcm(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Stage 2: ChaCha20-Poly1305 Encryption via BouncyCastle
     */
    private fun encryptChaCha20Poly1305(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = ChaCha20Poly1305()
        val params = AEADParameters(KeyParameter(key), 128, iv)
        cipher.init(true, params)
        val output = ByteArray(cipher.getOutputSize(plaintext.size))
        val len = cipher.processBytes(plaintext, 0, plaintext.size, output, 0)
        val finalLen = cipher.doFinal(output, len)
        return output.copyOf(len + finalLen)
    }

    /**
     * Stage 2: ChaCha20-Poly1305 Decryption via BouncyCastle
     */
    private fun decryptChaCha20Poly1305(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = ChaCha20Poly1305()
        val params = AEADParameters(KeyParameter(key), 128, iv)
        cipher.init(false, params)
        val output = ByteArray(cipher.getOutputSize(ciphertext.size))
        val len = cipher.processBytes(ciphertext, 0, ciphertext.size, output, 0)
        val finalLen = cipher.doFinal(output, len)
        return output.copyOf(len + finalLen)
    }

    /**
     * Stage 3: SHA-256 Keystream XOR Transformation (Self-inverting)
     */
    private fun applyXorTransformation(input: ByteArray, key: ByteArray, iv: ByteArray, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(key)
        md.update(iv)
        md.update(salt)
        val seed = md.digest()

        val output = ByteArray(input.size)
        val chunkDigest = MessageDigest.getInstance("SHA-256")
        var counter = 0
        var block = seed

        for (i in input.indices) {
            val blockOffset = i % 32
            if (blockOffset == 0 && i > 0) {
                counter++
                chunkDigest.reset()
                chunkDigest.update(seed)
                chunkDigest.update(byteArrayOf(
                    (counter shr 24).toByte(),
                    (counter shr 16).toByte(),
                    (counter shr 8).toByte(),
                    counter.toByte()
                ))
                block = chunkDigest.digest()
            }
            output[i] = (input[i].toInt() xor block[blockOffset].toInt()).toByte()
        }
        return output
    }

    /**
     * Full Cascaded Encryption
     * Returns Base64 encoded payload: [Salt (16)][IV (12)][Ciphertext (variable)]
     */
    fun encrypt(
        plaintext: String,
        password: String,
        pepper: String = DEFAULT_PEPPER
    ): Result<String> {
        if (pepper.isEmpty()) {
            val strength = evaluatePasswordStrength(password)
            if (!strength.first) {
                return Result.failure(IllegalArgumentException("Standalone encryption requires a very strong password: ${strength.third.joinToString(", ")}"))
            }
        } else if (password.length !in 12..64) {
            return Result.failure(IllegalArgumentException("Password length must be between 12 and 64 characters."))
        }
        if (plaintext.isEmpty()) {
            return Result.failure(IllegalArgumentException("Input text cannot be empty."))
        }

        var aesKey: ByteArray? = null
        var chachaKey: ByteArray? = null
        try {
            val salt = ByteArray(SALT_SIZE).apply { secureRandom.nextBytes(this) }
            val iv = ByteArray(IV_SIZE).apply { secureRandom.nextBytes(this) }

            // Key Derivation
            aesKey = deriveKey(password, salt)
            chachaKey = deriveChachaKey(password, pepper, salt)

            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)

            // Stage 1: AES-256-GCM
            val stage1 = encryptAesGcm(plaintextBytes, aesKey, iv)

            // Stage 2: ChaCha20-Poly1305
            val stage2 = encryptChaCha20Poly1305(stage1, chachaKey, iv)

            // Stage 3: XOR Stream
            val stage3 = applyXorTransformation(stage2, chachaKey, iv, salt)

            // Package final payload: Salt + IV + Ciphertext
            val finalPayload = ByteArray(SALT_SIZE + IV_SIZE + stage3.size)
            System.arraycopy(salt, 0, finalPayload, 0, SALT_SIZE)
            System.arraycopy(iv, 0, finalPayload, SALT_SIZE, IV_SIZE)
            System.arraycopy(stage3, 0, finalPayload, SALT_SIZE + IV_SIZE, stage3.size)

            val base64Output = encodeBase64(finalPayload)
            return Result.success(base64Output)
        } catch (e: Exception) {
            return Result.failure(Exception("Encryption error: ${e.message ?: "Unknown error"}", e))
        } finally {
            // Strict memory zeroization
            aesKey?.fill(0)
            chachaKey?.fill(0)
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str)
        } catch (_: Throwable) {
            android.util.Base64.decode(str, android.util.Base64.DEFAULT)
        }
    }

    /**
     * Full Cascaded Decryption
     * Takes Base64 encoded payload and password, returns original plaintext.
     * Automatically attempts standard pepper and standalone (no pepper) fallback.
     */
    fun decrypt(
        base64Payload: String,
        password: String,
        pepper: String = DEFAULT_PEPPER
    ): Result<String> {
        val firstAttempt = decryptInternal(base64Payload, password, pepper)
        if (firstAttempt.isSuccess) return firstAttempt

        // Fallback: If configured with a non-empty pepper, try Standalone mode (pepper = "")
        if (pepper.isNotEmpty()) {
            val standaloneAttempt = decryptInternal(base64Payload, password, "")
            if (standaloneAttempt.isSuccess) return standaloneAttempt
        }

        return firstAttempt
    }

    private fun decryptInternal(
        base64Payload: String,
        password: String,
        pepper: String
    ): Result<String> {
        val cleanPayload = base64Payload.trim().replace("\n", "").replace("\r", "")
        if (cleanPayload.isEmpty()) {
            return Result.failure(IllegalArgumentException("Ciphertext payload is empty."))
        }

        var aesKey: ByteArray? = null
        var chachaKey: ByteArray? = null
        try {
            val allBytes = try {
                decodeBase64(cleanPayload)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("Invalid Base64 format."))
            }

            // Minimum required length: 16 (salt) + 12 (iv) + 16 (GCM tag) + 16 (Poly1305 tag) = 60
            if (allBytes.size < (SALT_SIZE + IV_SIZE + 32)) {
                return Result.failure(IllegalArgumentException("Insufficient payload length or corrupted data."))
            }

            val salt = allBytes.copyOfRange(0, SALT_SIZE)
            val iv = allBytes.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
            val stage3Ciphertext = allBytes.copyOfRange(SALT_SIZE + IV_SIZE, allBytes.size)

            // Key Derivation
            aesKey = deriveKey(password, salt)
            chachaKey = deriveChachaKey(password, pepper, salt)

            // Reverse Stage 3: XOR
            val stage2Ciphertext = applyXorTransformation(stage3Ciphertext, chachaKey, iv, salt)

            // Reverse Stage 2: ChaCha20-Poly1305
            val stage1Ciphertext = decryptChaCha20Poly1305(stage2Ciphertext, chachaKey, iv)

            // Reverse Stage 1: AES-256-GCM
            val plainBytes = decryptAesGcm(stage1Ciphertext, aesKey, iv)

            val plaintext = String(plainBytes, Charsets.UTF_8)
            return Result.success(plaintext)
        } catch (e: Exception) {
            // Decryption failure (Bad tag, wrong key, padding exception, etc.)
            return Result.failure(SecurityException("Incorrect password or corrupted ciphertext payload."))
        } finally {
            // Strict memory zeroization
            aesKey?.fill(0)
            chachaKey?.fill(0)
        }
    }

    data class StreamingResult(
        val originalFileName: String,
        val outputExtension: String,
        val bytesProcessed: Long,
        val totalChunks: Long,
        val durationMs: Long
    )

    data class StreamingDecryptedMetadata(
        val originalFileName: String,
        val originalExtension: String,
        val bytesProcessed: Long,
        val totalChunks: Long,
        val durationMs: Long
    )

    val STREAM_MAGIC = byteArrayOf(0x43, 0x56, 0x4C, 0x54) // 'CVLT'
    const val STREAM_VERSION: Byte = 1
    const val CHUNK_SIZE = 128 * 1024 // 128 KB chunks

    private fun computeChunkIv(baseIv: ByteArray, chunkIndex: Long): ByteArray {
        val chunkIv = baseIv.clone()
        for (i in 0 until 8) {
            val shift = i * 8
            val b = ((chunkIndex ushr shift) and 0xFF).toByte()
            val targetIdx = chunkIv.size - 1 - i
            chunkIv[targetIdx] = (chunkIv[targetIdx].toInt() xor b.toInt()).toByte()
        }
        return chunkIv
    }

    /**
     * Cascaded Streaming Encryption for arbitrary sized files (supports 1GB+ files with constant ~256KB memory footprint).
     */
    fun encryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        password: String,
        pepper: String = DEFAULT_PEPPER,
        originalFileName: String = "",
        originalExtension: String = "bin",
        totalBytes: Long = -1L,
        isCancelled: () -> Boolean = { false },
        onProgress: (bytesProcessed: Long, totalBytes: Long, progressPercent: Float) -> Unit = { _, _, _ -> }
    ): Result<StreamingResult> {
        if (pepper.isEmpty()) {
            val strength = evaluatePasswordStrength(password)
            if (!strength.first) {
                return Result.failure(IllegalArgumentException("Standalone encryption requires a very strong password: ${strength.third.joinToString(", ")}"))
            }
        } else if (password.length !in 12..64) {
            return Result.failure(IllegalArgumentException("Password length must be between 12 and 64 characters."))
        }

        val startTime = System.currentTimeMillis()
        var aesKey: ByteArray? = null
        var chachaKey: ByteArray? = null

        try {
            val salt = ByteArray(SALT_SIZE).apply { secureRandom.nextBytes(this) }
            val baseIv = ByteArray(IV_SIZE).apply { secureRandom.nextBytes(this) }

            aesKey = deriveKey(password, salt)
            chachaKey = deriveChachaKey(password, pepper, salt)

            val outData = DataOutputStream(outputStream)

            // Write Header
            outData.write(STREAM_MAGIC)
            outData.writeByte(STREAM_VERSION.toInt())
            outData.write(salt)
            outData.write(baseIv)

            // Encrypted Metadata Header (includes standalone flag)
            val isStandalone = pepper.isEmpty()
            val metaString = "$originalFileName|$originalExtension|${System.currentTimeMillis()}|${if (isStandalone) "standalone" else "pepper"}"
            val encMeta = encryptAesGcm(metaString.toByteArray(Charsets.UTF_8), aesKey, baseIv)
            outData.writeShort(encMeta.size)
            outData.write(encMeta)

            // Chunked Streaming
            val buffer1 = ByteArray(CHUNK_SIZE)
            val buffer2 = ByteArray(CHUNK_SIZE)
            var activeBuf = buffer1
            var peekBuf = buffer2

            var activeLen = 0
            while (activeLen < CHUNK_SIZE) {
                val r = inputStream.read(activeBuf, activeLen, CHUNK_SIZE - activeLen)
                if (r < 0) break
                activeLen += r
            }

            var chunkIdx = 0L
            var totalProcessed = 0L

            if (activeLen <= 0) {
                // Empty file
                val chunkIv = computeChunkIv(baseIv, 0L)
                val s1 = encryptAesGcm(ByteArray(0), aesKey, chunkIv)
                val s2 = encryptChaCha20Poly1305(s1, chachaKey, chunkIv)
                val s3 = applyXorTransformation(s2, chachaKey, chunkIv, salt)
                outData.writeByte(1)
                outData.writeInt(s3.size)
                outData.write(s3)
                chunkIdx = 1L
            } else {
                while (true) {
                    if (isCancelled()) {
                        return Result.failure(java.util.concurrent.CancellationException("Operation cancelled by user."))
                    }
                    var peekLen = 0
                    while (peekLen < CHUNK_SIZE) {
                        val r = inputStream.read(peekBuf, peekLen, CHUNK_SIZE - peekLen)
                        if (r < 0) break
                        peekLen += r
                    }
                    val isLast = (peekLen <= 0)
                    val plainChunk = if (activeLen == activeBuf.size) activeBuf else activeBuf.copyOf(activeLen)
                    val chunkIv = computeChunkIv(baseIv, chunkIdx)

                    val s1 = encryptAesGcm(plainChunk, aesKey, chunkIv)
                    val s2 = encryptChaCha20Poly1305(s1, chachaKey, chunkIv)
                    val s3 = applyXorTransformation(s2, chachaKey, chunkIv, salt)

                    outData.writeByte(if (isLast) 1 else 0)
                    outData.writeInt(s3.size)
                    outData.write(s3)

                    totalProcessed += activeLen
                    chunkIdx++

                    val pct = if (totalBytes > 0) (totalProcessed.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                    onProgress(totalProcessed, totalBytes, pct)

                    if (isLast) break

                    activeBuf = peekBuf.also { peekBuf = activeBuf }
                    activeLen = peekLen
                }
            }

            outData.flush()
            val duration = System.currentTimeMillis() - startTime
            return Result.success(
                StreamingResult(
                    originalFileName = originalFileName,
                    outputExtension = "cvault",
                    bytesProcessed = totalProcessed,
                    totalChunks = chunkIdx,
                    durationMs = duration
                )
            )
        } catch (e: Exception) {
            return Result.failure(Exception("Streaming encryption error: ${e.message ?: "Unknown error"}", e))
        } finally {
            aesKey?.fill(0)
            chachaKey?.fill(0)
        }
    }

    /**
     * Inspects stream header and extracts metadata (filename, extension) if password is valid,
     * without writing chunks.
     */
    fun peekStreamMetadata(
        inputStream: InputStream,
        password: String
    ): Result<StreamingDecryptedMetadata> {
        return try {
            val inData = DataInputStream(inputStream)
            val magic = ByteArray(4)
            inData.readFully(magic)
            if (!magic.contentEquals(STREAM_MAGIC)) {
                return Result.failure(IllegalArgumentException("Invalid file format or incompatible CipherVault signature."))
            }
            val version = inData.readByte()
            if (version.toInt() != STREAM_VERSION.toInt()) {
                return Result.failure(IllegalArgumentException("Unsupported cipher stream format version."))
            }
            val salt = ByteArray(SALT_SIZE)
            inData.readFully(salt)
            val baseIv = ByteArray(IV_SIZE)
            inData.readFully(baseIv)

            val aesKey = deriveKey(password, salt)
            val metaLen = inData.readShort().toInt() and 0xFFFF
            if (metaLen <= 0 || metaLen > 4096) {
                return Result.failure(SecurityException("Incorrect password or corrupted file header."))
            }
            val encMeta = ByteArray(metaLen)
            inData.readFully(encMeta)

            val metaPlainBytes = try {
                decryptAesGcm(encMeta, aesKey, baseIv)
            } catch (e: Exception) {
                return Result.failure(SecurityException("Incorrect password or corrupted file."))
            }

            val metaString = String(metaPlainBytes, Charsets.UTF_8)
            val metaParts = metaString.split("|")
            val originalName = metaParts.getOrElse(0) { "decrypted_file" }
            val originalExt = metaParts.getOrElse(1) { "bin" }

            Result.success(
                StreamingDecryptedMetadata(
                    originalFileName = originalName,
                    originalExtension = originalExt,
                    bytesProcessed = 0L,
                    totalChunks = 0L,
                    durationMs = 0L
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cascaded Streaming Decryption for arbitrary sized files (supports 1GB+ files with constant ~256KB memory footprint).
     */
    fun decryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        password: String,
        pepper: String = DEFAULT_PEPPER,
        totalBytes: Long = -1L,
        isCancelled: () -> Boolean = { false },
        onProgress: (bytesProcessed: Long, totalBytes: Long, progressPercent: Float) -> Unit = { _, _, _ -> }
    ): Result<StreamingDecryptedMetadata> {
        val startTime = System.currentTimeMillis()
        var aesKey: ByteArray? = null
        var chachaKey: ByteArray? = null

        try {
            val inData = DataInputStream(inputStream)

            // Read & Verify Magic
            val magic = ByteArray(4)
            inData.readFully(magic)
            if (!magic.contentEquals(STREAM_MAGIC)) {
                return Result.failure(IllegalArgumentException("Invalid file format or incompatible CipherVault signature."))
            }

            val version = inData.readByte()
            if (version.toInt() != STREAM_VERSION.toInt()) {
                return Result.failure(IllegalArgumentException("Unsupported cipher stream format version."))
            }

            val salt = ByteArray(SALT_SIZE)
            inData.readFully(salt)
            val baseIv = ByteArray(IV_SIZE)
            inData.readFully(baseIv)

            aesKey = deriveKey(password, salt)

            // Read Encrypted Metadata
            val metaLen = inData.readShort().toInt() and 0xFFFF
            if (metaLen <= 0 || metaLen > 4096) {
                return Result.failure(SecurityException("Incorrect password or corrupted file header."))
            }
            val encMeta = ByteArray(metaLen)
            inData.readFully(encMeta)

            val metaPlainBytes = try {
                decryptAesGcm(encMeta, aesKey, baseIv)
            } catch (e: Exception) {
                return Result.failure(SecurityException("Incorrect password or corrupted file."))
            }

            val metaString = String(metaPlainBytes, Charsets.UTF_8)
            val metaParts = metaString.split("|")
            val originalName = metaParts.getOrElse(0) { "decrypted_file" }
            val originalExt = metaParts.getOrElse(1) { "bin" }
            val isStandalone = metaParts.getOrNull(3) == "standalone"

            val effectivePepper = if (isStandalone) "" else pepper
            chachaKey = deriveChachaKey(password, effectivePepper, salt)

            var chunkIdx = 0L
            var totalProcessed = 0L

            while (true) {
                if (isCancelled()) {
                    return Result.failure(java.util.concurrent.CancellationException("Operation cancelled by user."))
                }
                val isLastFlag = inData.readByte().toInt()
                val chunkSize = inData.readInt()

                if (chunkSize < 0 || chunkSize > 10 * 1024 * 1024) {
                    return Result.failure(SecurityException("Corrupted encrypted package stream."))
                }

                val stage3 = ByteArray(chunkSize)
                inData.readFully(stage3)

                val chunkIv = computeChunkIv(baseIv, chunkIdx)

                // Stage 3 Reverse: XOR
                val stage2 = applyXorTransformation(stage3, chachaKey, chunkIv, salt)
                // Stage 2 Reverse: ChaCha20-Poly1305
                val stage1 = decryptChaCha20Poly1305(stage2, chachaKey, chunkIv)
                // Stage 1 Reverse: AES-GCM
                val plainChunk = decryptAesGcm(stage1, aesKey, chunkIv)

                outputStream.write(plainChunk)
                totalProcessed += plainChunk.size
                chunkIdx++

                val pct = if (totalBytes > 0) (totalProcessed.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                onProgress(totalProcessed, totalBytes, pct)

                if (isLastFlag == 1) {
                    break
                }
            }

            outputStream.flush()
            val duration = System.currentTimeMillis() - startTime
            return Result.success(
                StreamingDecryptedMetadata(
                    originalFileName = originalName,
                    originalExtension = originalExt,
                    bytesProcessed = totalProcessed,
                    totalChunks = chunkIdx,
                    durationMs = duration
                )
            )
        } catch (e: Exception) {
            return Result.failure(SecurityException("Incorrect password or corrupted encrypted file data."))
        } finally {
            aesKey?.fill(0)
            chachaKey?.fill(0)
        }
    }
}
