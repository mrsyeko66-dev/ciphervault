package com.kafappstore.ciphervault.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kafappstore.ciphervault.crypto.CipherEngine
import com.kafappstore.ciphervault.data.AppSettingsState
import com.kafappstore.ciphervault.data.SettingsRepository
import com.kafappstore.ciphervault.data.db.CipherProject
import com.kafappstore.ciphervault.data.db.ProjectRepository
import com.kafappstore.ciphervault.network.TextDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EncryptUiState(
    val inputText: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isEncrypting: Boolean = false,
    val resultBase64: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class DecryptUiState(
    val inputBase64: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isDecrypting: Boolean = false,
    val resultPlaintext: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class FileStreamingUiState(
    val selectedFileUri: Uri? = null,
    val selectedFileName: String = "",
    val selectedFileSize: Long = 0L,
    val isStreaming: Boolean = false,
    val isDecryption: Boolean = false,
    val progressPercent: Float = 0f,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val speedMBs: Float = 0f,
    val password: String = "",
    val passwordVisible: Boolean = false,
    val selectedOutputExtension: String = "cvault", // cvault, cenc, enc, custom
    val customOutputExtension: String = "",
    val encryptResult: CipherEngine.StreamingResult? = null,
    val decryptResult: CipherEngine.StreamingDecryptedMetadata? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class EditorUiState(
    val activeProjectId: Long? = null,
    val title: String = "Untitled Draft",
    val content: String = "",
    val tag: String = "Security Note",
    val isDirty: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val isSearchVisible: Boolean = false,
    val message: String? = null
)

class CipherViewModel(
    private val settingsRepository: SettingsRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val settings: StateFlow<AppSettingsState> = settingsRepository.settings

    val allProjects: StateFlow<List<CipherProject>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _encryptState = MutableStateFlow(EncryptUiState())
    val encryptState: StateFlow<EncryptUiState> = _encryptState.asStateFlow()

    private val _decryptState = MutableStateFlow(DecryptUiState())
    val decryptState: StateFlow<DecryptUiState> = _decryptState.asStateFlow()

    private val _streamingState = MutableStateFlow(FileStreamingUiState())
    val streamingState: StateFlow<FileStreamingUiState> = _streamingState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    // Navigation target for direct switching (e.g. from editor to encrypt)
    private val _pendingNavigateTab = MutableStateFlow<Int?>(null)
    val pendingNavigateTab: StateFlow<Int?> = _pendingNavigateTab.asStateFlow()

    // Splash Screen State
    private val _isShowingSplash = MutableStateFlow(true)
    val isShowingSplash: StateFlow<Boolean> = _isShowingSplash.asStateFlow()

    fun finishSplash() {
        _isShowingSplash.value = false
    }

    fun triggerSplashReplay() {
        _isShowingSplash.value = true
    }

    // URL Dialog State
    private val _urlDialogVisible = MutableStateFlow(false)
    val urlDialogVisible: StateFlow<Boolean> = _urlDialogVisible.asStateFlow()

    private val _urlInput = MutableStateFlow("https://")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isDownloadingUrl = MutableStateFlow(false)
    val isDownloadingUrl: StateFlow<Boolean> = _isDownloadingUrl.asStateFlow()

    private val _urlErrorMessage = MutableStateFlow<String?>(null)
    val urlErrorMessage: StateFlow<String?> = _urlErrorMessage.asStateFlow()

    private var autoClearJob: Job? = null

    fun clearPendingNavigation() {
        _pendingNavigateTab.value = null
    }

    // ==========================================
    // --- Text Encrypt Handlers ---
    // ==========================================

    fun onEncryptInputChanged(text: String) {
        _encryptState.value = _encryptState.value.copy(
            inputText = text,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onEncryptPasswordChanged(pw: String) {
        val trimmed = if (pw.length > 22) pw.substring(0, 22) else pw
        _encryptState.value = _encryptState.value.copy(
            password = trimmed,
            errorMessage = null
        )
    }

    fun toggleEncryptPasswordVisibility() {
        _encryptState.value = _encryptState.value.copy(
            passwordVisible = !_encryptState.value.passwordVisible
        )
    }

    fun clearEncrypt() {
        _encryptState.value = EncryptUiState()
    }

    fun executeEncrypt() {
        val currentState = _encryptState.value
        val text = currentState.inputText
        val password = currentState.password

        if (text.isBlank()) {
            _encryptState.value = currentState.copy(errorMessage = "Please enter text to encrypt.")
            return
        }

        if (password.length !in 12..22) {
            _encryptState.value = currentState.copy(errorMessage = "Password length must be between 12 and 22 characters.")
            return
        }

        _encryptState.value = currentState.copy(isEncrypting = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.Default) {
            val pepper = settings.value.pepper
            val result = CipherEngine.encrypt(
                plaintext = text,
                password = password,
                pepper = pepper
            )

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { base64 ->
                        _encryptState.value = _encryptState.value.copy(
                            isEncrypting = false,
                            resultBase64 = base64,
                            errorMessage = null,
                            successMessage = "3-Layer encryption completed successfully."
                        )
                    },
                    onFailure = { ex ->
                        _encryptState.value = _encryptState.value.copy(
                            isEncrypting = false,
                            errorMessage = ex.message ?: "Encryption error"
                        )
                    }
                )
            }
        }
    }

    // ==========================================
    // --- Text Decrypt Handlers ---
    // ==========================================

    fun onDecryptInputChanged(text: String) {
        _decryptState.value = _decryptState.value.copy(
            inputBase64 = text,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onDecryptPasswordChanged(pw: String) {
        val trimmed = if (pw.length > 22) pw.substring(0, 22) else pw
        _decryptState.value = _decryptState.value.copy(
            password = trimmed,
            errorMessage = null
        )
    }

    fun toggleDecryptPasswordVisibility() {
        _decryptState.value = _decryptState.value.copy(
            passwordVisible = !_decryptState.value.passwordVisible
        )
    }

    fun clearDecrypt() {
        autoClearJob?.cancel()
        _decryptState.value = DecryptUiState()
    }

    fun executeDecrypt() {
        val currentState = _decryptState.value
        val base64 = currentState.inputBase64.trim()
        val password = currentState.password

        if (base64.isBlank()) {
            _decryptState.value = currentState.copy(errorMessage = "Please enter the encrypted Base64 ciphertext.")
            return
        }

        if (password.isEmpty()) {
            _decryptState.value = currentState.copy(errorMessage = "Please enter the password.")
            return
        }

        _decryptState.value = currentState.copy(isDecrypting = true, errorMessage = null, resultPlaintext = null)

        viewModelScope.launch(Dispatchers.Default) {
            val pepper = settings.value.pepper
            val result = CipherEngine.decrypt(
                base64Payload = base64,
                password = password,
                pepper = pepper
            )

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { plaintext ->
                        _decryptState.value = _decryptState.value.copy(
                            isDecrypting = false,
                            resultPlaintext = plaintext,
                            errorMessage = null,
                            successMessage = "Decryption successful! Data verified and recovered."
                        )

                        if (settings.value.autoClearMemory) {
                            scheduleAutoClear()
                        }
                    },
                    onFailure = { ex ->
                        _decryptState.value = _decryptState.value.copy(
                            isDecrypting = false,
                            resultPlaintext = null,
                            errorMessage = ex.message ?: "Incorrect password or corrupted data."
                        )
                    }
                )
            }
        }
    }

    private fun scheduleAutoClear() {
        autoClearJob?.cancel()
        autoClearJob = viewModelScope.launch {
            delay(90_000)
            _decryptState.value = _decryptState.value.copy(
                resultPlaintext = null,
                successMessage = "Decrypted text automatically purged from memory after 90 seconds."
            )
        }
    }

    // ==========================================
    // --- Large File Streaming Handlers (1GB+) ---
    // ==========================================

    fun selectFileForStreaming(
        uri: Uri,
        name: String,
        size: Long,
        isDecryption: Boolean
    ) {
        _streamingState.value = _streamingState.value.copy(
            selectedFileUri = uri,
            selectedFileName = name,
            selectedFileSize = size,
            isDecryption = isDecryption,
            progressPercent = 0f,
            bytesProcessed = 0L,
            totalBytes = size,
            speedMBs = 0f,
            encryptResult = null,
            decryptResult = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onStreamingPasswordChanged(pw: String) {
        val trimmed = if (pw.length > 22) pw.substring(0, 22) else pw
        _streamingState.value = _streamingState.value.copy(
            password = trimmed,
            errorMessage = null
        )
    }

    fun toggleStreamingPasswordVisibility() {
        _streamingState.value = _streamingState.value.copy(
            passwordVisible = !_streamingState.value.passwordVisible
        )
    }

    fun setStreamingOutputExtension(ext: String) {
        _streamingState.value = _streamingState.value.copy(
            selectedOutputExtension = ext
        )
    }

    fun setCustomOutputExtension(ext: String) {
        _streamingState.value = _streamingState.value.copy(
            customOutputExtension = ext
        )
    }

    fun clearStreamingState() {
        _streamingState.value = FileStreamingUiState()
    }

    fun getEffectiveOutputExtension(): String {
        return if (_streamingState.value.selectedOutputExtension == "custom") {
            _streamingState.value.customOutputExtension.trim().removePrefix(".").ifEmpty { "cvault" }
        } else {
            _streamingState.value.selectedOutputExtension
        }
    }

    fun startStreamingEncryption(
        targetUri: Uri,
        contentResolver: ContentResolver
    ) {
        val st = _streamingState.value
        val sourceUri = st.selectedFileUri ?: return
        val password = st.password
        val originalName = st.selectedFileName
        val ext = originalName.substringAfterLast('.', "bin")
        val effectiveOutExt = getEffectiveOutputExtension()

        if (password.length !in 12..22) {
            _streamingState.value = st.copy(errorMessage = "Password length must be between 12 and 22 characters.")
            return
        }

        _streamingState.value = st.copy(
            isStreaming = true,
            progressPercent = 0f,
            bytesProcessed = 0L,
            errorMessage = null,
            encryptResult = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            var lastTime = System.currentTimeMillis()
            var lastBytes = 0L

            try {
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                        val result = CipherEngine.encryptStream(
                            inputStream = inputStream,
                            outputStream = outputStream,
                            password = password,
                            pepper = settings.value.pepper,
                            originalFileName = originalName,
                            originalExtension = ext,
                            totalBytes = st.selectedFileSize
                        ) { processed, total, pct ->
                            val now = System.currentTimeMillis()
                            val dt = (now - lastTime).coerceAtLeast(1)
                            if (dt >= 400 || pct >= 1f) {
                                val speed = ((processed - lastBytes).toFloat() / (dt / 1000f)) / (1024f * 1024f)
                                lastTime = now
                                lastBytes = processed
                                _streamingState.value = _streamingState.value.copy(
                                    bytesProcessed = processed,
                                    totalBytes = total,
                                    progressPercent = pct,
                                    speedMBs = speed.coerceAtLeast(0f)
                                )
                            }
                        }

                        withContext(Dispatchers.Main) {
                            result.fold(
                                onSuccess = { res ->
                                    val finalRes = res.copy(outputExtension = effectiveOutExt)
                                    _streamingState.value = _streamingState.value.copy(
                                        isStreaming = false,
                                        progressPercent = 1f,
                                        encryptResult = finalRes,
                                        errorMessage = null,
                                        successMessage = "Large file (${formatFileSize(res.bytesProcessed)}) encrypted and saved successfully."
                                    )
                                },
                                onFailure = { ex ->
                                    _streamingState.value = _streamingState.value.copy(
                                        isStreaming = false,
                                        errorMessage = ex.message ?: "Streaming file encryption error"
                                    )
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _streamingState.value = _streamingState.value.copy(
                        isStreaming = false,
                        errorMessage = "File I/O error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun startStreamingDecryption(
        targetUri: Uri,
        contentResolver: ContentResolver
    ) {
        val st = _streamingState.value
        val sourceUri = st.selectedFileUri ?: return
        val password = st.password

        if (password.isEmpty()) {
            _streamingState.value = st.copy(errorMessage = "Please enter the password.")
            return
        }

        _streamingState.value = st.copy(
            isStreaming = true,
            progressPercent = 0f,
            bytesProcessed = 0L,
            errorMessage = null,
            decryptResult = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            var lastTime = System.currentTimeMillis()
            var lastBytes = 0L

            try {
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                        val result = CipherEngine.decryptStream(
                            inputStream = inputStream,
                            outputStream = outputStream,
                            password = password,
                            pepper = settings.value.pepper,
                            totalBytes = st.selectedFileSize
                        ) { processed, total, pct ->
                            val now = System.currentTimeMillis()
                            val dt = (now - lastTime).coerceAtLeast(1)
                            if (dt >= 400 || pct >= 1f) {
                                val speed = ((processed - lastBytes).toFloat() / (dt / 1000f)) / (1024f * 1024f)
                                lastTime = now
                                lastBytes = processed
                                _streamingState.value = _streamingState.value.copy(
                                    bytesProcessed = processed,
                                    totalBytes = total,
                                    progressPercent = pct,
                                    speedMBs = speed.coerceAtLeast(0f)
                                )
                            }
                        }

                        withContext(Dispatchers.Main) {
                            result.fold(
                                onSuccess = { meta ->
                                    _streamingState.value = _streamingState.value.copy(
                                        isStreaming = false,
                                        progressPercent = 1f,
                                        decryptResult = meta,
                                        errorMessage = null,
                                        successMessage = "File decrypted successfully: ${meta.originalFileName}"
                                    )
                                },
                                onFailure = { ex ->
                                    _streamingState.value = _streamingState.value.copy(
                                        isStreaming = false,
                                        errorMessage = ex.message ?: "Incorrect password or corrupted file."
                                    )
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _streamingState.value = _streamingState.value.copy(
                        isStreaming = false,
                        errorMessage = "File read error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // ==========================================
    // --- Advanced Text Editor Handlers ---
    // ==========================================

    fun onEditorTitleChanged(title: String) {
        _editorState.value = _editorState.value.copy(title = title, isDirty = true)
    }

    fun onEditorContentChanged(content: String) {
        _editorState.value = _editorState.value.copy(content = content, isDirty = true)
    }

    fun onEditorTagChanged(tag: String) {
        _editorState.value = _editorState.value.copy(tag = tag, isDirty = true)
    }

    fun toggleEditorSearch() {
        val current = _editorState.value.isSearchVisible
        _editorState.value = _editorState.value.copy(isSearchVisible = !current)
    }

    fun onSearchQueryChanged(q: String) {
        _editorState.value = _editorState.value.copy(searchQuery = q)
    }

    fun onReplaceQueryChanged(r: String) {
        _editorState.value = _editorState.value.copy(replaceQuery = r)
    }

    fun executeReplaceAll() {
        val st = _editorState.value
        if (st.searchQuery.isEmpty()) return
        val newContent = st.content.replace(st.searchQuery, st.replaceQuery)
        _editorState.value = st.copy(content = newContent, isDirty = true, message = "Replacement completed successfully.")
    }

    fun insertEditorTimestamp() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val stamp = "\n[TIMESTAMP: ${sdf.format(Date())}]\n"
        val newContent = _editorState.value.content + stamp
        _editorState.value = _editorState.value.copy(content = newContent, isDirty = true)
    }

    fun insertEditorSecurityHeader() {
        val header = "\n--- CLASSIFIED: TOP SECRET // CIPHERVAULT ENCRYPTION SUITE ---\n"
        val newContent = header + _editorState.value.content
        _editorState.value = _editorState.value.copy(content = newContent, isDirty = true)
    }

    fun insertEditorDivider() {
        val div = "\n------------------------------------------------------------\n"
        val newContent = _editorState.value.content + div
        _editorState.value = _editorState.value.copy(content = newContent, isDirty = true)
    }

    fun clearEditorContent() {
        _editorState.value = _editorState.value.copy(
            activeProjectId = null,
            title = "Untitled Draft",
            content = "",
            isDirty = false,
            message = "Editor workspace cleared."
        )
    }

    fun saveEditorProject(onComplete: ((Long) -> Unit)? = null) {
        val st = _editorState.value
        val title = st.title.ifBlank { "Untitled Project" }
        viewModelScope.launch(Dispatchers.IO) {
            val proj = CipherProject(
                id = st.activeProjectId ?: 0L,
                title = title,
                content = st.content,
                tag = st.tag,
                updatedAt = System.currentTimeMillis()
            )
            val newId = if (st.activeProjectId == null || st.activeProjectId == 0L) {
                projectRepository.insertProject(proj)
            } else {
                projectRepository.updateProject(proj)
                st.activeProjectId!!
            }
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    activeProjectId = newId,
                    isDirty = false,
                    message = "Project saved to local database successfully."
                )
                onComplete?.invoke(newId)
            }
        }
    }

    fun sendEditorToEncrypt() {
        val text = _editorState.value.content
        _encryptState.value = _encryptState.value.copy(inputText = text)
        _pendingNavigateTab.value = 0 // Navigate to Terminal Encrypt
    }

    // ==========================================
    // --- Room Database Projects Management ---
    // ==========================================

    fun loadProjectIntoEditor(project: CipherProject) {
        _editorState.value = EditorUiState(
            activeProjectId = project.id,
            title = project.title,
            content = project.content,
            tag = project.tag,
            isDirty = false
        )
    }

    fun deleteProject(project: CipherProject) {
        viewModelScope.launch(Dispatchers.IO) {
            projectRepository.deleteProject(project)
            if (_editorState.value.activeProjectId == project.id) {
                withContext(Dispatchers.Main) {
                    clearEditorContent()
                }
            }
        }
    }

    fun encryptProjectDirectly(project: CipherProject) {
        _encryptState.value = _encryptState.value.copy(
            inputText = project.content,
            errorMessage = null,
            successMessage = "Project '${project.title}' content loaded into encryption workspace."
        )
        _pendingNavigateTab.value = 0
    }

    fun saveDecryptedTextAsProject(title: String, tag: String = "Decrypted Text", onComplete: (Long) -> Unit) {
        val plain = _decryptState.value.resultPlaintext ?: return
        val cleanTitle = title.ifBlank { "Decrypted Data [${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}]" }
        viewModelScope.launch(Dispatchers.IO) {
            val project = CipherProject(
                title = cleanTitle,
                content = plain,
                tag = tag,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = projectRepository.insertProject(project)
            withContext(Dispatchers.Main) {
                onComplete(id)
            }
        }
    }

    fun openDecryptedInEditor() {
        val plain = _decryptState.value.resultPlaintext ?: return
        _editorState.value = EditorUiState(
            activeProjectId = null,
            title = "Decrypted [${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}]",
            content = plain,
            tag = "Recovered",
            isDirty = true
        )
    }

    // ==========================================
    // --- File Info Query & Helpers ---
    // ==========================================

    fun queryFileInfo(uri: Uri, contentResolver: ContentResolver): Pair<String, Long> {
        var name = "unknown_file"
        var size = 0L
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}
        return Pair(name, size)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    // ==========================================
    // --- Text File SAF & Network I/O ---
    // ==========================================

    fun loadTextFromFile(uri: Uri, contentResolver: ContentResolver, isForDecrypt: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                    val content = reader.readText()
                    withContext(Dispatchers.Main) {
                        if (isForDecrypt) {
                            _decryptState.value = _decryptState.value.copy(
                                inputBase64 = content.trim(),
                                errorMessage = null,
                                successMessage = "File loaded successfully."
                            )
                        } else {
                            _encryptState.value = _encryptState.value.copy(
                                inputText = content,
                                errorMessage = null,
                                successMessage = "Text file (${content.length} chars) loaded successfully."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Error reading file: ${e.localizedMessage ?: "Invalid format"}"
                    if (isForDecrypt) {
                        _decryptState.value = _decryptState.value.copy(errorMessage = msg)
                    } else {
                        _encryptState.value = _encryptState.value.copy(errorMessage = msg)
                    }
                }
            }
        }
    }

    fun saveContentToFile(uri: Uri, content: String, contentResolver: ContentResolver, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    val writer = OutputStreamWriter(stream, Charsets.UTF_8)
                    writer.write(content)
                    writer.flush()
                }
                withContext(Dispatchers.Main) {
                    onComplete(true, "File saved successfully.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Error saving file: ${e.localizedMessage}")
                }
            }
        }
    }

    // ==========================================
    // --- URL Download Dialog ---
    // ==========================================

    fun openUrlDialog() {
        _urlErrorMessage.value = null
        _urlDialogVisible.value = true
    }

    fun closeUrlDialog() {
        _urlDialogVisible.value = false
    }

    fun onUrlInputChanged(url: String) {
        _urlInput.value = url
        _urlErrorMessage.value = null
    }

    fun downloadUrlContent() {
        val url = _urlInput.value
        _isDownloadingUrl.value = true
        _urlErrorMessage.value = null

        viewModelScope.launch {
            val result = TextDownloader.downloadTextFromUrl(url)
            _isDownloadingUrl.value = false
            result.fold(
                onSuccess = { content ->
                    _urlDialogVisible.value = false
                    _encryptState.value = _encryptState.value.copy(
                        inputText = content,
                        errorMessage = null,
                        successMessage = "Online content (${content.length} chars) downloaded successfully."
                    )
                },
                onFailure = { ex ->
                    _urlErrorMessage.value = ex.message ?: "Error downloading online resource"
                }
            )
        }
    }

    // ==========================================
    // --- Settings Passthrough ---
    // ==========================================

    fun updatePepper(newPepper: String) = settingsRepository.updatePepper(newPepper)
    fun resetPepper() = settingsRepository.resetPepperToDefault()
    fun setAutoClear(enabled: Boolean) = settingsRepository.setAutoClearMemory(enabled)
    fun setThreshold(threshold: Int) = settingsRepository.setOutputThreshold(threshold)
    fun setTheme(mode: com.kafappstore.ciphervault.data.CyberThemeMode) = settingsRepository.setThemeMode(mode)

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val projectRepository: ProjectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CipherViewModel(settingsRepository, projectRepository) as T
        }
    }
}
