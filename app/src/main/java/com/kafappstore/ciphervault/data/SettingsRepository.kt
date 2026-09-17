package com.kafappstore.ciphervault.data

import android.content.Context
import android.content.SharedPreferences
import com.kafappstore.ciphervault.crypto.AndroidKeyStoreVault
import com.kafappstore.ciphervault.crypto.CipherEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CyberThemeMode {
    MATRIX_GREEN,
    CYBER_NEON,
    DEEP_VOID
}

enum class AppLockType {
    BIOMETRIC,
    PIN_NUMERIC,
    PASSWORD_ALPHANUMERIC
}

data class AppSettingsState(
    val pepper: String = CipherEngine.DEFAULT_PEPPER,
    val autoClearMemory: Boolean = true,
    val outputThreshold: Int = 3000,
    val themeMode: CyberThemeMode = CyberThemeMode.MATRIX_GREEN,
    val biometricLockEnabled: Boolean = false,
    val lockType: AppLockType = AppLockType.BIOMETRIC,
    val hasCustomPasscode: Boolean = false,
    val screenSecurityEnabled: Boolean = true
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ciphervault_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): AppSettingsState {
        // Retrieve pepper: check for encrypted pepper first, fallback to legacy plaintext if any, then encrypt it
        val encryptedPepper = prefs.getString(KEY_PEPPER_ENCRYPTED, null)
        val pepper = if (!encryptedPepper.isNullOrBlank()) {
            val decrypted = AndroidKeyStoreVault.decrypt(encryptedPepper)
            if (!decrypted.isNullOrBlank()) decrypted else CipherEngine.DEFAULT_PEPPER
        } else {
            // Check legacy plain pepper
            val legacyPepper = prefs.getString(KEY_PEPPER, null)
            if (!legacyPepper.isNullOrBlank()) {
                // Migrate to KeyStore encryption
                try {
                    val enc = AndroidKeyStoreVault.encrypt(legacyPepper)
                    prefs.edit()
                        .putString(KEY_PEPPER_ENCRYPTED, enc)
                        .remove(KEY_PEPPER)
                        .apply()
                } catch (_: Exception) {}
                legacyPepper
            } else {
                // Save default pepper encrypted
                try {
                    val enc = AndroidKeyStoreVault.encrypt(CipherEngine.DEFAULT_PEPPER)
                    prefs.edit().putString(KEY_PEPPER_ENCRYPTED, enc).apply()
                } catch (_: Exception) {}
                CipherEngine.DEFAULT_PEPPER
            }
        }

        val autoClear = prefs.getBoolean(KEY_AUTO_CLEAR, true)
        val threshold = prefs.getInt(KEY_OUTPUT_THRESHOLD, 3000)
        val themeName = prefs.getString(KEY_THEME_MODE, CyberThemeMode.MATRIX_GREEN.name)
        val biometricLock = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
        val lockTypeName = prefs.getString(KEY_LOCK_TYPE, AppLockType.BIOMETRIC.name)
        val hasCustomPasscode = !prefs.getString(KEY_PASSCODE_HASH, null).isNullOrBlank()
        val screenSecurity = prefs.getBoolean(KEY_SCREEN_SECURITY, true)

        val theme = try {
            CyberThemeMode.valueOf(themeName ?: CyberThemeMode.MATRIX_GREEN.name)
        } catch (_: Exception) {
            CyberThemeMode.MATRIX_GREEN
        }

        val lockType = try {
            AppLockType.valueOf(lockTypeName ?: AppLockType.BIOMETRIC.name)
        } catch (_: Exception) {
            AppLockType.BIOMETRIC
        }

        return AppSettingsState(
            pepper = pepper,
            autoClearMemory = autoClear,
            outputThreshold = threshold,
            themeMode = theme,
            biometricLockEnabled = biometricLock,
            lockType = lockType,
            hasCustomPasscode = hasCustomPasscode,
            screenSecurityEnabled = screenSecurity
        )
    }

    fun updatePepper(newPepper: String) {
        val valid = if (newPepper.isBlank()) CipherEngine.DEFAULT_PEPPER else newPepper
        try {
            val enc = AndroidKeyStoreVault.encrypt(valid)
            prefs.edit().putString(KEY_PEPPER_ENCRYPTED, enc).remove(KEY_PEPPER).apply()
        } catch (_: Exception) {
            // In testing environments without AndroidKeyStore provider, fallback gracefully
            prefs.edit().putString(KEY_PEPPER, valid).apply()
        }
        _settings.value = _settings.value.copy(pepper = valid)
    }

    fun resetPepperToDefault() {
        updatePepper(CipherEngine.DEFAULT_PEPPER)
    }

    fun setAutoClearMemory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLEAR, enabled).apply()
        _settings.value = _settings.value.copy(autoClearMemory = enabled)
    }

    fun setOutputThreshold(threshold: Int) {
        val clamped = threshold.coerceIn(1000, 10000)
        prefs.edit().putInt(KEY_OUTPUT_THRESHOLD, clamped).apply()
        _settings.value = _settings.value.copy(outputThreshold = clamped)
    }

    fun setThemeMode(mode: CyberThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
        _settings.value = _settings.value.copy(biometricLockEnabled = enabled)
    }

    fun setLockType(type: AppLockType) {
        prefs.edit().putString(KEY_LOCK_TYPE, type.name).apply()
        _settings.value = _settings.value.copy(lockType = type)
    }

    fun savePasscode(passcode: String) {
        val hash = com.kafappstore.ciphervault.crypto.AppLockAuthManager.hashPasscode(passcode)
        prefs.edit().putString(KEY_PASSCODE_HASH, hash).apply()
        _settings.value = _settings.value.copy(hasCustomPasscode = true)
    }

    fun verifyPasscode(input: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSCODE_HASH, null) ?: return false
        return com.kafappstore.ciphervault.crypto.AppLockAuthManager.verifyPasscode(input, storedHash)
    }

    fun clearPasscode() {
        prefs.edit().remove(KEY_PASSCODE_HASH).apply()
        _settings.value = _settings.value.copy(hasCustomPasscode = false)
    }

    fun setScreenSecurityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREEN_SECURITY, enabled).apply()
        _settings.value = _settings.value.copy(screenSecurityEnabled = enabled)
    }

    companion object {
        private const val KEY_PEPPER_ENCRYPTED = "key_pepper_encrypted"
        private const val KEY_PEPPER = "key_pepper"
        private const val KEY_AUTO_CLEAR = "key_auto_clear"
        private const val KEY_OUTPUT_THRESHOLD = "key_output_threshold"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_BIOMETRIC_LOCK = "key_biometric_lock"
        private const val KEY_LOCK_TYPE = "key_lock_type"
        private const val KEY_PASSCODE_HASH = "key_passcode_hash"
        private const val KEY_SCREEN_SECURITY = "key_screen_security"
    }
}
