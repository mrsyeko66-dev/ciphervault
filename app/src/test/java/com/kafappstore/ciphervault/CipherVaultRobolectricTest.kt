package com.kafappstore.ciphervault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kafappstore.ciphervault.crypto.CipherEngine
import com.kafappstore.ciphervault.data.db.CipherProject
import com.kafappstore.ciphervault.data.db.CipherVaultDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CipherVaultRobolectricTest {

    @Test
    fun `test app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CipherVault", appName)
    }

    @Test
    fun `test 3-layer string encryption and decryption roundtrip`() {
        val originalText = "CLASSIFIED DATA: 0x99AABBCC // TOP SECRET"
        val password = "StrongPassword#2026"
        val pepper = "ciphervault_pepper_key_v1"

        val encryptResult = CipherEngine.encrypt(originalText, password, pepper)
        assertTrue(encryptResult.isSuccess)
        val base64Payload = encryptResult.getOrThrow()
        assertTrue(base64Payload.isNotEmpty())

        val decryptResult = CipherEngine.decrypt(base64Payload, password, pepper)
        assertTrue(decryptResult.isSuccess)
        assertEquals(originalText, decryptResult.getOrThrow())

        // Wrong password test
        val wrongResult = CipherEngine.decrypt(base64Payload, "WrongPass123456", pepper)
        assertFalse(wrongResult.isSuccess)
    }

    @Test
    fun `test stream encryption and decryption roundtrip with metadata preservation`() {
        // Test with arbitrary binary data representing a simulated large file
        val sampleData = ByteArray(512 * 1024) { (it % 256).toByte() }
        val originalFileName = "contract_archive.pdf"
        val originalExt = "pdf"
        val password = "MatrixSafe#998811"
        val pepper = "stream_pepper_test"

        val inputStream = ByteArrayInputStream(sampleData)
        val encryptedOut = ByteArrayOutputStream()

        val encryptRes = CipherEngine.encryptStream(
            inputStream = inputStream,
            outputStream = encryptedOut,
            password = password,
            pepper = pepper,
            originalFileName = originalFileName,
            originalExtension = originalExt,
            totalBytes = sampleData.size.toLong()
        )
        assertTrue(encryptRes.isSuccess)

        val encryptedBytes = encryptedOut.toByteArray()
        assertTrue(encryptedBytes.size > sampleData.size)

        // Decrypt stream
        val cipherIn = ByteArrayInputStream(encryptedBytes)
        val decryptedOut = ByteArrayOutputStream()

        val decryptRes = CipherEngine.decryptStream(
            inputStream = cipherIn,
            outputStream = decryptedOut,
            password = password,
            pepper = pepper,
            totalBytes = encryptedBytes.size.toLong()
        )
        assertTrue(decryptRes.isSuccess)
        val metadata = decryptRes.getOrThrow()

        assertEquals(originalFileName, metadata.originalFileName)
        assertEquals(originalExt, metadata.originalExtension)
        assertEquals(sampleData.size.toLong(), metadata.bytesProcessed)
        assertArrayEquals(sampleData, decryptedOut.toByteArray())
    }

    @Test
    fun `test Room database project entity and DAO operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, CipherVaultDatabase::class.java).build()
        val dao = db.projectDao()

        val project = CipherProject(
            title = "Security Report Project",
            content = "This text is drafted today and edited tomorrow.",
            tag = "Confidential Report"
        )
        val insertedId = dao.insertProject(project)
        assertTrue(insertedId > 0)

        val fetched = dao.getProjectById(insertedId)
        assertEquals("Security Report Project", fetched?.title)
        assertEquals("Confidential Report", fetched?.tag)

        val updated = fetched!!.copy(content = "Updated content with new information")
        dao.updateProject(updated)

        val reFetched = dao.getProjectById(insertedId)
        assertEquals("Updated content with new information", reFetched?.content)

        dao.deleteProject(reFetched!!)
        val afterDelete = dao.getProjectById(insertedId)
        assertEquals(null, afterDelete)

        db.close()
    }

    @Test
    fun `test splash screen procedural glyph themes and non-repetitive loading bar`() {
        val generatedConfigs = mutableSetOf<String>()
        val generatedGlyphs = mutableSetOf<String>()

        // Generate multiple random configs to verify variety and non-repetition
        repeat(20) {
            val config = com.kafappstore.ciphervault.ui.screens.GlyphThemes.generateRandomConfig()
            generatedConfigs.add(config.name)
            generatedGlyphs.add(config.fillGlyph)

            // Test bracket enclosure
            assertTrue(config.openBracket.isNotEmpty())
            assertTrue(config.closeBracket.isNotEmpty())

            // Test that NO Persian or Arabic characters exist in the glyph pool
            val isArabicOrPersian = config.fillGlyph.any { c ->
                c in '\u0600'..'\u06FF' || c in '\uFB50'..'\uFDFF' || c in '\uFE70'..'\uFEFF'
            }
            assertFalse("Loading glyph should not be Persian/Arabic", isArabicOrPersian)

            // Simulate progress bar rendering
            val filled = config.fillGlyph.repeat(10)
            val unfilled = config.unfilledGlyph.repeat(10)
            val bar = "${config.openBracket}$filled$unfilled${config.closeBracket}"
            assertTrue(bar.startsWith(config.openBracket))
            assertTrue(bar.endsWith(config.closeBracket))
        }

        // Verify that randomness produces variety across 20 iterations
        assertTrue(generatedGlyphs.size > 1)
    }

    @Test
    fun `test splash screen terminal boot scenarios generation`() {
        val logs = com.kafappstore.ciphervault.ui.screens.BootScenarios.generateScenarioLogs()
        assertTrue(logs.isNotEmpty())
        assertTrue(logs.size >= 5)

        val prompt = com.kafappstore.ciphervault.ui.screens.BootScenarios.getRandomPrompt()
        assertTrue(prompt.contains("# ./"))
    }
}
