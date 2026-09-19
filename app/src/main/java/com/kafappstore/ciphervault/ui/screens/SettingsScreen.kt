package com.kafappstore.ciphervault.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import com.kafappstore.ciphervault.data.AppLockType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.crypto.CipherEngine
import com.kafappstore.ciphervault.util.SecureClipboardHelper
import com.kafappstore.ciphervault.data.CyberThemeMode
import com.kafappstore.ciphervault.ui.components.CyberButton
import com.kafappstore.ciphervault.ui.components.CyberSecondaryButton
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kafappstore.ciphervault.util.StoragePermissionHelper
import com.kafappstore.ciphervault.viewmodel.CipherViewModel

@Composable
fun SettingsScreen(
    viewModel: CipherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val settings by viewModel.settings.collectAsState()

    var showPepperDialog by remember { mutableStateOf(false) }
    var editedPepper by remember(settings.pepper) { mutableStateOf(settings.pepper) }

    // Storage permission state & auto-refresh when resuming from system settings
    var hasStorageAccess by remember {
        mutableStateOf(StoragePermissionHelper.hasStoragePermission(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStorageAccess = StoragePermissionHelper.hasStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Passcode Configuration Dialog State
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var pendingLockType by remember { mutableStateOf(AppLockType.PIN_NUMERIC) }
    var newPasscodeInput by remember { mutableStateOf("") }
    var confirmPasscodeInput by remember { mutableStateOf("") }
    var passcodeDialogError by remember { mutableStateOf<String?>(null) }

    // Pepper Edit Dialog
    if (showPepperDialog) {
        AlertDialog(
            onDismissRequest = { showPepperDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = CyberAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Custom Secret Key (Pepper)",
                        style = MaterialTheme.typography.titleMedium.copy(color = CyberAmber)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "You can paste your previous secret key or set a custom one (e.g. 32 alphanumeric characters that you memorize).\nThis key is combined with the password; to decrypt files, this key must exactly match the one used during encryption.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, lineHeight = 19.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedPepper,
                        onValueChange = { editedPepper = it },
                        singleLine = false,
                        maxLines = 3,
                        label = { Text("Secret Key or Custom Pepper") },
                        placeholder = { Text("Example: MySecretCustomKeyForFiles2026") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Length: ${editedPepper.length} chars",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (editedPepper.length >= 8) MatrixGreenPrimary else CyberAmber
                            )
                        )
                        CyberSecondaryButton(
                            text = "Paste Key",
                            icon = Icons.Default.ContentCopy,
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    editedPepper = clip.trim()
                                    Toast.makeText(context, "Key pasted from clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            accentColor = CyberCyan,
                            depth = 2.5.dp
                        )
                    }
                    if (editedPepper.length < 8 && editedPepper.isNotEmpty()) {
                        Text(
                            text = "Security notice: Enter at least 8 to 32 characters.",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberAmber)
                        )
                    }
                }
            },
            confirmButton = {
                CyberButton(
                    text = "Save Key",
                    icon = Icons.Default.VpnKey,
                    onClick = {
                        val trimmed = editedPepper.trim()
                        if (trimmed.isEmpty()) {
                            Toast.makeText(context, "Key cannot be empty.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updatePepper(trimmed)
                            showPepperDialog = false
                            Toast.makeText(context, "Secret key saved successfully.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    accentColor = MatrixGreenPrimary,
                    depth = 3.5.dp
                )
            },
            dismissButton = {
                CyberSecondaryButton(
                    text = "Reset Default",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        viewModel.resetPepper()
                        editedPepper = CipherEngine.DEFAULT_PEPPER
                        showPepperDialog = false
                        Toast.makeText(context, "Pepper reset to system default.", Toast.LENGTH_SHORT).show()
                    },
                    accentColor = CyberCyan
                )
            },
            containerColor = Color(0xFF0F1A14),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Passcode Setup Dialog for PIN or Alphanumeric Password
    if (showPasscodeDialog) {
        val isPin = pendingLockType == AppLockType.PIN_NUMERIC
        AlertDialog(
            onDismissRequest = {
                showPasscodeDialog = false
                passcodeDialogError = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPin) Icons.Default.Key else Icons.Default.Password,
                        contentDescription = null,
                        tint = MatrixGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPin) "Set Numeric PIN" else "Set Master Password",
                        style = MaterialTheme.typography.titleMedium.copy(color = MatrixGreenPrimary)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isPin)
                            "Create a numeric PIN (4-8 digits) to secure your vault with on-screen numeric keypad."
                        else
                            "Create an alphanumeric password (min 6 characters) to protect your workspace.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPasscodeInput,
                        onValueChange = { input ->
                            if (isPin) {
                                if (input.all { it.isDigit() } && input.length <= 12) {
                                    newPasscodeInput = input
                                    passcodeDialogError = null
                                }
                            } else {
                                newPasscodeInput = input
                                passcodeDialogError = null
                            }
                        },
                        label = { Text(if (isPin) "New PIN" else "New Password") },
                        placeholder = { Text(if (isPin) "e.g. 135790" else "e.g. SecretVault2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPasscodeInput,
                        onValueChange = { input ->
                            if (isPin) {
                                if (input.all { it.isDigit() } && input.length <= 12) {
                                    confirmPasscodeInput = input
                                    passcodeDialogError = null
                                }
                            } else {
                                confirmPasscodeInput = input
                                passcodeDialogError = null
                            }
                        },
                        label = { Text(if (isPin) "Confirm PIN" else "Confirm Password") },
                        placeholder = { Text("Re-enter to confirm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    passcodeDialogError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = com.kafappstore.ciphervault.ui.theme.CyberCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            },
            confirmButton = {
                CyberButton(
                    text = "Save Passcode",
                    icon = Icons.Default.Lock,
                    onClick = {
                        if (isPin && (newPasscodeInput.length < 4 || newPasscodeInput.length > 12)) {
                            passcodeDialogError = "PIN must be between 4 and 12 digits."
                        } else if (!isPin && newPasscodeInput.length < 6) {
                            passcodeDialogError = "Password must be at least 6 characters."
                        } else if (newPasscodeInput != confirmPasscodeInput) {
                            passcodeDialogError = "Passcodes do not match."
                        } else {
                            viewModel.savePasscode(newPasscodeInput)
                            viewModel.setLockType(pendingLockType)
                            viewModel.setBiometricLock(true)
                            showPasscodeDialog = false
                            passcodeDialogError = null
                            Toast.makeText(context, "Lock method configured and activated.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    accentColor = MatrixGreenPrimary,
                    depth = 3.5.dp
                )
            },
            dismissButton = {
                CyberSecondaryButton(
                    text = "Cancel",
                    icon = Icons.Default.Close,
                    onClick = {
                        showPasscodeDialog = false
                        passcodeDialogError = null
                    },
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
        Text(
            text = "Security System Settings",
            style = MaterialTheme.typography.titleLarge.copy(color = MatrixGreenPrimary)
        )
        Text(
            text = "Configure cryptographic parameters for CipherVault engine",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B8A74))
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Pepper Setting Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Secret Key (Pepper)",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                        )
                    }

                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberAmber)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A secret string combined with user passwords before PBKDF2 derivation.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88A391))
                )

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF050B07),
                    border = BorderStroke(1.dp, Color(0xFF162B1D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = Color(0xFF6B8A74),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE VALUE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF88A391),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF102417),
                                border = BorderStroke(1.dp, MatrixGreenPrimary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${settings.pepper.length} CHARS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MatrixGreenPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (settings.pepper.length > 14)
                                "${settings.pepper.take(8)}••••••••••••••••${settings.pepper.takeLast(6)}"
                            else
                                settings.pepper,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MatrixGreenPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary full-width 3D key button
                CyberButton(
                    text = "Edit Secret Key (Pepper)",
                    icon = Icons.Default.Edit,
                    onClick = {
                        editedPepper = settings.pepper
                        showPepperDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = CyberAmber
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary 3D action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberSecondaryButton(
                        text = "Copy Key",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            SecureClipboardHelper.copyToClipboard(
                                context = context,
                                label = "CipherVault Pepper Secret Key",
                                text = settings.pepper,
                                isSensitive = true,
                                autoClearSeconds = 30L,
                                onSuccessMessage = "Secret key copied (auto-cleared in 30 seconds for security)"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = MatrixGreenPrimary
                    )

                    CyberSecondaryButton(
                        text = "Reset Default",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            viewModel.resetPepper()
                            Toast.makeText(context, "Pepper reset to default.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = CyberCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage & File Access Permission Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1610)),
            shape = RoundedCornerShape(12.dp)
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
                            imageVector = Icons.Default.FolderShared,
                            contentDescription = null,
                            tint = if (hasStorageAccess) MatrixGreenPrimary else CyberAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Storage & File Access",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (hasStorageAccess) Color(0xFF102417) else Color(0xFF2B180A),
                        border = BorderStroke(
                            1.dp,
                            if (hasStorageAccess) MatrixGreenPrimary.copy(alpha = 0.6f) else CyberAmber.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = if (hasStorageAccess) "GRANTED ✓" else "RESTRICTED ⚠️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (hasStorageAccess) MatrixGreenPrimary else CyberAmber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Allows CipherVault to browse device directories (Downloads, Documents, Internal Storage, and SD Cards), load files for encryption/decryption, and save encrypted vaults. You can grant or revoke this access at any time.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88A391), fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberButton(
                        text = if (hasStorageAccess) "Manage Access" else "Grant Access",
                        icon = if (hasStorageAccess) Icons.Default.CheckCircle else Icons.Default.Security,
                        onClick = {
                            StoragePermissionHelper.requestStoragePermission(context)
                        },
                        accentColor = if (hasStorageAccess) MatrixGreenPrimary else CyberAmber,
                        modifier = Modifier.weight(1f),
                        testTag = "settings_grant_storage_btn"
                    )

                    CyberSecondaryButton(
                        text = "Revoke in Settings",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            StoragePermissionHelper.openAppSettings(context)
                        },
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "settings_revoke_storage_btn"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Auto-clear Memory Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-clear RAM Memory",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatically wipe decrypted plaintext from RAM after 90 seconds to prevent data leakage.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88A391))
                    )
                }

                Switch(
                    checked = settings.autoClearMemory,
                    onCheckedChange = { viewModel.setAutoClear(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = MatrixGreenPrimary,
                        uncheckedTrackColor = Color(0xFF1B2C21)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. App Lock & Authentication Method Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vault Security Lock",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Require authentication when launching app or returning from background to secure local projects.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88A391))
                        )
                    }

                    Switch(
                        checked = settings.biometricLockEnabled,
                        onCheckedChange = { viewModel.setBiometricLock(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyberAmber,
                            uncheckedTrackColor = Color(0xFF1B2C21)
                        )
                    )
                }

                if (settings.biometricLockEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Authentication Method",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberAmber,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val lockMethods = listOf(
                        Triple(
                            AppLockType.BIOMETRIC,
                            "Biometric / Fingerprint",
                            Icons.Default.Fingerprint
                        ),
                        Triple(
                            AppLockType.PIN_NUMERIC,
                            "Numeric PIN (Keypad)",
                            Icons.Default.Key
                        ),
                        Triple(
                            AppLockType.PASSWORD_ALPHANUMERIC,
                            "Alphanumeric Password",
                            Icons.Default.Password
                        )
                    )

                    for ((type, label, icon) in lockMethods) {
                        val isSelected = settings.lockType == type
                        Surface(
                            onClick = {
                                if (type == AppLockType.BIOMETRIC) {
                                    viewModel.setLockType(AppLockType.BIOMETRIC)
                                } else {
                                    pendingLockType = type
                                    newPasscodeInput = ""
                                    confirmPasscodeInput = ""
                                    passcodeDialogError = null
                                    showPasscodeDialog = true
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF16251A) else Color(0xFF070F0A),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MatrixGreenPrimary else Color(0xFF1B3623)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MatrixGreenPrimary else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) MatrixGreenPrimary else Color.LightGray,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }

                                if (isSelected) {
                                    Text(
                                        text = "Active ✓",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MatrixGreenPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (settings.lockType != AppLockType.BIOMETRIC) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            CyberSecondaryButton(
                                text = "Change Passcode",
                                icon = Icons.Default.Edit,
                                onClick = {
                                    pendingLockType = settings.lockType
                                    newPasscodeInput = ""
                                    confirmPasscodeInput = ""
                                    passcodeDialogError = null
                                    showPasscodeDialog = true
                                },
                                accentColor = CyberCyan,
                                depth = 2.5.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Screen Security Card (FLAG_SECURE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Screenshot & Task Switcher Guard",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enable FLAG_SECURE window flag to block screen recordings, screenshots, and task preview thumbnails.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88A391))
                    )
                }

                Switch(
                    checked = settings.screenSecurityEnabled,
                    onCheckedChange = { viewModel.setScreenSecurity(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberCyan,
                        uncheckedTrackColor = Color(0xFF1B2C21)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Output Threshold Setting
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Output Length Threshold (chars)",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                        )
                    }

                    Text(
                        text = "${settings.outputThreshold} chars",
                        style = MaterialTheme.typography.labelMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Outputs exceeding this threshold will trigger a warning and require confirmation before copying (default: 3000).",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88A391))
                )

                Spacer(modifier = Modifier.height(8.dp))

                var sliderPos by remember(settings.outputThreshold) {
                    mutableFloatStateOf(settings.outputThreshold.toFloat())
                }

                Slider(
                    value = sliderPos,
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = { viewModel.setThreshold(sliderPos.toInt()) },
                    valueRange = 1000f..10000f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = Color(0xFF1B2C21)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Cyberpunk Theme Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MatrixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visual Theme",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val themes = listOf(
                    CyberThemeMode.MATRIX_GREEN to ("Matrix Classic" to MatrixGreenPrimary),
                    CyberThemeMode.CYBER_NEON to ("Cyberpunk Neon" to CyberCyan),
                    CyberThemeMode.DEEP_VOID to ("Deep Void (Pitch Black)" to Color.White)
                )

                themes.forEach { (mode, pair) ->
                    val isSelected = settings.themeMode == mode
                    val (title, accent) = pair
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF15261D) else Color(0xFF08110B))
                            .border(
                                BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accent else Color(0x334C7356)
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setTheme(mode) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) accent else Color.LightGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            if (isSelected) {
                                Text(
                                    text = "Active ✓",
                                    style = MaterialTheme.typography.labelSmall.copy(color = accent)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
