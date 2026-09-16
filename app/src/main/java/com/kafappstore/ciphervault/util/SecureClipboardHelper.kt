package com.kafappstore.ciphervault.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Secure Clipboard Manager:
 * - Copies sensitive text to system clipboard.
 * - Flags clipboard data with ClipDescription.EXTRA_IS_SENSITIVE on Android 13+ (API 33+)
 *   to suppress visual previews in keyboard history / system overlay.
 * - Automatically purges clipboard content after a designated timeout (default 45 seconds).
 */
object SecureClipboardHelper {

    private var autoClearJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        isSensitive: Boolean = true,
        autoClearSeconds: Long = 45L,
        onSuccessMessage: String? = null
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

        val clip = ClipData.newPlainText(label, text)

        // Flag as sensitive on Android 13+ (API 33+) to hide from system clipboard previews
        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboard.setPrimaryClip(clip)

        val message = onSuccessMessage ?: if (isSensitive) {
            "کپی شد (پاکسازی خودکار در $autoClearSeconds ثانیه)"
        } else {
            "کپی شد"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        if (isSensitive && autoClearSeconds > 0) {
            autoClearJob?.cancel()
            autoClearJob = scope.launch {
                delay(autoClearSeconds * 1000L)
                try {
                    // Check if clipboard still holds this item before wiping
                    val currentPrimaryClip = clipboard.primaryClip
                    if (currentPrimaryClip != null && currentPrimaryClip.itemCount > 0) {
                        val currentText = currentPrimaryClip.getItemAt(0).text?.toString()
                        if (currentText == text) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                clipboard.clearPrimaryClip()
                            } else {
                                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                            }
                            Toast.makeText(context, "کلیپ‌بورد برای حفظ امنیت پاکسازی شد.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (_: Exception) {
                    // Ignore background clipboard access restrictions
                }
            }
        }
    }
}
