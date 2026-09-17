package com.kafappstore.ciphervault.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.data.AppLockType
import com.kafappstore.ciphervault.data.AppSettingsState
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCrimson
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary

@Composable
fun AppAuthenticationOverlay(
    settings: AppSettingsState,
    onBiometricClick: () -> Unit,
    onVerifyPasscode: (String) -> Boolean,
    onUnlockSuccess: () -> Unit
) {
    var pinDigits by remember { mutableStateOf("") }
    var alphanumericPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isPinMode = settings.lockType == AppLockType.PIN_NUMERIC
    val isAlphaMode = settings.lockType == AppLockType.PASSWORD_ALPHANUMERIC
    val isBiometricMode = settings.lockType == AppLockType.BIOMETRIC

    // Function to verify current input
    fun verifyCurrent(passcode: String) {
        if (passcode.isBlank()) {
            errorMessage = "Please enter your passcode"
            return
        }
        val isCorrect = onVerifyPasscode(passcode)
        if (isCorrect) {
            errorMessage = null
            onUnlockSuccess()
        } else {
            errorMessage = "Incorrect passcode. Access Denied."
            if (isPinMode) {
                pinDigits = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030704)),
        contentAlignment = Alignment.Center
    ) {
        MatrixRainCanvas(alpha = 0.12f)

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    BorderStroke(
                        1.5.dp,
                        if (errorMessage != null) CyberCrimson else CyberAmber
                    ),
                    RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF09130D)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF142217))
                        .border(
                            BorderStroke(1.5.dp, if (errorMessage != null) CyberCrimson else CyberAmber),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            errorMessage != null -> Icons.Default.Lock
                            isPinMode -> Icons.Default.Key
                            isAlphaMode -> Icons.Default.Password
                            else -> Icons.Default.Fingerprint
                        },
                        contentDescription = "Security Lock",
                        tint = if (errorMessage != null) CyberCrimson else CyberAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "VAULT SECURED",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (settings.lockType) {
                        AppLockType.PIN_NUMERIC -> "NUMERIC PIN ENCLAVE AUTHENTICATION"
                        AppLockType.PASSWORD_ALPHANUMERIC -> "ALPHANUMERIC MASTER PASSWORD AUTHENTICATION"
                        AppLockType.BIOMETRIC -> "BIOMETRIC HARDWARE ENCLAVE AUTHENTICATION"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberAmber,
                        letterSpacing = 0.8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (settings.lockType) {
                        AppLockType.PIN_NUMERIC -> "Enter your security PIN on the keypad to decrypt the local workspace."
                        AppLockType.PASSWORD_ALPHANUMERIC -> "Enter your master alphanumeric password to unlock CipherVault."
                        AppLockType.BIOMETRIC -> "Biometric or system credentials verification required to access encrypted projects."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFA5C4AF),
                        fontSize = 11.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Error Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        color = Color(0x33FF3366),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberCrimson,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode 1: Numeric PIN Keypad
                if (isPinMode) {
                    // PIN Dots Display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        val maxDisplay = pinDigits.length.coerceAtLeast(4)
                        for (i in 0 until maxDisplay) {
                            val isFilled = i < pinDigits.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isFilled) MatrixGreenPrimary else Color(0xFF14241B))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isFilled) MatrixGreenPrimary else Color(0xFF2C4B37)
                                        ),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // Keypad Grid: 1..9, Backspace, 0, Unlock
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9")
                        )

                        for (row in rows) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (digit in row) {
                                    NumericKeypadButton(
                                        text = digit,
                                        onClick = {
                                            if (pinDigits.length < 12) {
                                                pinDigits += digit
                                                errorMessage = null
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Last row: Backspace, 0, Unlock confirm
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Backspace
                            Surface(
                                onClick = {
                                    if (pinDigits.isNotEmpty()) {
                                        pinDigits = pinDigits.dropLast(1)
                                        errorMessage = null
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF131D16),
                                border = BorderStroke(1.dp, Color(0xFF294732)),
                                modifier = Modifier
                                    .size(62.dp, 46.dp)
                                    .testTag("pin_keypad_backspace")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = CyberAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Digit 0
                            NumericKeypadButton(
                                text = "0",
                                onClick = {
                                    if (pinDigits.length < 12) {
                                        pinDigits += "0"
                                        errorMessage = null
                                    }
                                }
                            )

                            // Confirm / Unlock button
                            Surface(
                                onClick = { verifyCurrent(pinDigits) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (pinDigits.isNotEmpty()) MatrixGreenPrimary else Color(0xFF132218),
                                border = BorderStroke(1.dp, MatrixGreenPrimary),
                                modifier = Modifier
                                    .size(62.dp, 46.dp)
                                    .testTag("pin_keypad_unlock")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Unlock",
                                        tint = if (pinDigits.isNotEmpty()) Color.Black else MatrixGreenPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Biometric Fallback button if device supports it
                    Spacer(modifier = Modifier.height(14.dp))
                    CyberSecondaryButton(
                        text = "Use Biometric / System Credentials",
                        icon = Icons.Default.Fingerprint,
                        onClick = onBiometricClick,
                        accentColor = CyberCyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mode 2: Professional Alphanumeric Password
                else if (isAlphaMode) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = alphanumericPassword,
                            onValueChange = {
                                alphanumericPassword = it
                                errorMessage = null
                            },
                            label = { Text("Master Password") },
                            placeholder = { Text("Enter password...") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = MatrixGreenPrimary
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { verifyCurrent(alphanumericPassword) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MatrixGreenPrimary,
                                unfocusedBorderColor = Color(0xFF23442C),
                                focusedLabelColor = MatrixGreenPrimary,
                                unfocusedLabelColor = Color.Gray,
                                cursorColor = MatrixGreenPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_auth_alphanumeric_password")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CyberButton(
                            text = "Unlock Vault",
                            icon = Icons.Default.LockOpen,
                            onClick = { verifyCurrent(alphanumericPassword) },
                            accentColor = MatrixGreenPrimary,
                            depth = 4.dp,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "btn_auth_unlock_alphanumeric"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CyberSecondaryButton(
                            text = "Use Biometric / System Credentials",
                            icon = Icons.Default.Fingerprint,
                            onClick = onBiometricClick,
                            accentColor = CyberCyan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Mode 3: Biometric Lock Default
                else {
                    CyberButton(
                        text = "Verify Biometrics / PIN",
                        icon = Icons.Default.Fingerprint,
                        onClick = onBiometricClick,
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = CyberAmber,
                        depth = 4.dp,
                        testTag = "btn_unlock_biometric"
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericKeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F1D13),
        border = BorderStroke(1.dp, Color(0xFF274431)),
        modifier = Modifier
            .size(62.dp, 46.dp)
            .testTag("pin_key_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            )
        }
    }
}
