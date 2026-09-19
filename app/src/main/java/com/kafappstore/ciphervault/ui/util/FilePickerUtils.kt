package com.kafappstore.ciphervault.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher

object FilePickerUtils {

    /**
     * Launches the system file picker using the most reliable intent strategy across Android OEMs.
     * Starts with Intent.createChooser wrapping ACTION_GET_CONTENT, which works on virtually
     * all Android devices (Samsung, Xiaomi, Nokia/HMD, Google Pixel, Huawei, Motorola, etc.).
     * Falls back to direct GET_CONTENT, then ACTION_OPEN_DOCUMENT (SAF).
     *
     * @return true if an activity was successfully launched, false if no activity handled the intent.
     */
    fun launchSystemFilePicker(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        mimeType: String = "*/*"
    ): Boolean {
        val pm = context.packageManager

        // Strategy 1: Standard ACTION_GET_CONTENT with CATEGORY_OPENABLE
        try {
            val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = mimeType
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            if (getContentIntent.resolveActivity(pm) != null) {
                launcher.launch(getContentIntent)
                return true
            }
        } catch (e: Exception) {
            Log.w("FilePickerUtils", "Strategy 1 failed: ${e.message}")
        }

        // Strategy 2: Chooser with ACTION_GET_CONTENT
        try {
            val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = mimeType
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            val chooser = Intent.createChooser(getContentIntent, "Select File")
            launcher.launch(chooser)
            return true
        } catch (e: Exception) {
            Log.w("FilePickerUtils", "Strategy 2 failed: ${e.message}")
        }

        // Strategy 3: Storage Access Framework (SAF) ACTION_OPEN_DOCUMENT
        try {
            val openDocIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
            }
            if (openDocIntent.resolveActivity(pm) != null) {
                launcher.launch(openDocIntent)
                return true
            }
        } catch (e: Exception) {
            Log.w("FilePickerUtils", "Strategy 3 failed: ${e.message}")
        }

        // Strategy 4: Direct ACTION_GET_CONTENT without OPENABLE
        try {
            val bareIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = mimeType
            }
            if (bareIntent.resolveActivity(pm) != null) {
                launcher.launch(bareIntent)
                return true
            }
        } catch (e: Exception) {
            Log.w("FilePickerUtils", "Strategy 4 failed: ${e.message}")
        }

        // Strategy 5: Files by Google or Common File Managers direct launch
        val knownFileManagerPackages = listOf(
            "com.google.android.apps.nbu.files",
            "com.android.documentsui",
            "com.sec.android.app.myfiles",
            "com.mi.android.globalFileexplorer",
            "com.huawei.hidisk"
        )
        for (pkg in knownFileManagerPackages) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    val filePickerIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        `package` = pkg
                        type = mimeType
                    }
                    if (filePickerIntent.resolveActivity(pm) != null) {
                        launcher.launch(filePickerIntent)
                        return true
                    }
                }
            } catch (_: Exception) {}
        }

        return false
    }

    /**
     * Robust URI extraction from ActivityResult Intent.
     * Checks intent.data first, then fallback to intent.clipData.
     */
    fun extractUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        intent.data?.let { return it }
        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            return clipData.getItemAt(0)?.uri
        }
        return null
    }
}
