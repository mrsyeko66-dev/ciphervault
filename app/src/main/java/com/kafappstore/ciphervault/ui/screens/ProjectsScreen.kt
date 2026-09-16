package com.kafappstore.ciphervault.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.data.db.CipherProject
import com.kafappstore.ciphervault.ui.components.CyberButton
import com.kafappstore.ciphervault.ui.components.CyberSecondaryButton
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
fun ProjectsScreen(
    viewModel: CipherViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToEncrypt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.allProjects.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<CipherProject?>(null) }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) {
            projects
        } else {
            projects.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.tag.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Delete Confirmation Dialog
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = {
                Text("Delete Project", color = CyberCrimson, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${proj.title}'? This action cannot be undone.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                CyberButton(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    onClick = {
                        viewModel.deleteProject(proj)
                        Toast.makeText(context, "Project deleted.", Toast.LENGTH_SHORT).show()
                        projectToDelete = null
                    },
                    accentColor = CyberCrimson,
                    depth = 3.5.dp,
                    testTag = "btn_confirm_delete_project"
                )
            },
            dismissButton = {
                CyberSecondaryButton(
                    text = "Cancel",
                    icon = Icons.Default.Close,
                    onClick = { projectToDelete = null },
                    accentColor = Color.LightGray
                )
            },
            containerColor = Color(0xFF14070A)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Header & New Project button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "Projects & Drafts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MatrixGreenPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${projects.size} saved locally",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6B8A74)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CyberButton(
                text = "New",
                icon = Icons.Default.Add,
                onClick = {
                    viewModel.clearEditorContent()
                    onNavigateToEditor()
                },
                accentColor = MatrixGreenPrimary,
                testTag = "btn_new_project"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search titles and project text...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MatrixGreenPrimary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF33553C),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank())
                            "No projects created yet.\nYou can write your drafts here and continue on subsequent days."
                        else "No results found for your search.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProjects, key = { it.id }) { project ->
                    ProjectCardItem(
                        project = project,
                        onEdit = {
                            viewModel.loadProjectIntoEditor(project)
                            onNavigateToEditor()
                        },
                        onEncrypt = {
                            viewModel.encryptProjectDirectly(project)
                            onNavigateToEncrypt()
                        },
                        onDelete = {
                            projectToDelete = project
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCardItem(
    project: CipherProject,
    onEdit: () -> Unit,
    onEncrypt: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(project.updatedAt) { sdf.format(Date(project.updatedAt)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF08120B)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MatrixBorderNeon),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MatrixGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tags row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF142018))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AES-GCM 🔒",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF102617))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.tag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MatrixGreenPrimary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Content Preview snippet
            Text(
                text = if (project.content.isBlank()) "(Empty project)" else project.content,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9EBAA5),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modified: $formattedDate | ${project.content.length} chars",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF5B7864),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit in editor",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onEncrypt,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Direct encrypt",
                            tint = MatrixGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete project",
                            tint = CyberCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
