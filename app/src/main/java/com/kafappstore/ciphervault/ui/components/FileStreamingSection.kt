package com.kafappstore.ciphervault.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.kafappstore.ciphervault.crypto.CipherEngine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCrimson
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.ui.theme.MatrixTextCode
import com.kafappstore.ciphervault.viewmodel.CipherViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LargeFileStreamingSection(
    viewModel: CipherViewModel,
    isDecryptionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val streamingState by viewModel.streamingState.collectAsState()
    var showFormatsGuideDialog by remember { mutableStateOf(false) }

    // File picker for any file type (documents, media, archives, binaries)
    val openAnyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val (name, size) = viewModel.queryFileInfo(it, context.contentResolver)
            viewModel.selectFileForStreaming(it, name, size, isDecryption = isDecryptionMode)
        }
    }

    // Save encrypted or decrypted file target
    val saveTargetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri: Uri? ->
        targetUri?.let {
            if (isDecryptionMode) {
                viewModel.startStreamingDecryption(it, context.contentResolver)
            } else {
                viewModel.startStreamingEncryption(it, context.contentResolver)
            }
        }
    }

    // Formats & Guidance Dialog
    if (showFormatsGuideDialog) {
        FileFormatsGuideDialog(onDismiss = { showFormatsGuideDialog = false })
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // Guidance & Format Specs Card
        Surface(
            color = Color(0xFF09140C),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MatrixBorderNeon),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDecryptionMode) "Streaming Decryption" else "Streaming Encryption",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    CyberSecondaryButton(
                        text = "Guide",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        onClick = { showFormatsGuideDialog = true },
                        accentColor = CyberAmber,
                        testTag = "btn_formats_guide"
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Chunked streaming with fixed memory usage (~256 KB RAM). Ideal for large files such as video, PDF, images, databases, and archives without OOM risk.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF8BAA94),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected File Display or Picker Button
        Surface(
            color = Color(0xFF070F0A),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (streamingState.selectedFileUri != null) MatrixGreenPrimary else MatrixBorderNeon),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (streamingState.selectedFileUri == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isDecryptionMode) "Select encrypted file (.cvault or any file)" else "Select file to encrypt (any type, up to 1GB+)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CyberButton(
                            text = "Select File",
                            icon = Icons.Default.FolderOpen,
                            onClick = {
                                openAnyFileLauncher.launch(arrayOf("*/*"))
                            },
                            accentColor = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                            testTag = "btn_select_streaming_file"
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = streamingState.selectedFileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Size: ${viewModel.formatFileSize(streamingState.selectedFileSize)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MatrixTextCode,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        CyberSecondaryButton(
                            text = "Change",
                            icon = Icons.Default.FolderOpen,
                            onClick = { openAnyFileLauncher.launch(arrayOf("*/*")) },
                            accentColor = Color.LightGray,
                            testTag = "btn_change_streaming_file"
                        )
                    }
                }
            }
        }

        // Encryption Options: Output Extension Selector (Only for Encryption Mode)
        if (!isDecryptionMode && streamingState.selectedFileUri != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFF09140C),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MatrixBorderNeon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Output File Format & Extension:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MatrixGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            Pair("cvault", "Dedicated Container (.cvault) [Recommended]"),
                            Pair("cenc", "Secure Package (.cenc)"),
                            Pair("enc", "Standard Encrypted (.enc)"),
                            Pair("custom", "Custom Extension...")
                        )

                        options.forEach { (extKey, label) ->
                            val isSelected = streamingState.selectedOutputExtension == extKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) MatrixGreenPrimary else Color(0xFF2A3D2F)
                                        ),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .background(if (isSelected) Color(0xFF13331C) else Color(0xFF0A160E))
                                    .clickable { viewModel.setStreamingOutputExtension(extKey) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) MatrixGreenPrimary else Color.LightGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }

                    if (streamingState.selectedOutputExtension == "custom") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = streamingState.customOutputExtension,
                            onValueChange = { viewModel.setCustomOutputExtension(it) },
                            singleLine = true,
                            placeholder = { Text("e.g. secure, lock, bin...") },
                            label = { Text("Desired Output Extension") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Password Input Section
        if (streamingState.selectedFileUri != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFF09140C),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MatrixBorderNeon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isDecryptionMode) {
                            "Decryption Password:"
                        } else if (!streamingState.useSecretKey) {
                            "Standalone Password (Requires Very Strong Password):"
                        } else {
                            "Encryption Password (12 to 64 characters):"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = streamingState.password,
                        onValueChange = { viewModel.onStreamingPasswordChanged(it) },
                        singleLine = true,
                        visualTransformation = if (streamingState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleStreamingPasswordVisibility() }) {
                                Icon(
                                    imageVector = if (streamingState.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        placeholder = {
                            Text(
                                if (isDecryptionMode) "Enter file password..."
                                else if (!streamingState.useSecretKey) "حداقل ۱۴ کاراکتر با حروف بزرگ و کوچک، عدد و نماد..."
                                else "Password (12-64 chars)..."
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Whitespace warning & trim for file streaming password
                    PasswordWhitespaceWarning(
                        password = streamingState.password,
                        onTrimPassword = { viewModel.onStreamingPasswordChanged(streamingState.password.trim()) }
                    )

                    if (!isDecryptionMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordStrengthMeter(password = streamingState.password)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Standalone Mode (No Secret Key) Switch Card
                        Surface(
                            color = if (!streamingState.useSecretKey) Color(0xFF14241B) else Color(0xFF09140C),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (!streamingState.useSecretKey) MatrixGreenPrimary else Color(0xFF1B3B24)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = if (!streamingState.useSecretKey) MatrixGreenPrimary else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "رمزنگاری بدون کلید مخفی (حالت مستقل)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (!streamingState.useSecretKey) MatrixGreenPrimary else Color.LightGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (!streamingState.useSecretKey)
                                            "فایل فقط با این رمز عبور باز می‌شود (فاقد وابستگی به کلید مخفی برنامه). نیازمند پسورد بسیار قوی."
                                        else
                                            "استفاده از کلید مخفی برنامه (پیش‌فرض با حداکثر امنیت)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 10.sp)
                                    )
                                }
                                Switch(
                                    checked = !streamingState.useSecretKey,
                                    onCheckedChange = { isStandalone ->
                                        viewModel.toggleStreamingUseSecretKey(!isStandalone)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MatrixGreenPrimary,
                                        checkedTrackColor = Color(0xFF134E27),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF101C14)
                                    )
                                )
                            }
                        }

                        // Strong Password Criteria Checklist when in Standalone mode
                        if (!streamingState.useSecretKey) {
                            val pw = streamingState.password
                            val cLength = pw.length >= 14
                            val cUpper = pw.any { it.isUpperCase() }
                            val cLower = pw.any { it.isLowerCase() }
                            val cDigit = pw.any { it.isDigit() }
                            val cSpecial = pw.any { !it.isLetterOrDigit() }

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF07140B),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1C3A24)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "الزامات پسورد قوی در حالت بدون کلید مخفی:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MatrixGreenPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    StrengthCheckItem(label = "حداقل ۱۴ کاراکتر (${pw.length}/14)", passed = cLength)
                                    StrengthCheckItem(label = "شامل حروف بزرگ انگلیسی (A-Z)", passed = cUpper)
                                    StrengthCheckItem(label = "شامل حروف کوچک انگلیسی (a-z)", passed = cLower)
                                    StrengthCheckItem(label = "شامل حداقل یک رقم عدد (0-9)", passed = cDigit)
                                    StrengthCheckItem(label = "شامل حداقل یک نماد خاص (@#\$%...)", passed = cSpecial)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            val strength = remember(streamingState.password) { CipherEngine.evaluatePasswordStrength(streamingState.password) }
            val isEnabled = !streamingState.isStreaming && when {
                isDecryptionMode -> streamingState.password.isNotEmpty()
                !streamingState.useSecretKey -> strength.first
                else -> streamingState.password.length in 12..64
            }

            CyberButton(
                text = if (isDecryptionMode) "Decrypt File & Save" else "Encrypt File & Save",
                icon = if (isDecryptionMode) Icons.Default.LockOpen else Icons.Default.Lock,
                onClick = {
                    if (isDecryptionMode) {
                        val peek = viewModel.inspectEncryptedFileForDecryption(context.contentResolver)
                        if (peek.isFailure) {
                            val ex = peek.exceptionOrNull()
                            val msg = if (ex is SecurityException || ex is IllegalArgumentException) {
                                "رمز عبور اشتباه است یا فرمت فایل رمزنگاری شده نامعتبر است."
                            } else {
                                ex?.message ?: "خطا در بررسی فایل رمزنگاری شده"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            return@CyberButton
                        }
                        val meta = peek.getOrThrow()
                        val origName = meta.originalFileName.ifBlank { "decrypted_file" }
                        val origExt = meta.originalExtension.ifBlank { "bin" }
                        val finalName = if (origName.contains(".")) origName else "$origName.$origExt"
                        saveTargetLauncher.launch(finalName)
                    } else {
                        val baseName = streamingState.selectedFileName.substringBeforeLast('.', "file")
                        val outExt = viewModel.getEffectiveOutputExtension()
                        saveTargetLauncher.launch("${baseName}_encrypted.$outExt")
                    }
                },
                enabled = isEnabled,
                accentColor = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_start_streaming"
            )
        }

        // Live Streaming Progress Bar & Status
        AnimatedVisibility(visible = streamingState.isStreaming) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = Color(0xFF061109),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, if (isDecryptionMode) CyberCyan else MatrixGreenPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDecryptionMode) "Decrypting byte stream..." else "Cascading stream encryption...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", streamingState.progressPercent * 100f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Key Derivation Notice during initial phase
                    if (streamingState.bytesProcessed == 0L) {
                        KeyDerivationIndicator(
                            isDeriving = true,
                            accentColor = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { streamingState.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                        trackColor = Color(0xFF1B2E21)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${viewModel.formatFileSize(streamingState.bytesProcessed)} of ${viewModel.formatFileSize(streamingState.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "Speed: %.1f MB/s", streamingState.speedMBs),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MatrixGreenPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Error Banner
        streamingState.errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0x33FF0055),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CyberCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = CyberCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                    )
                }
            }
        }

        // Success Banner
        streamingState.successMessage?.let { succ ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0x2200FF41),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MatrixGreenPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MatrixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = succ,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MatrixGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (isDecryptionMode && streamingState.decryptResult != null && streamingState.lastSavedTargetUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberButton(
                        text = "باز کردن فایل",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            val savedUri = streamingState.lastSavedTargetUri ?: return@CyberButton
                            val ext = streamingState.decryptResult?.originalExtension?.lowercase(Locale.ROOT) ?: "bin"
                            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(savedUri, mime)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Open File"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "برنامه‌ای برای باز کردن این فایل یافت نشد", Toast.LENGTH_SHORT).show()
                            }
                        },
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    CyberButton(
                        text = "اشتراک‌گذاری",
                        icon = Icons.Default.Share,
                        onClick = {
                            val savedUri = streamingState.lastSavedTargetUri ?: return@CyberButton
                            val ext = streamingState.decryptResult?.originalExtension?.lowercase(Locale.ROOT) ?: "bin"
                            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = mime
                                putExtra(Intent.EXTRA_STREAM, savedUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Share File"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در اشتراک‌گذاری فایل", Toast.LENGTH_SHORT).show()
                            }
                        },
                        accentColor = MatrixGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StrengthCheckItem(label: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (passed) MatrixGreenPrimary else Color(0xFF888888),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (passed) MatrixGreenPrimary else Color(0xFFAAAAAA),
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun FileFormatsGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = CyberAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "File Formats & Extensions Guide",
                    style = MaterialTheme.typography.titleMedium.copy(color = CyberAmber)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "1. Supported Input Files:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MatrixGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Thanks to the chunked streaming architecture, the system can process any file type and extension up to 1GB and beyond:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Documents & Text: PDF, DOCX, TXT, CSV, JSON, MD, LOG\n" +
                            "• Archives: ZIP, RAR, 7Z, TAR, GZ\n" +
                            "• Multimedia: MP4, MKV, AVI, MP3, WAV, JPG, PNG\n" +
                            "• Databases & Binaries: DB, SQLITE, BIN, ISO, APK",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MatrixTextCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "2. Output Extension Options:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• .cvault (Recommended): Proprietary ultra-fast container format with metadata header for automatic restoration of original name and extension upon decryption.\n" +
                            "• .cenc: Standard secure package extension.\n" +
                            "• .enc: Classic encrypted file extension.\n" +
                            "• Custom: Specify any file extension in the custom field.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        },
        confirmButton = {
            CyberButton(
                text = "Got It",
                icon = Icons.Default.CheckCircle,
                onClick = onDismiss,
                accentColor = MatrixGreenPrimary,
                depth = 3.5.dp
            )
        },
        containerColor = Color(0xFF0C1710)
    )
}
