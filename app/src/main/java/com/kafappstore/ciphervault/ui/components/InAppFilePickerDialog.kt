package com.kafappstore.ciphervault.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary
import com.kafappstore.ciphervault.ui.theme.MatrixTextCode
import com.kafappstore.ciphervault.util.StoragePermissionHelper
import com.kafappstore.ciphervault.viewmodel.CipherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class StorageLocation(
    val title: String,
    val file: File,
    val icon: ImageVector
)

@Composable
fun InAppFilePickerDialog(
    viewModel: CipherViewModel,
    isForDecrypt: Boolean = true,
    onDismiss: () -> Unit,
    onLaunchSystemPicker: () -> Unit,
    onFileSelected: ((File) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val accentColor = if (isForDecrypt) CyberCyan else MatrixGreenPrimary

    // Storage permission state & reload trigger
    var reloadTrigger by remember { mutableStateOf(0) }
    var hasStoragePermission by remember {
        mutableStateOf(StoragePermissionHelper.hasStoragePermission(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermissionHelper.hasStoragePermission(context)
                if (granted) {
                    hasStoragePermission = true
                    reloadTrigger++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted || StoragePermissionHelper.hasStoragePermission(context)
        if (hasStoragePermission) {
            Toast.makeText(context, "Storage permission granted", Toast.LENGTH_SHORT).show()
            reloadTrigger++
        }
    }

    // Identify primary locations
    val storageLocations = remember(context) {
        val list = mutableListOf<StorageLocation>()
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null) {
            list.add(StorageLocation("Downloads", downloads, Icons.Default.Download))
        }

        val primaryStorage = Environment.getExternalStorageDirectory()
        if (primaryStorage != null && primaryStorage.exists()) {
            list.add(StorageLocation("Internal Storage", primaryStorage, Icons.Default.Smartphone))
            val docs = File(primaryStorage, "Documents")
            if (docs.exists()) {
                list.add(StorageLocation("Documents", docs, Icons.Default.Description))
            }
        }

        val appVault = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        list.add(StorageLocation("App Vault", appVault, Icons.Default.Lock))

        // Check for SD cards
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (f in externalDirs) {
                if (f != null && Environment.isExternalStorageRemovable(f)) {
                    var cur: File? = f
                    while (cur?.parentFile != null && cur.parentFile?.path != "/storage" && cur.parentFile?.path != "/") {
                        cur = cur.parentFile
                    }
                    if (cur != null && cur.exists() && cur.canRead()) {
                        list.add(StorageLocation("SD Card", cur, Icons.Default.SdCard))
                    }
                }
            }
        } catch (_: Exception) {}

        list.distinctBy { it.file.absolutePath }
    }

    // Default current directory: Downloads if readable, else primary storage, else app dir
    var currentDirectory by remember {
        val initial = storageLocations.firstOrNull { it.file.exists() && it.file.canRead() }?.file
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        mutableStateOf(initial)
    }

    var directoryFolders by remember { mutableStateOf<List<File>>(emptyList()) }
    var directoryFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var filterOnlyTextAndKeys by remember { mutableStateOf(false) }

    var showCreateSection by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    // Load items in current directory
    LaunchedEffect(currentDirectory, searchQuery, filterOnlyTextAndKeys, reloadTrigger, hasStoragePermission) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val list = currentDirectory.listFiles() ?: emptyArray()
                val q = searchQuery.trim()

                val filtered = if (q.isNotEmpty()) {
                    list.filter { it.name.contains(q, ignoreCase = true) }
                } else {
                    list.toList()
                }

                // Folders
                val folders = filtered
                    .filter { it.isDirectory && !it.name.startsWith(".") }
                    .sortedBy { it.name.lowercase(Locale.ROOT) }

                // Files
                val files = filtered
                    .filter { it.isFile && !it.name.startsWith(".") }
                    .filter { file ->
                        if (!filterOnlyTextAndKeys) true
                        else {
                            val lower = file.name.lowercase(Locale.ROOT)
                            lower.endsWith(".txt") || lower.endsWith(".cvault") || lower.endsWith(".cenc") ||
                            lower.endsWith(".enc") || lower.endsWith(".json") || lower.endsWith(".b64") ||
                            lower.endsWith(".dat") || lower.endsWith(".log") || lower.endsWith(".csv") ||
                            lower.endsWith(".md") || lower.endsWith(".xml") || lower.endsWith(".key")
                        }
                    }
                    .sortedByDescending { it.lastModified() }

                withContext(Dispatchers.Main) {
                    directoryFolders = folders
                    directoryFiles = files
                    isLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    directoryFolders = emptyList()
                    directoryFiles = emptyList()
                    isLoading = false
                }
            }
        }
    }

    val selectAndLoadFile: (File) -> Unit = { file ->
        if (onFileSelected != null) {
            onFileSelected(file)
            onDismiss()
        } else {
            viewModel.loadTextFromLocalFile(file, isForDecrypt = isForDecrypt)
            Toast.makeText(context, "Loading ${file.name}...", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF060D09),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isForDecrypt) "File & Folder Explorer (Decrypt)" else "File & Folder Explorer (Encrypt)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Browse folders & select files directly from storage",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                // Storage Permission Alert if full storage permission is not granted
                if (!hasStoragePermission) {
                    Surface(
                        color = Color(0xFF231808),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Storage access needed for all folders",
                                color = CyberAmber,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CyberSecondaryButton(
                                text = "Grant Access",
                                icon = Icons.Default.Check,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        StoragePermissionHelper.requestStoragePermission(context)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                },
                                accentColor = CyberAmber,
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                // Quick Action Buttons Row (System Picker + Paste Clip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // System Picker
                    CyberSecondaryButton(
                        text = "System Picker",
                        icon = Icons.Default.Folder,
                        onClick = {
                            onDismiss()
                            onLaunchSystemPicker()
                        },
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        testTag = "dialog_system_picker_btn"
                    )

                    // Paste Clip
                    CyberSecondaryButton(
                        text = "Paste Clipboard",
                        icon = Icons.Default.ContentPaste,
                        onClick = {
                            clipboardManager.getText()?.text?.let { text ->
                                if (text.isNotBlank()) {
                                    if (isForDecrypt) {
                                        viewModel.onDecryptInputChanged(text.trim())
                                    } else {
                                        viewModel.onEncryptInputChanged(text)
                                    }
                                    Toast.makeText(context, "Text loaded from clipboard", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        accentColor = MatrixGreenPrimary,
                        modifier = Modifier.weight(1f),
                        testTag = "dialog_paste_clip_btn"
                    )
                }

                // Demo encrypted text button for quick decrypt testing
                if (isForDecrypt && onFileSelected == null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberSecondaryButton(
                        text = "Load Test Encrypted Demo",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            viewModel.loadDemoEncryptedPayload()
                            Toast.makeText(context, "Encrypted demo loaded (Password: DemoPassword#2026)", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        accentColor = CyberAmber,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dialog_demo_cipher_btn"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Storage Locations Quick Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    storageLocations.forEach { loc ->
                        val isSelected = currentDirectory.absolutePath.startsWith(loc.file.absolutePath)
                        Surface(
                            color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF0F1B13),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accentColor else MatrixBorderNeon.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.clickable {
                                currentDirectory = loc.file
                                searchQuery = ""
                                showSearchField = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = loc.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accentColor else Color.LightGray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = loc.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Directory Navigation Header Bar
                Surface(
                    color = Color(0xFF0A150E),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MatrixBorderNeon.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val canGoUp = currentDirectory.parentFile != null &&
                                currentDirectory.parentFile?.canRead() == true &&
                                currentDirectory.path != "/" &&
                                currentDirectory.path != "/storage"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    currentDirectory.parentFile?.let {
                                        currentDirectory = it
                                        searchQuery = ""
                                        showSearchField = false
                                    }
                                },
                                enabled = canGoUp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Up",
                                    tint = if (canGoUp) accentColor else Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentDirectory.name.ifBlank { "Storage" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Filter toggle
                            IconButton(
                                onClick = { filterOnlyTextAndKeys = !filterOnlyTextAndKeys },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (filterOnlyTextAndKeys) CyberAmber else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Search toggle
                            IconButton(
                                onClick = {
                                    showSearchField = !showSearchField
                                    if (!showSearchField) searchQuery = ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (showSearchField || searchQuery.isNotBlank()) CyberCyan else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Refresh
                            IconButton(
                                onClick = { reloadTrigger++ },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Search field (expandable)
                AnimatedVisibility(visible = showSearchField) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search in current folder...", color = Color.Gray, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = MatrixBorderNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // List of Folders and Files
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 160.dp, max = 260.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accentColor,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading directory contents...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                                )
                            }
                        }
                    } else if (directoryFolders.isEmpty() && directoryFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching files or folders found"
                                    else "This folder is empty or restricted.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                )
                                if (currentDirectory.parentFile != null && currentDirectory.parentFile?.canRead() == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CyberSecondaryButton(
                                        text = "Go to Parent Folder",
                                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                                        onClick = {
                                            currentDirectory.parentFile?.let { currentDirectory = it }
                                        },
                                        accentColor = accentColor,
                                        modifier = Modifier.height(32.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Folders section
                            items(directoryFolders, key = { "dir_${it.absolutePath}" }) { folder ->
                                FolderRowItem(
                                    folder = folder,
                                    accentColor = CyberCyan,
                                    onClick = {
                                        currentDirectory = folder
                                        searchQuery = ""
                                        showSearchField = false
                                    }
                                )
                            }

                            // Files section
                            items(directoryFiles, key = { "file_${it.absolutePath}" }) { file ->
                                FileRowItem(
                                    file = file,
                                    accentColor = accentColor,
                                    onClick = { selectAndLoadFile(file) }
                                )
                            }
                        }
                    }
                }

                // Section to create a new file in current directory
                Spacer(modifier = Modifier.height(6.dp))
                CyberSecondaryButton(
                    text = if (showCreateSection) "Close New File Editor" else "➕ Create New File in Folder",
                    icon = if (showCreateSection) Icons.Default.Close else Icons.Default.Add,
                    onClick = { showCreateSection = !showCreateSection },
                    accentColor = MatrixGreenPrimary,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    testTag = "dialog_toggle_create_file_btn"
                )

                AnimatedVisibility(visible = showCreateSection) {
                    Surface(
                        color = Color(0xFF0C1710),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MatrixBorderNeon),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            OutlinedTextField(
                                value = newFileName,
                                onValueChange = { newFileName = it },
                                placeholder = { Text("File name (e.g. secret.txt or key.cvault)", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = MatrixBorderNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newFileContent,
                                onValueChange = { newFileContent = it },
                                placeholder = { Text("Enter text content for file here...", color = Color.Gray, fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp, max = 90.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = MatrixBorderNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CyberSecondaryButton(
                                text = "Save to Folder & Select",
                                icon = Icons.Default.Save,
                                onClick = {
                                    if (newFileContent.isNotBlank()) {
                                        try {
                                            val name = if (newFileName.isBlank()) "file_${System.currentTimeMillis()}.txt"
                                            else if (!newFileName.contains(".")) "$newFileName.txt"
                                            else newFileName
                                            val targetFile = File(currentDirectory, name)
                                            targetFile.writeText(newFileContent, Charsets.UTF_8)
                                            reloadTrigger++
                                            selectAndLoadFile(targetFile)
                                            Toast.makeText(context, "File $name saved and selected", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error saving file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "File content cannot be empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                accentColor = MatrixGreenPrimary,
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun FolderRowItem(
    folder: File,
    accentColor: Color,
    onClick: () -> Unit
) {
    val subItemCount = remember(folder) {
        try {
            folder.listFiles()?.size ?: 0
        } catch (_: Exception) {
            0
        }
    }

    Surface(
        color = Color(0xFF09140D),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MatrixBorderNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$subItemCount items",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open folder",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FileRowItem(
    file: File,
    accentColor: Color,
    onClick: () -> Unit
) {
    val lowerName = remember(file.name) { file.name.lowercase(Locale.ROOT) }
    val isVault = lowerName.endsWith(".cvault") || lowerName.endsWith(".cenc") || lowerName.endsWith(".enc")
    val isText = lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".b64")

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(file.lastModified()) { dateFormat.format(Date(file.lastModified())) }
    val formattedSize = remember(file.length()) {
        val bytes = file.length()
        if (bytes < 1024) "$bytes B"
        else if (bytes < 1024 * 1024) String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    }

    val ext = remember(file.name) {
        val idx = file.name.lastIndexOf('.')
        if (idx != -1 && idx < file.name.length - 1) file.name.substring(idx + 1).uppercase(Locale.ROOT)
        else "FILE"
    }

    Surface(
        color = Color(0xFF0C160F),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (isVault) CyberAmber.copy(alpha = 0.7f) else MatrixBorderNeon.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isVault) Icons.Default.Lock else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isVault) CyberAmber else if (isText) accentColor else Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$formattedSize • $formattedDate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MatrixTextCode,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = if (isVault) CyberAmber.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (isVault) CyberAmber.copy(alpha = 0.6f) else accentColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = ext.take(6),
                    color = if (isVault) CyberAmber else accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}
