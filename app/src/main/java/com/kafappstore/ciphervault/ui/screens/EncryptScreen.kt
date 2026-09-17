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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.crypto.CipherEngine
import com.kafappstore.ciphervault.util.SecureClipboardHelper
import com.kafappstore.ciphervault.ui.components.ConditionalOutputSection
import com.kafappstore.ciphervault.ui.components.Cyber3DTab
import com.kafappstore.ciphervault.ui.components.CyberButton
import com.kafappstore.ciphervault.ui.components.CyberSecondaryButton
import com.kafappstore.ciphervault.ui.components.CyberTerminalTextField
import com.kafappstore.ciphervault.ui.components.KeyDerivationIndicator
import com.kafappstore.ciphervault.ui.components.LargeFileStreamingSection
import com.kafappstore.ciphervault.ui.components.PasswordStrengthMeter
import com.kafappstore.ciphervault.ui.components.PasswordWhitespaceWarning
import com.kafappstore.ciphervault.ui.components.StrengthCheckItem
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCrimson
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.viewmodel.CipherViewModel

@Composable
fun EncryptScreen(
    viewModel: CipherViewModel,
    onNavigateToEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.encryptState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Sub-mode: 0 = Text & Drafts, 1 = Large Files Streaming (1GB+)
    var encryptSubMode by remember { mutableIntStateOf(0) }

    val urlDialogVisible by viewModel.urlDialogVisible.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val isDownloadingUrl by viewModel.isDownloadingUrl.collectAsState()
    val urlError by viewModel.urlErrorMessage.collectAsState()

    // File Open Launcher for Text
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadTextFromFile(it, context.contentResolver, isForDecrypt = false) }
    }

    // Save File Launcher for Output
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { targetUri ->
            state.resultBase64?.let { base64Content ->
                viewModel.saveContentToFile(targetUri, base64Content, context.contentResolver) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // URL Dialog
    if (urlDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.closeUrlDialog() },
            title = {
                Text(text = "Fetch Text from Direct URL", color = CyberCyan)
            },
            text = {
                Column {
                    Text(
                        text = "Enter direct URL to raw text file (Raw URL):",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.onUrlInputChanged(it) },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (urlError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = urlError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberCrimson)
                        )
                    }
                }
            },
            confirmButton = {
                CyberButton(
                    text = "Fetch Text",
                    icon = Icons.Default.CloudDownload,
                    onClick = { viewModel.downloadUrlContent() },
                    enabled = !isDownloadingUrl,
                    isLoading = isDownloadingUrl,
                    accentColor = CyberCyan,
                    depth = 3.5.dp
                )
            },
            dismissButton = {
                CyberSecondaryButton(
                    text = "Cancel",
                    icon = Icons.Default.Close,
                    onClick = { viewModel.closeUrlDialog() },
                    enabled = !isDownloadingUrl,
                    accentColor = Color.LightGray
                )
            },
            containerColor = Color(0xFF0F1A14),
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Mode Selector: Text vs Large Files Streaming (Duolingo 3D Tabs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Cyber3DTab(
                text = "Text & Drafts",
                icon = Icons.Default.TextFields,
                selected = encryptSubMode == 0,
                onClick = { encryptSubMode = 0 },
                modifier = Modifier.weight(1f)
            )

            Cyber3DTab(
                text = "Large File (1GB+)",
                icon = Icons.Default.Speed,
                selected = encryptSubMode == 1,
                onClick = { encryptSubMode = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (encryptSubMode == 1) {
            // Streaming Mode for files up to 1GB+
            LargeFileStreamingSection(
                viewModel = viewModel,
                isDecryptionMode = false
            )
        } else {
            // Text & Draft Encryption Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Data Input for Encryption",
                    style = MaterialTheme.typography.titleMedium.copy(color = MatrixGreenPrimary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Utility Action Buttons - Responsive 3D row where each button has equal weight and never cramps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CyberSecondaryButton(
                        text = "Editor",
                        icon = Icons.Default.EditNote,
                        onClick = onNavigateToEditor,
                        accentColor = MatrixGreenPrimary,
                        modifier = Modifier.weight(1f),
                        testTag = "encrypt_open_editor_btn"
                    )

                    CyberSecondaryButton(
                        text = "File",
                        icon = Icons.Default.Description,
                        onClick = {
                            openFileLauncher.launch(
                                arrayOf("text/*", "application/json", "application/xml", "application/javascript", "*/*")
                            )
                        },
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "encrypt_from_file_btn"
                    )

                    CyberSecondaryButton(
                        text = "URL",
                        icon = Icons.Default.CloudDownload,
                        onClick = { viewModel.openUrlDialog() },
                        accentColor = CyberAmber,
                        modifier = Modifier.weight(1f),
                        testTag = "encrypt_from_url_btn"
                    )

                    CyberSecondaryButton(
                        text = "Clear",
                        icon = Icons.Default.Clear,
                        onClick = { viewModel.clearEncrypt() },
                        accentColor = CyberCrimson,
                        modifier = Modifier.weight(1f),
                        testTag = "encrypt_clear_btn"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Text Input Field
            CyberTerminalTextField(
                value = state.inputText,
                onValueChange = { viewModel.onEncryptInputChanged(it) },
                placeholder = "Type your text, load from the advanced editor, or open a text file / URL...",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 8,
                testTag = "encrypt_input_text"
            )

            // Character count helper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Length: ${state.inputText.length} chars",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF5A7864))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Section
            Text(
                text = if (!state.useSecretKey) "Set Standalone Password (Very Strong Required)" else "Set Encryption Password (12 to 64 chars)",
                style = MaterialTheme.typography.titleMedium.copy(color = MatrixGreenPrimary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            CyberTerminalTextField(
                value = state.password,
                onValueChange = { viewModel.onEncryptPasswordChanged(it) },
                placeholder = if (!state.useSecretKey) "Min 14 chars with upper, lower, digit & symbol..." else "Enter encryption password...",
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleEncryptPasswordVisibility() }) {
                        Icon(
                            imageVector = if (state.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = MatrixGreenPrimary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "encrypt_password_input"
            )

            // Password whitespace warning & trim
            PasswordWhitespaceWarning(
                password = state.password,
                onTrimPassword = { viewModel.onEncryptPasswordChanged(state.password.trim()) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password Strength Bar
            PasswordStrengthMeter(password = state.password)

            Spacer(modifier = Modifier.height(10.dp))

            // Standalone Mode (No Secret Key) Switch Card
            Surface(
                color = if (!state.useSecretKey) Color(0xFF14241B) else Color(0xFF09140C),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (!state.useSecretKey) MatrixGreenPrimary else Color(0xFF1B3B24)),
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
                                tint = if (!state.useSecretKey) MatrixGreenPrimary else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Encrypt without Secret Key (Standalone Mode)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (!state.useSecretKey) MatrixGreenPrimary else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!state.useSecretKey)
                                "Data decrypts using password only (no dependency on app's secret pepper). Requires very strong password."
                            else
                                "Combined with app's secret key (Default with maximum protection)",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 10.sp)
                        )
                    }
                    Switch(
                        checked = !state.useSecretKey,
                        onCheckedChange = { isStandalone ->
                            viewModel.toggleEncryptUseSecretKey(!isStandalone)
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
            if (!state.useSecretKey) {
                val pw = state.password
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
                            text = "Strong Password Requirements (Standalone Mode):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MatrixGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        StrengthCheckItem(label = "At least 14 characters (${pw.length}/14)", passed = cLength)
                        StrengthCheckItem(label = "Uppercase letter (A-Z)", passed = cUpper)
                        StrengthCheckItem(label = "Lowercase letter (a-z)", passed = cLower)
                        StrengthCheckItem(label = "At least one digit (0-9)", passed = cDigit)
                        StrengthCheckItem(label = "At least one special symbol (@#\$%...)", passed = cSpecial)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big 3D Tactile "Encrypt" Button
            val strength = remember(state.password) { CipherEngine.evaluatePasswordStrength(state.password) }
            val isEncryptEnabled = state.inputText.isNotBlank() && !state.isEncrypting && (
                if (!state.useSecretKey) strength.first else state.password.length in 12..64
            )

            CyberButton(
                text = if (!state.useSecretKey) "Encrypt (Standalone Mode)" else "Encrypt Now (3-Layer Cascade)",
                icon = Icons.Default.Lock,
                onClick = { viewModel.executeEncrypt() },
                enabled = isEncryptEnabled,
                isLoading = state.isEncrypting,
                loadingText = "Deriving 600k-iteration PBKDF2 keys...",
                modifier = Modifier.fillMaxWidth(),
                accentColor = MatrixGreenPrimary,
                testTag = "execute_encrypt_button"
            )

            // Key Derivation Indicator
            KeyDerivationIndicator(
                isDeriving = state.isEncrypting,
                accentColor = MatrixGreenPrimary
            )

            // Error message if any
            AnimatedVisibility(visible = state.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FF3366))
                        .border(BorderStroke(1.dp, CyberCrimson), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = CyberCrimson)
                    )
                }
            }

            // Success message if any
            AnimatedVisibility(visible = state.successMessage != null && state.resultBase64 == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2200FF41))
                        .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = state.successMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = MatrixGreenPrimary)
                    )
                }
            }

            // Output Result Section (Conditional logic strictly enforced)
            state.resultBase64?.let { base64Output ->
                Spacer(modifier = Modifier.height(20.dp))
                ConditionalOutputSection(
                    output = base64Output,
                    threshold = settings.outputThreshold,
                    onCopy = {
                        SecureClipboardHelper.copyToClipboard(
                            context = context,
                            label = "CipherVault Encrypted Output",
                            text = base64Output,
                            isSensitive = true,
                            autoClearSeconds = 45L
                        )
                    },
                    onSaveToFile = {
                        saveFileLauncher.launch("ciphervault_encrypted_${System.currentTimeMillis()}.txt")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
