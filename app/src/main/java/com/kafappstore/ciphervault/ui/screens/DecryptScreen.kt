package com.kafappstore.ciphervault.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.ui.components.CyberButton
import com.kafappstore.ciphervault.ui.components.CyberSecondaryButton
import com.kafappstore.ciphervault.ui.components.CyberTerminalTextField
import com.kafappstore.ciphervault.ui.components.LargeFileStreamingSection
import com.kafappstore.ciphervault.ui.theme.CyberCrimson
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.ui.theme.MatrixTextCode
import com.kafappstore.ciphervault.viewmodel.CipherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DecryptScreen(
    viewModel: CipherViewModel,
    onNavigateToEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.decryptState.collectAsState()

    // Sub-mode: 0 = Text Base64, 1 = Encrypted File Streaming (1GB+)
    var decryptSubMode by remember { mutableIntStateOf(0) }

    var showSaveToProjectDialog by remember { mutableStateOf(false) }
    var saveProjectTitle by remember { mutableStateOf("") }

    // File picker to read Base64 text file
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadTextFromFile(it, context.contentResolver, isForDecrypt = true) }
    }

    // Save decrypted output to custom file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { targetUri ->
            state.resultPlaintext?.let { plainContent ->
                viewModel.saveContentToFile(targetUri, plainContent, context.contentResolver) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Save to Project Dialog
    if (showSaveToProjectDialog) {
        val defaultTitle = "Decrypted [${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}]"
        AlertDialog(
            onDismissRequest = { showSaveToProjectDialog = false },
            title = {
                Text("Save Decrypted Text to Projects", color = CyberCyan, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Choose a title for this draft so you can edit it later in the Projects section:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (saveProjectTitle.isEmpty()) defaultTitle else saveProjectTitle,
                        onValueChange = { saveProjectTitle = it },
                        label = { Text("Project Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                CyberButton(
                    text = "Save to Database",
                    icon = Icons.Default.Folder,
                    onClick = {
                        val title = if (saveProjectTitle.isEmpty()) defaultTitle else saveProjectTitle
                        viewModel.saveDecryptedTextAsProject(title) {
                            Toast.makeText(context, "Saved to Projects.", Toast.LENGTH_SHORT).show()
                            showSaveToProjectDialog = false
                        }
                    },
                    accentColor = CyberCyan,
                    testTag = "btn_save_decrypted_project"
                )
            },
            dismissButton = {
                TextButton(onClick = { showSaveToProjectDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF091417)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Mode Selector: Text vs Large Files Streaming
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF071109))
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (decryptSubMode == 0) CyberCyan else Color.Transparent)
                    .clickable { decryptSubMode = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = if (decryptSubMode == 0) Color.Black else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Base64 String",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (decryptSubMode == 0) Color.Black else Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (decryptSubMode == 1) CyberCyan else Color.Transparent)
                    .clickable { decryptSubMode = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (decryptSubMode == 1) Color.Black else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Large File (Up to 1GB+)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (decryptSubMode == 1) Color.Black else Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (decryptSubMode == 1) {
            // Streaming Decryption for Large Files (1GB+)
            LargeFileStreamingSection(
                viewModel = viewModel,
                isDecryptionMode = true
            )
        } else {
            // Text Base64 Decryption
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Encrypted Base64 String",
                    style = MaterialTheme.typography.titleMedium.copy(color = CyberCyan)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CyberSecondaryButton(
                        text = "File",
                        icon = Icons.Default.Description,
                        onClick = {
                            openFileLauncher.launch(
                                arrayOf("text/*", "application/octet-stream", "*/*")
                            )
                        },
                        accentColor = CyberCyan,
                        testTag = "decrypt_from_file_btn"
                    )

                    CyberSecondaryButton(
                        text = "Clear",
                        icon = Icons.Default.Clear,
                        onClick = { viewModel.clearDecrypt() },
                        accentColor = CyberCrimson,
                        testTag = "decrypt_clear_btn"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            CyberTerminalTextField(
                value = state.inputBase64,
                onValueChange = { viewModel.onDecryptInputChanged(it) },
                placeholder = "Paste encrypted Base64 string here or load from file...",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 8,
                testTag = "decrypt_input_base64"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Length: ${state.inputBase64.length} chars",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF5A7864))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            Text(
                text = "Decryption Password",
                style = MaterialTheme.typography.titleMedium.copy(color = CyberCyan)
            )

            Spacer(modifier = Modifier.height(8.dp))

            CyberTerminalTextField(
                value = state.password,
                onValueChange = { viewModel.onDecryptPasswordChanged(it) },
                placeholder = "Enter decryption password...",
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleDecryptPasswordVisibility() }) {
                        Icon(
                            imageVector = if (state.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = CyberCyan
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "decrypt_password_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Big 3D "Decrypt" Button
            CyberButton(
                text = "Decrypt Now (Reverse Cascade)",
                icon = Icons.Default.LockOpen,
                onClick = { viewModel.executeDecrypt() },
                enabled = state.inputBase64.isNotBlank() && state.password.isNotEmpty() && !state.isDecrypting,
                isLoading = state.isDecrypting,
                modifier = Modifier.fillMaxWidth(),
                accentColor = CyberCyan,
                testTag = "execute_decrypt_button"
            )

            // Error message: when password is wrong
            AnimatedVisibility(visible = state.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FF3366))
                        .border(BorderStroke(1.dp, CyberCrimson), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CyberCrimson)
                    )
                }
            }

            // Success Plaintext Output Section
            state.resultPlaintext?.let { plaintext ->
                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
                    color = Color(0xFF09120C),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Decrypted Plaintext",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MatrixGreenPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "${plaintext.length} chars",
                                style = MaterialTheme.typography.labelSmall.copy(color = MatrixGreenPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF050B07))
                                .border(BorderStroke(1.dp, Color(0xFF1B3824)), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = plaintext,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = MatrixTextCode,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 1: Copy & Save File
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyberButton(
                                text = "Copy Text",
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(plaintext))
                                    Toast.makeText(context, "Text copied to clipboard.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = MatrixGreenPrimary,
                                testTag = "decrypt_copy_btn"
                            )

                            CyberButton(
                                text = "Save to File",
                                icon = Icons.Default.FileDownload,
                                onClick = {
                                    saveFileLauncher.launch("ciphervault_decrypted_${System.currentTimeMillis()}.txt")
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = CyberCyan,
                                testTag = "decrypt_save_file_btn"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Save as Project & Open in Advanced Editor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyberSecondaryButton(
                                text = "Save to Projects",
                                icon = Icons.Default.Folder,
                                onClick = { showSaveToProjectDialog = true },
                                modifier = Modifier.weight(1f),
                                accentColor = MatrixGreenPrimary,
                                testTag = "decrypt_save_project_btn"
                            )

                            CyberSecondaryButton(
                                text = "Open in Editor",
                                icon = Icons.Default.EditNote,
                                onClick = {
                                    viewModel.openDecryptedInEditor()
                                    onNavigateToEditor()
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = CyberCyan,
                                testTag = "decrypt_open_in_editor_btn"
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
