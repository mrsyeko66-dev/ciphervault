package com.kafappstore.ciphervault.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCrimson
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.ui.theme.MatrixTextCode
import com.kafappstore.ciphervault.viewmodel.CipherViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: CipherViewModel,
    onNavigateToEncrypt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val editorState by viewModel.editorState.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf(editorState.title) }
    var saveTagInput by remember { mutableStateOf(editorState.tag) }

    // Calculate live text stats
    val text = editorState.content
    val lineCount = remember(text) {
        val count = text.count { it == '\n' } + 1
        count.coerceAtLeast(1)
    }
    val charCount = remember(text) { text.length }
    val wordCount = remember(text) {
        if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = MatrixGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Projects & Drafts", color = MatrixGreenPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter project details for permanent local storage in the secure database:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("Project Title or Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveTagInput,
                        onValueChange = { saveTagInput = it },
                        label = { Text("Tag / Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                CyberButton(
                    text = "Save Project",
                    icon = Icons.Default.Save,
                    onClick = {
                        viewModel.onEditorTitleChanged(saveTitleInput)
                        viewModel.onEditorTagChanged(saveTagInput)
                        viewModel.saveEditorProject {
                            Toast.makeText(context, "Project saved to local database.", Toast.LENGTH_SHORT).show()
                            showSaveDialog = false
                        }
                    },
                    accentColor = MatrixGreenPrimary,
                    testTag = "btn_dialog_save_confirm"
                )
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF0D1C13)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {

        // Top Header: Title, Active Status, Stats
        Surface(
            color = Color(0xFF09140C),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MatrixBorderNeon),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = editorState.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MatrixGreenPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        if (editorState.isDirty) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "*",
                                color = CyberAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "Tag: ${editorState.tag} | Stats: $lineCount lines | $wordCount words | $charCount chars",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF7B9984),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            saveTitleInput = editorState.title
                            saveTagInput = editorState.tag
                            showSaveDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save to project",
                            tint = MatrixGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleEditorSearch() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search and replace",
                            tint = if (editorState.isSearchVisible) CyberCyan else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Search & Replace Panel (Collapsible)
        AnimatedVisibility(visible = editorState.isSearchVisible) {
            Surface(
                color = Color(0xFF0A1910),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CyberCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search & Replace in Text",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        IconButton(
                            onClick = { viewModel.toggleEditorSearch() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editorState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search word...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editorState.replaceQuery,
                            onValueChange = { viewModel.onReplaceQueryChanged(it) },
                            placeholder = { Text("Replace with...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    CyberSecondaryButton(
                        text = "Replace All",
                        icon = Icons.Default.FindReplace,
                        onClick = { viewModel.executeReplaceAll() },
                        accentColor = CyberCyan,
                        testTag = "btn_execute_replace"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Insert Toolbelt
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CyberSecondaryButton(
                text = "+ Time",
                icon = Icons.Default.AccessTime,
                onClick = { viewModel.insertEditorTimestamp() },
                accentColor = MatrixGreenPrimary,
                testTag = "btn_insert_timestamp"
            )

            CyberSecondaryButton(
                text = "+ Security Header",
                icon = Icons.Default.Security,
                onClick = { viewModel.insertEditorSecurityHeader() },
                accentColor = CyberAmber,
                testTag = "btn_insert_header"
            )

            CyberSecondaryButton(
                text = "+ Divider",
                icon = Icons.Default.Edit,
                onClick = { viewModel.insertEditorDivider() },
                accentColor = Color.LightGray,
                testTag = "btn_insert_divider"
            )

            CyberSecondaryButton(
                text = "Copy",
                icon = Icons.Default.ContentCopy,
                onClick = {
                    clipboardManager.setText(AnnotatedString(editorState.content))
                    Toast.makeText(context, "Text copied to clipboard.", Toast.LENGTH_SHORT).show()
                },
                accentColor = CyberCyan,
                testTag = "btn_copy_editor"
            )

            CyberSecondaryButton(
                text = "Clear",
                icon = Icons.Default.Clear,
                onClick = { viewModel.clearEditorContent() },
                accentColor = CyberCrimson,
                testTag = "btn_clear_editor"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Editor Surface with Line Numbers Gutter
        Surface(
            color = Color(0xFF050C07),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MatrixBorderNeon),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val totalLines = remember(editorState.content) {
                (editorState.content.count { it == '\n' } + 1).coerceAtLeast(1)
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                // Line Numbers Gutter
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .width(36.dp)
                        .padding(end = 8.dp)
                ) {
                    for (i in 1..totalLines) {
                        Text(
                            text = "$i",
                            style = TextStyle(
                                color = Color(0xFF33553C),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height((totalLines * 20).coerceAtLeast(40).dp)
                        .background(Color(0xFF1B3322))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Editable Area
                BasicTextField(
                    value = editorState.content,
                    onValueChange = { viewModel.onEditorContentChanged(it) },
                    textStyle = TextStyle(
                        color = Color(0xFFD4E6D9),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(MatrixGreenPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_text_input"),
                    decorationBox = { innerTextField ->
                        if (editorState.content.isEmpty()) {
                            Text(
                                text = "Start typing confidential text, reports, or documentation...\nYou can write today, save to projects, and continue tomorrow.",
                                style = TextStyle(
                                    color = Color(0xFF4A6B53),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Action Row: Direct Encryption & Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberButton(
                text = "Direct Encrypt This Text",
                icon = Icons.Default.Lock,
                onClick = {
                    viewModel.sendEditorToEncrypt()
                    onNavigateToEncrypt()
                },
                enabled = editorState.content.isNotBlank(),
                accentColor = MatrixGreenPrimary,
                modifier = Modifier.weight(1.3f),
                testTag = "btn_editor_direct_encrypt"
            )

            CyberSecondaryButton(
                text = "Save to Projects",
                icon = Icons.Default.Save,
                onClick = {
                    saveTitleInput = editorState.title
                    saveTagInput = editorState.tag
                    showSaveDialog = true
                },
                accentColor = CyberCyan,
                modifier = Modifier.weight(1f),
                testTag = "btn_editor_save_to_projects"
            )
        }
    }
}
