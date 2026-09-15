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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.kafappstore.ciphervault.data.CyberThemeMode
import com.kafappstore.ciphervault.ui.components.CyberButton
import com.kafappstore.ciphervault.ui.components.CyberSecondaryButton
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF050B07))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (settings.pepper.length > 14)
                            "${settings.pepper.take(8)}••••••••••••••••${settings.pepper.takeLast(6)}"
                        else
                            settings.pepper,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MatrixGreenPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "${settings.pepper.length} chars",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MatrixGreenPrimary
                        )
                    )
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
                            clipboardManager.setText(AnnotatedString(settings.pepper))
                            Toast.makeText(context, "Secret key copied to clipboard (store safely).", Toast.LENGTH_SHORT).show()
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

        // 3. Output Threshold Setting
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
