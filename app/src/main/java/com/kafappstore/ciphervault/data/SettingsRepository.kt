package com.kafappstore.ciphervault.data

import android.content.Context
import android.content.SharedPreferences
import com.kafappstore.ciphervault.crypto.CipherEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CyberThemeMode {
    MATRIX_GREEN,
    CYBER_NEON,
    DEEP_VOID
}

data class AppSettingsState(
    val pepper: String = CipherEngine.DEFAULT_PEPPER,
    val autoClearMemory: Boolean = true,
    val outputThreshold: Int = 3000,
    val themeMode: CyberThemeMode = CyberThemeMode.MATRIX_GREEN
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ciphervault_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): AppSettingsState {
        val pepper = prefs.getString(KEY_PEPPER, CipherEngine.DEFAULT_PEPPER)
            ?.takeIf { it.isNotBlank() } ?: CipherEngine.DEFAULT_PEPPER
        val autoClear = prefs.getBoolean(KEY_AUTO_CLEAR, true)
        val threshold = prefs.getInt(KEY_OUTPUT_THRESHOLD, 3000)
        val themeName = prefs.getString(KEY_THEME_MODE, CyberThemeMode.MATRIX_GREEN.name)
        val theme = try {
            CyberThemeMode.valueOf(themeName ?: CyberThemeMode.MATRIX_GREEN.name)
        } catch (_: Exception) {
            CyberThemeMode.MATRIX_GREEN
        }

        return AppSettingsState(
            pepper = pepper,
            autoClearMemory = autoClear,
            outputThreshold = threshold,
            themeMode = theme
        )
    }

    fun updatePepper(newPepper: String) {
        val valid = if (newPepper.isBlank()) CipherEngine.DEFAULT_PEPPER else newPepper
        prefs.edit().putString(KEY_PEPPER, valid).apply()
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

    companion object {
        private const val KEY_PEPPER = "key_pepper"
        private const val KEY_AUTO_CLEAR = "key_auto_clear"
        private const val KEY_OUTPUT_THRESHOLD = "key_output_threshold"
        private const val KEY_THEME_MODE = "key_theme_mode"
    }
}
