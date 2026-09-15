package com.kafappstore.ciphervault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * Duolingo-Style 3D Tactile Cyber Button with Obsidian Black & Matrix Green Theme
 * Features a physical base extrusion plate, dynamic press-down travel, and single-line text protection.
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
    depth: Dp = 3.5.dp,
    testTag: String = "cyber_button"
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile push-down depth animation (Duolingo style)
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled && !isLoading) depth else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "cyber_button_offset"
    )

    val buttonShape = RoundedCornerShape(10.dp)

    // Base extrusion rim (dark saturated 3D platform beneath the button)
    val baseExtrusionColor = remember(accentColor, enabled) {
        if (!enabled) Color(0xFF0C140E)
        else when (accentColor) {
            MatrixGreenPrimary -> Color(0xFF003814)
            CyberCyan -> Color(0xFF003440)
            CyberAmber -> Color(0xFF422C00)
            CyberCrimson -> Color(0xFF420715)
            else -> Color(0xFF082B14)
        }
    }

    // Top face matrix background
    val faceBg = remember(accentColor, enabled) {
        if (!enabled) Color(0xFF131A15)
        else when (accentColor) {
            MatrixGreenPrimary -> Color(0xFF07140B)
            CyberCyan -> Color(0xFF061418)
            CyberAmber -> Color(0xFF141005)
            CyberCrimson -> Color(0xFF140508)
            else -> Color(0xFF0A160E)
        }
    }

    val borderColor = if (enabled) accentColor.copy(alpha = 0.85f) else Color(0x334C7356)
    val contentColor = if (enabled) accentColor else Color(0xFF5B7363)

    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Bottom Extrusion Plate (The Duolingo 3D lower rim)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(buttonShape)
                .background(baseExtrusionColor)
                .border(BorderStroke(1.2.dp, borderColor.copy(alpha = 0.45f)), buttonShape)
        )

        // 2. Raised Top Face (Shifts down on press)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = pressOffset)
                .padding(bottom = depth)
                .clip(buttonShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            faceBg.copy(alpha = 0.95f),
                            faceBg
                        )
                    )
                )
                .border(BorderStroke(1.3.dp, borderColor), buttonShape)
                .padding(vertical = 8.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.3.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Compact Tactical Secondary Cyber Action Button in 3D Duolingo Style
 */
@Composable
fun CyberSecondaryButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = MatrixGreenPrimary,
    enabled: Boolean = true,
    depth: Dp = 3.dp,
    testTag: String = "secondary_button"
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) depth else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "sec_button_offset"
    )

    val baseExtrusionColor = remember(accentColor, enabled) {
        if (!enabled) Color(0xFF0C140E)
        else when (accentColor) {
            MatrixGreenPrimary -> Color(0xFF003011)
            CyberCyan -> Color(0xFF002B36)
            CyberAmber -> Color(0xFF382500)
            CyberCrimson -> Color(0xFF380612)
            else -> Color(0xFF072410)
        }
    }

    val faceBg = remember(accentColor, enabled) {
        if (!enabled) Color(0xFF131A15)
        else when (accentColor) {
            MatrixGreenPrimary -> Color(0xFF08150D)
            CyberCyan -> Color(0xFF06151A)
            CyberAmber -> Color(0xFF141005)
            CyberCrimson -> Color(0xFF15060A)
            else -> Color(0xFF0B140E)
        }
    }

    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (enabled) accentColor.copy(alpha = 0.8f) else Color(0x334C7356)
    val contentColor = if (enabled) accentColor else Color(0xFF5B7363)

    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 34.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Bottom Extrusion Plate
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(shape)
                .background(baseExtrusionColor)
                .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)), shape)
        )

        // 2. Raised Top Face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = pressOffset)
                .padding(bottom = depth)
                .clip(shape)
                .background(faceBg)
                .border(BorderStroke(1.2.dp, borderColor), shape)
                .padding(horizontal = 7.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3D Duolingo Tactile Submode Switch Tab (Text vs File Streaming)
 */
@Composable
fun Cyber3DTab(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MatrixGreenPrimary
) {
    val haptic = LocalHapticFeedback.current
    val depth = 3.5.dp
    val shape = RoundedCornerShape(9.dp)

    val offset by animateDpAsState(
        targetValue = if (selected) depth else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "tab_3d_offset"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Base plate (depth rim)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .clip(shape)
                .background(if (selected) Color(0xFF003814) else Color(0xFF0A140E))
                .border(
                    BorderStroke(
                        1.dp,
                        if (selected) accentColor.copy(alpha = 0.5f) else MatrixBorderNeon.copy(alpha = 0.3f)
                    ),
                    shape
                )
        )

        // Top face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offset)
                .padding(bottom = depth)
                .clip(shape)
                .background(
                    if (selected) {
                        Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.85f)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF0F1E14), Color(0xFF08120B)))
                    }
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        if (selected) accentColor else MatrixBorderNeon.copy(alpha = 0.5f)
                    ),
                    shape
                )
                .padding(vertical = 7.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.Black else Color.LightGray,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (selected) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
                CyberButton(
                    text = "Copy Anyway",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showCopyConfirmDialog = false
                        onCopy()
                    },
                    accentColor = MatrixGreenPrimary,
                    depth = 3.5.dp,
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            },
            dismissButton = {
                CyberSecondaryButton(
                    text = "Cancel",
                    icon = Icons.Default.Close,
                    onClick = { showCopyConfirmDialog = false },
                    accentColor = Color.LightGray
                )
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
                            text = "Copy Text",
                            icon = Icons.Default.ContentCopy,
                            onClick = onCopy,
                            modifier = Modifier.weight(1f),
                            accentColor = MatrixGreenPrimary,
                            testTag = "copy_output_button"
                        )
                        CyberButton(
                            text = "Save File",
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
                        text = "Save to File",
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
