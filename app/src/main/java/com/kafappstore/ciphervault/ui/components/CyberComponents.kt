package com.kafappstore.ciphervault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCrimson
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.ui.theme.MatrixTextCode
import kotlin.random.Random

/**
 * 3D Tactile Cyber Button with Glowing Borders and Press Depth
 */
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    accentColor: Color = MatrixGreenPrimary,
    testTag: String = "cyber_button"
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonShape = RoundedCornerShape(12.dp)

    // Dynamic 3D depth shift
    val elevation = if (isPressed) 2.dp else 6.dp
    val glowColor = if (enabled) accentColor.copy(alpha = if (isPressed) 0.8f else 0.4f) else Color.Transparent

    val backgroundBrush = if (enabled) {
        Brush.verticalGradient(
            colors = if (isPressed) {
                listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.10f))
            } else {
                listOf(accentColor.copy(alpha = 0.20f), accentColor.copy(alpha = 0.05f))
            }
        )
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1B241E), Color(0xFF141A16)))
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .shadow(elevation = elevation, shape = buttonShape, spotColor = glowColor, ambientColor = glowColor)
            .clip(buttonShape)
            .background(backgroundBrush)
            .border(
                BorderStroke(
                    width = if (isPressed) 2.dp else 1.5.dp,
                    color = if (enabled) accentColor.copy(alpha = if (isPressed) 0.95f else 0.7f) else Color(0x334C7356)
                ),
                shape = buttonShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Processing...",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) accentColor else Color(0xFF6B8071),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (enabled) accentColor else Color(0xFF6B8071),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Compact Tactical Secondary Cyber Action Button
 */
@Composable
fun CyberSecondaryButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = MatrixGreenPrimary,
    testTag: String = "secondary_button"
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .testTag(testTag)
            .clip(shape)
            .background(Color(0xFF0D1812))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Cyber Terminal Input Field with Custom Dark Cyber Styling
 */
@Composable
fun CyberTerminalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 8,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = "terminal_input"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4C7356))
            )
        },
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MatrixGreenPrimary,
            lineHeight = 20.sp
        ),
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MatrixGreenPrimary,
            unfocusedBorderColor = MatrixBorderNeon,
            focusedContainerColor = Color(0xFF08110B),
            unfocusedContainerColor = Color(0xFF08110B),
            cursorColor = MatrixGreenPrimary
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
    )
}

/**
 * Password Strength Meter (12 to 22 characters)
 */
@Composable
fun PasswordStrengthMeter(
    password: String,
    modifier: Modifier = Modifier
) {
    val length = password.length
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val strengthScore = when {
        length < 12 -> 0
        length in 12..22 && ((hasUpper && hasLower && hasDigit) || hasSpecial) && length >= 16 -> 3 // Strong
        length in 12..22 && ((hasUpper && hasLower) || (hasLower && hasDigit)) -> 2 // Medium
        length in 12..22 -> 1 // Weak
        else -> 0
    }

    val (label, color) = when (strengthScore) {
        3 -> "Strong (Maximum Security)" to MatrixGreenPrimary
        2 -> "Moderate (Acceptable)" to CyberCyan
        1 -> "Weak (Improvement Advised)" to CyberAmber
        else -> if (length == 0) "Enter 12 to 22 characters" to Color(0xFF5A7864)
                else "Password length must be 12-22" to CyberCrimson
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength: $label",
                style = MaterialTheme.typography.labelSmall.copy(color = color)
            )
            Text(
                text = "$length / 22 characters",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (length in 12..22) MatrixGreenPrimary else CyberCrimson
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (step in 1..3) {
                val active = strengthScore >= step
                val stepColor = if (active) color else Color(0xFF19291E)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(stepColor)
                )
            }
        }
    }
}

/**
 * Output Display Section with Strict Conditional Logic (Section 7):
 * - Short (< 3000 chars): Display in text field, Copy enabled, Save enabled
 * - Medium (3000 - 10000 chars): Display with warning, Copy with confirmation, Save prominent
 * - Long (> 10000 chars): Text field hidden/read-only info, Copy hidden/disabled, Only Save active with alert
 */
