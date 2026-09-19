package com.kafappstore.ciphervault.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.util.StoragePermissionHelper
import kotlinx.coroutines.delay

@Composable
fun StoragePermissionDialog(
    onDismiss: () -> Unit,
    onPermissionGranted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(StoragePermissionHelper.hasStoragePermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Android 6-10 direct permission request launcher
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || StoragePermissionHelper.hasStoragePermission(context)) {
            isGranted = true
            Toast.makeText(context, "Storage permission granted", Toast.LENGTH_SHORT).show()
            onPermissionGranted?.invoke()
            onDismiss() // Automatically dismiss/hide the dialog!
        }
    }

    // Automatically check and dismiss the dialog box when resuming from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermissionHelper.hasStoragePermission(context)
                isGranted = granted
                if (granted) {
                    Toast.makeText(context, "Storage permission granted", Toast.LENGTH_SHORT).show()
                    onPermissionGranted?.invoke()
                    onDismiss() // Automatically dismiss/hide the dialog!
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodically re-check while active so the dialog box disappears as soon as permission is granted
    LaunchedEffect(Unit) {
        if (StoragePermissionHelper.hasStoragePermission(context)) {
            onPermissionGranted?.invoke()
            onDismiss()
            return@LaunchedEffect
        }
        while (true) {
            delay(800)
            if (StoragePermissionHelper.hasStoragePermission(context)) {
                isGranted = true
                onPermissionGranted?.invoke()
                onDismiss()
                break
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(16.dp)),
        containerColor = Color(0xFF060E09),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0E1F14),
                    border = BorderStroke(1.dp, if (isGranted) MatrixGreenPrimary else CyberCyan)
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = if (isGranted) MatrixGreenPrimary else CyberCyan,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "STORAGE PERMISSION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "File Manager & Vault Access",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CipherVault needs storage permission to browse directories, read files for encryption/decryption, and save secured vaults across your device folders (Downloads, Documents, and Internal Storage).",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB0C4B6),
                        lineHeight = 19.sp,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Permission Status Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF09140D),
                    border = BorderStroke(
                        1.dp,
                        if (isGranted) MatrixGreenPrimary.copy(alpha = 0.5f) else CyberAmber.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isGranted) MatrixGreenPrimary else CyberAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Text(
                            text = if (isGranted) "GRANTED ✓" else "NOT GRANTED ⚠️",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isGranted) MatrixGreenPrimary else CyberAmber,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                if (!isGranted) {
                    CyberButton(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            "Grant All Files Access"
                        else
                            "Grant Storage Permission",
                        icon = Icons.Default.Security,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                StoragePermissionHelper.requestStoragePermission(context)
                            } else {
                                legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_grant_storage_permission"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CyberSecondaryButton(
                        text = "Open App Settings",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            StoragePermissionHelper.openAppSettings(context)
                        },
                        accentColor = Color.LightGray,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_open_app_settings"
                    )
                } else {
                    CyberButton(
                        text = "Permission Granted - Continue",
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            onPermissionGranted?.invoke()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_permission_continue"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = if (isGranted) "Close" else "Later",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    )
}
