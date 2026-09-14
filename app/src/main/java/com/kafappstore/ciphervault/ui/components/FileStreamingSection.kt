package com.kafappstore.ciphervault.ui.components

import android.net.Uri
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDecryptionMode) "Streaming Large File Decryption (Up to 1GB+)" else "Streaming Large File Encryption (Up to 1GB+)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    CyberSecondaryButton(
                        text = "Format Guide",
                        icon = Icons.Default.HelpOutline,
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
                            text = "Select File from Device",
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
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isDecryptionMode) CyberCyan else MatrixGreenPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = streamingState.selectedFileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Size: ${viewModel.formatFileSize(streamingState.selectedFileSize)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MatrixTextCode,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        CyberSecondaryButton(
                            text = "Change File",
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
                        text = "Encryption Password (12 to 22 characters):",
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
                        placeholder = { Text("Password (12-22 chars)...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isDecryptionMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordStrengthMeter(password = streamingState.password)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            val isEnabled = !streamingState.isStreaming &&
                    (if (isDecryptionMode) streamingState.password.isNotEmpty() else streamingState.password.length in 12..22)

            CyberButton(
                text = if (isDecryptionMode) "Start File Decryption & Save" else "Start Streaming Encryption & Save",
                icon = if (isDecryptionMode) Icons.Default.LockOpen else Icons.Default.Lock,
                onClick = {
                    val defaultName = if (isDecryptionMode) {
                        "decrypted_${streamingState.selectedFileName.removeSuffix(".cvault").removeSuffix(".cenc").removeSuffix(".enc")}"
                    } else {
                        val baseName = streamingState.selectedFileName.substringBeforeLast('.', "file")
                        val outExt = viewModel.getEffectiveOutputExtension()
                        "${baseName}_encrypted.$outExt"
                    }
                    saveTargetLauncher.launch(defaultName)
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
        }
    }
}

@Composable
fun FileFormatsGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
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
            TextButton(onClick = onDismiss) {
                Text("Got It", color = MatrixGreenPrimary)
            }
        },
        containerColor = Color(0xFF0C1710)
    )
}