@Composable
fun ConditionalOutputSection(
    output: String,
    onCopy: () -> Unit,
    onSaveToFile: () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Int = 3000
) {
    var showCopyConfirmDialog by remember { mutableStateOf(false) }
    val length = output.length

    val outputMode = when {
        length < threshold -> OutputMode.SHORT
        length in threshold..10000 -> OutputMode.MEDIUM
        else -> OutputMode.LONG
    }

    if (showCopyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCopyConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Copy Large Text",
                    style = MaterialTheme.typography.titleMedium.copy(color = CyberAmber)
                )
            },
            text = {
                Text(
                    text = "Output size is $length characters. Copying this amount to the clipboard may cause lag. Do you wish to continue?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCopyConfirmDialog = false
                        onCopy()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreenPrimary)
                ) {
                    Text("Copy", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCopyConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF101B15),
            shape = RoundedCornerShape(12.dp)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
        color = Color(0xFF09120C),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Encryption Result (Base64)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MatrixGreenPrimary,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = "$length characters",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = when (outputMode) {
                            OutputMode.SHORT -> MatrixGreenPrimary
                            OutputMode.MEDIUM -> CyberAmber
                            OutputMode.LONG -> CyberCrimson
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (outputMode) {
                OutputMode.SHORT -> {
                    // Case 1: Short output (< 3000 chars)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF050B07))
                            .border(BorderStroke(1.dp, Color(0xFF1B3824)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = output,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MatrixTextCode,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberButton(
                            text = "Copy to Clipboard",
                            icon = Icons.Default.ContentCopy,
                            onClick = onCopy,
                            modifier = Modifier.weight(1f),
                            accentColor = MatrixGreenPrimary,
                            testTag = "copy_output_button"
                        )
                        CyberButton(
                            text = "Save to File",
                            icon = Icons.Default.FileDownload,
                            onClick = onSaveToFile,
                            modifier = Modifier.weight(1f),
                            accentColor = CyberCyan,
                            testTag = "save_file_button"
                        )
                    }
                }

                OutputMode.MEDIUM -> {
                    // Case 2: Medium output (3000 to 10000 chars)
                    // Warning Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFB800))
                            .border(BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Output is large ($length characters), copying may be slow.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberAmber)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF050B07))
                            .border(BorderStroke(1.dp, Color(0xFF1B3824)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = output,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MatrixTextCode,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberButton(
                            text = "Copy (Confirm)",
                            icon = Icons.Default.ContentCopy,
                            onClick = { showCopyConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            accentColor = CyberAmber,
                            testTag = "copy_output_confirm_button"
                        )
                        // Save button is highlighted prominently
                        CyberButton(
                            text = "Save to File ★",
                            icon = Icons.Default.FileDownload,
                            onClick = onSaveToFile,
                            modifier = Modifier.weight(1.3f),
                            accentColor = MatrixGreenPrimary,
                            testTag = "save_file_prominent_button"
                        )
                    }
                }

                OutputMode.LONG -> {
                    // Case 3: Long output (> 10000 chars)
                    // Text field hidden / read-only summary, copy disabled/hidden, only save button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FF3366))
                            .border(BorderStroke(1.dp, CyberCrimson.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyberCrimson,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Output is very large ($length characters).",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please save it directly to a file. In-line text rendering is disabled to preserve device performance.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFFA3B8),
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ONLY Save to File button
                    CyberButton(
                        text = "Save to File (Storage Access Framework)",
                        icon = Icons.Default.FileDownload,
                        onClick = onSaveToFile,
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = MatrixGreenPrimary,
                        testTag = "save_file_only_button"
                    )
                }
            }
        }
    }
}

private enum class OutputMode {
    SHORT,
    MEDIUM,
    LONG
}

/**
 * Ambient Cyber Matrix Digital Scanlines & Rain Canvas
 */
@Composable
fun MatrixRainCanvas(
    modifier: Modifier = Modifier,
    alpha: Float = 0.08f
) {
    val transition = rememberInfiniteTransition(label = "matrix_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val lineSpacing = 32f
        val cols = (width / lineSpacing).toInt()

        for (c in 0..cols) {
            val x = c * lineSpacing
            val yOffset = ((c * 47f) + (phase * height)) % height
            drawLine(
                color = MatrixGreenPrimary.copy(alpha = alpha),
                start = Offset(x, yOffset),
                end = Offset(x, (yOffset + 60f).coerceAtMost(height)),
                strokeWidth = 1.2f
            )
        }

        // Horizontal scanlines
        val scanlineGap = 8f
        var y = 0f
        while (y < height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += scanlineGap
        }
    }
}
