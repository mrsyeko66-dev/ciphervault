package com.kafappstore.ciphervault.viewmodel

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
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
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EncryptUiState(
    val inputText: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val useSecretKey: Boolean = true, // Toggle for Standalone mode (without secret key)
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
    val useSecretKey: Boolean = true, // Toggle for Standalone file encryption
    val selectedOutputExtension: String = "cvault", // cvault, cenc, enc, custom
    val customOutputExtension: String = "",
    val encryptResult: CipherEngine.StreamingResult? = null,
    val decryptResult: CipherEngine.StreamingDecryptedMetadata? = null,
    val lastSavedTargetUri: Uri? = null,
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
    private var streamingJob: Job? = null
    @Volatile
    private var isStreamingCancelled: Boolean = false
    private var activeStreamingTargetUri: Uri? = null
    private var activeStreamingContentResolver: ContentResolver? = null

    fun cancelStreamingOperation() {
        if (!_streamingState.value.isStreaming) return
        isStreamingCancelled = true
        streamingJob?.cancel()
        val targetUri = activeStreamingTargetUri
        val cr = activeStreamingContentResolver
        if (targetUri != null && cr != null) {
            deleteIncompleteFile(targetUri, cr)
        }
        _streamingState.value = _streamingState.value.copy(
            isStreaming = false,
            speedMBs = 0f,
            errorMessage = "Streaming operation aborted by user. Incomplete temporary file rolled back and removed."
        )
    }

    private fun deleteIncompleteFile(uri: Uri, contentResolver: ContentResolver) {
        try {
            if (DocumentsContract.isDocumentUri(null, uri)) {
                DocumentsContract.deleteDocument(contentResolver, uri)
            } else {
                contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {
            // Attempt overwrite with empty content if direct delete is unsupported
            try {
                contentResolver.openOutputStream(uri, "wt")?.use { /* truncated to 0 */ }
            } catch (_: Exception) {}
        }
    }

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
        val trimmed = if (pw.length > 64) pw.substring(0, 64) else pw
        _encryptState.value = _encryptState.value.copy(
            password = trimmed,
            errorMessage = null
        )
    }

    fun toggleEncryptUseSecretKey(enabled: Boolean) {
        _encryptState.value = _encryptState.value.copy(
            useSecretKey = enabled,
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
        val useSecret = currentState.useSecretKey

        if (text.isBlank()) {
            _encryptState.value = currentState.copy(errorMessage = "Please enter text to encrypt.")
            return
        }

        if (!useSecret) {
            val strength = CipherEngine.evaluatePasswordStrength(password)
            if (!strength.first) {
                _encryptState.value = currentState.copy(
                    errorMessage = "Standalone encryption (No secret key) requires a high-entropy password:\n" + strength.third.joinToString("\n• ", prefix = "• ")
                )
                return
            }
        } else if (password.length !in 12..64) {
            _encryptState.value = currentState.copy(errorMessage = "Password length must be between 12 and 64 characters.")
            return
        }

        _encryptState.value = currentState.copy(isEncrypting = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.Default) {
            val pepper = if (useSecret) settings.value.pepper else ""
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
                            successMessage = if (useSecret) "3-Layer encryption completed successfully." else "Standalone encryption (No secret key) completed successfully."
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
        val trimmed = if (pw.length > 64) pw.substring(0, 64) else pw
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
        val trimmed = if (pw.length > 64) pw.substring(0, 64) else pw
        _streamingState.value = _streamingState.value.copy(
            password = trimmed,
            errorMessage = null
        )
    }

    fun toggleStreamingUseSecretKey(enabled: Boolean) {
        _streamingState.value = _streamingState.value.copy(
            useSecretKey = enabled,
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

    fun inspectEncryptedFileForDecryption(contentResolver: ContentResolver): Result<CipherEngine.StreamingDecryptedMetadata> {
        val st = _streamingState.value
        val sourceUri = st.selectedFileUri ?: return Result.failure(IllegalStateException("No file selected"))
        val password = st.password
        return try {
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                CipherEngine.peekStreamMetadata(inputStream, password)
            } ?: Result.failure(IllegalStateException("Cannot open file input stream"))
        } catch (e: Exception) {
            Result.failure(e)
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
        val useSecret = st.useSecretKey

        if (!useSecret) {
            val strength = CipherEngine.evaluatePasswordStrength(password)
            if (!strength.first) {
                _streamingState.value = st.copy(
                    errorMessage = "Standalone file encryption (No secret key) requires a high-entropy password:\n" + strength.third.joinToString("\n• ", prefix = "• ")
                )
                return
            }
        } else if (password.length !in 12..64) {
            _streamingState.value = st.copy(errorMessage = "Password length must be between 12 and 64 characters.")
            return
        }

        _streamingState.value = st.copy(
            isStreaming = true,
            progressPercent = 0f,
            bytesProcessed = 0L,
            errorMessage = null,
            encryptResult = null
        )

        isStreamingCancelled = false
        activeStreamingTargetUri = targetUri
        activeStreamingContentResolver = contentResolver

        streamingJob = viewModelScope.launch(Dispatchers.IO) {
            var lastTime = System.currentTimeMillis()
            var lastBytes = 0L

            try {
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                        val result = CipherEngine.encryptStream(
                            inputStream = inputStream,
                            outputStream = outputStream,
                            password = password,
                            pepper = if (useSecret) settings.value.pepper else "",
                            originalFileName = originalName,
                            originalExtension = ext,
                            totalBytes = st.selectedFileSize,
                            isCancelled = { isStreamingCancelled }
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
                                        lastSavedTargetUri = targetUri,
                                        errorMessage = null,
                                        successMessage = "Large file (${formatFileSize(res.bytesProcessed)}) encrypted and saved successfully."
                                    )
                                },
                                onFailure = { ex ->
                                    // Automatic cleanup of incomplete file
                                    deleteIncompleteFile(targetUri, contentResolver)
                                    _streamingState.value = _streamingState.value.copy(
                                        isStreaming = false,
                                        errorMessage = if (isStreamingCancelled) {
                                            "Streaming operation cancelled. Incomplete file removed."
                                        } else {
                                            "Stream encryption error: ${ex.message ?: "Unknown"} (cleaned up incomplete file)"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Incomplete file cleanup
                deleteIncompleteFile(targetUri, contentResolver)
                withContext(Dispatchers.Main) {
                    _streamingState.value = _streamingState.value.copy(
                        isStreaming = false,
                        errorMessage = if (isStreamingCancelled) {
                            "Streaming operation cancelled. Incomplete file removed."
                        } else {
                            "File storage error: ${e.localizedMessage} (cleaned up incomplete file)"
                        }
                    )
                }
            } finally {
                activeStreamingTargetUri = null
                activeStreamingContentResolver = null
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

        isStreamingCancelled = false
        activeStreamingTargetUri = targetUri
        activeStreamingContentResolver = contentResolver

        streamingJob = viewModelScope.launch(Dispatchers.IO) {
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
                            totalBytes = st.selectedFileSize,
                            isCancelled = { isStreamingCancelled }
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
                                        lastSavedTargetUri = targetUri,
                                        errorMessage = null,
                                        successMessage = "File decrypted successfully: ${meta.originalFileName}"
                                    )
                                },
                                onFailure = { ex ->
                                    // Automatic cleanup of incomplete file
                                    deleteIncompleteFile(targetUri, contentResolver)
                                    _streamingState.value = _streamingState.value.copy(
                                        isStreaming = false,
                                        errorMessage = if (isStreamingCancelled) {
                                            "Streaming operation cancelled. Incomplete file removed."
                                        } else {
                                            ex.message ?: "Incorrect password or corrupted file."
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Incomplete file cleanup
                deleteIncompleteFile(targetUri, contentResolver)
                withContext(Dispatchers.Main) {
                    _streamingState.value = _streamingState.value.copy(
                        isStreaming = false,
                        errorMessage = if (isStreamingCancelled) {
                            "Streaming operation cancelled. Incomplete file removed."
                        } else {
                            "File I/O error: ${e.localizedMessage} (incomplete file removed)"
                        }
                    )
                }
            } finally {
                activeStreamingTargetUri = null
                activeStreamingContentResolver = null
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
    // --- Incoming Intent Handlers ---
    // ==========================================

    fun handleIncomingSharedText(text: String) {
        _encryptState.value = _encryptState.value.copy(
            inputText = text,
            errorMessage = null,
            successMessage = "Text loaded from external app."
        )
        _pendingNavigateTab.value = 0 // Terminal Encrypt tab
    }

    fun handleIncomingFileUri(uri: Uri, contentResolver: ContentResolver) {
        val (name, size) = queryFileInfo(uri, contentResolver)
        val lowerName = name.lowercase(Locale.ROOT)
        val isEncryptedFile = lowerName.endsWith(".cvault") || lowerName.endsWith(".cenc") || lowerName.endsWith(".enc")

        if (isEncryptedFile) {
            selectFileForStreaming(uri, name, size, isDecryption = true)
            _pendingNavigateTab.value = 1 // Terminal Decrypt tab
        } else {
            selectFileForStreaming(uri, name, size, isDecryption = false)
            _pendingNavigateTab.value = 0 // Terminal Encrypt tab
        }
    }

    // ==========================================
    // --- File Info Query & Helpers ---
    // ==========================================

    fun queryFileInfo(uri: Uri, contentResolver: ContentResolver): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "unknown_file"
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
                val (name, size) = queryFileInfo(uri, contentResolver)
                val lowerName = name.lowercase(Locale.ROOT)
                val isVaultExt = lowerName.endsWith(".cvault") || lowerName.endsWith(".cenc") || lowerName.endsWith(".enc")

                if (isForDecrypt && isVaultExt) {
                    withContext(Dispatchers.Main) {
                        selectFileForStreaming(uri, name, size, isDecryption = true)
                        _decryptState.value = _decryptState.value.copy(
                            successMessage = "Encrypted file ($name) detected. Switched to Streaming Decryption mode."
                        )
                    }
                    return@launch
                }

                contentResolver.openInputStream(uri)?.use { stream ->
                    val pushback = java.io.PushbackInputStream(stream, 4)
                    val header = ByteArray(4)
                    val bytesRead = pushback.read(header)
                    if (bytesRead == 4 && header.contentEquals(CipherEngine.STREAM_MAGIC) && isForDecrypt) {
                        withContext(Dispatchers.Main) {
                            selectFileForStreaming(uri, name, size, isDecryption = true)
                            _decryptState.value = _decryptState.value.copy(
                                successMessage = "Binary vault format detected. Switched to Streaming Decryption mode."
                            )
                        }
                        return@launch
                    }
                    if (bytesRead > 0) {
                        pushback.unread(header, 0, bytesRead)
                    }
                    val reader = BufferedReader(InputStreamReader(pushback, Charsets.UTF_8))
                    val content = reader.readText()
                    withContext(Dispatchers.Main) {
                        if (isForDecrypt) {
                            _decryptState.value = _decryptState.value.copy(
                                inputBase64 = content.trim(),
                                errorMessage = null,
                                successMessage = "File loaded: $name (${content.length} chars)"
                            )
                        } else {
                            _encryptState.value = _encryptState.value.copy(
                                inputText = content,
                                errorMessage = null,
                                successMessage = "Text file loaded: $name (${content.length} chars)"
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

    /**
     * Directly loads from a local storage File (e.g. from Downloads or App Storage directory).
     */
    fun loadTextFromLocalFile(file: File, isForDecrypt: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!file.exists() || !file.canRead()) {
                    withContext(Dispatchers.Main) {
                        val msg = "Cannot access file: ${file.name}"
                        if (isForDecrypt) _decryptState.value = _decryptState.value.copy(errorMessage = msg)
                        else _encryptState.value = _encryptState.value.copy(errorMessage = msg)
                    }
                    return@launch
                }
                val name = file.name
                val size = file.length()
                val lowerName = name.lowercase(Locale.ROOT)
                val isVaultExt = lowerName.endsWith(".cvault") || lowerName.endsWith(".cenc") || lowerName.endsWith(".enc")

                if (isForDecrypt && isVaultExt) {
                    val fileUri = Uri.fromFile(file)
                    withContext(Dispatchers.Main) {
                        selectFileForStreaming(fileUri, name, size, isDecryption = true)
                        _decryptState.value = _decryptState.value.copy(
                            successMessage = "Encrypted file ($name) detected. Switched to Streaming Decryption mode."
                        )
                    }
                    return@launch
                }

                file.inputStream().use { stream ->
                    val pushback = java.io.PushbackInputStream(stream, 4)
                    val header = ByteArray(4)
                    val bytesRead = pushback.read(header)
                    if (bytesRead == 4 && header.contentEquals(CipherEngine.STREAM_MAGIC) && isForDecrypt) {
                        val fileUri = Uri.fromFile(file)
                        withContext(Dispatchers.Main) {
                            selectFileForStreaming(fileUri, name, size, isDecryption = true)
                            _decryptState.value = _decryptState.value.copy(
                                successMessage = "Binary vault format detected. Switched to Streaming Decryption mode."
                            )
                        }
                        return@launch
                    }
                    if (bytesRead > 0) {
                        pushback.unread(header, 0, bytesRead)
                    }
                    val reader = BufferedReader(InputStreamReader(pushback, Charsets.UTF_8))
                    val content = reader.readText()
                    withContext(Dispatchers.Main) {
                        if (isForDecrypt) {
                            _decryptState.value = _decryptState.value.copy(
                                inputBase64 = content.trim(),
                                errorMessage = null,
                                successMessage = "File loaded: $name (${content.length} chars)"
                            )
                        } else {
                            _encryptState.value = _encryptState.value.copy(
                                inputText = content,
                                errorMessage = null,
                                successMessage = "Text file loaded: $name (${content.length} chars)"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Error reading local file: ${e.localizedMessage ?: "Invalid file"}"
                    if (isForDecrypt) _decryptState.value = _decryptState.value.copy(errorMessage = msg)
                    else _encryptState.value = _encryptState.value.copy(errorMessage = msg)
                }
            }
        }
    }

    /**
     * Loads a pre-verified encrypted demo payload with password filled for instant decryption verification.
     * Computes on Dispatchers.Default so the UI thread remains 100% responsive.
     */
    fun loadDemoEncryptedPayload() {
        viewModelScope.launch(Dispatchers.Default) {
            val demoText = "TOP SECRET // CYBERVAULT VERIFIED DECRYPTION: Operation Quantum Nexus is active. All security nodes online."
            val demoPass = "DemoPassword#2026"
            val encrypted = CipherEngine.encrypt(
                plaintext = demoText,
                password = demoPass,
                pepper = "ciphervault_pepper_v1"
            ).getOrNull() ?: ""

            withContext(Dispatchers.Main) {
                _decryptState.value = _decryptState.value.copy(
                    inputBase64 = encrypted,
                    password = demoPass,
                    errorMessage = null,
                    successMessage = "Demo encrypted payload loaded! Password filled. Tap 'Decrypt Terminal' to test."
                )
            }
        }
    }

    /**
     * Fast sample file preparation with zero CPU lag (no PBKDF2 derivations on UI thread).
     */
    fun ensureSampleFilesExist(context: Context) {
        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()

            val plainSampleFile = File(docsDir, "sample_quick_notes.txt")
            if (!plainSampleFile.exists() || plainSampleFile.length() == 0L) {
                plainSampleFile.writeText(
                    "CipherVault Secure Note:\n" +
                    "- AES-256-GCM authenticated encryption\n" +
                    "- Zero server tracking, 100% offline security.",
                    Charsets.UTF_8
                )
            }
        } catch (_: Exception) {}
    }

    /**
     * Saves user-entered text as a new file in local storage and returns the created File.
     */
    fun createLocalTextFile(context: Context, fileName: String, content: String): File? {
        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()
            val cleanName = if (fileName.isBlank()) "file_${System.currentTimeMillis()}.txt"
            else if (!fileName.contains(".")) "$fileName.txt"
            else fileName
            val file = File(docsDir, cleanName)
            file.writeText(content, Charsets.UTF_8)
            file
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Scans device and app directories for readable text or encrypted files.
     */
    fun getAvailableLocalFiles(context: Context): List<File> {
        ensureSampleFilesExist(context)
        val candidateDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(null),
            context.filesDir
        )
        val result = mutableListOf<File>()
        for (dir in candidateDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { file ->
                        file.isFile && file.canRead() && (
                            file.name.endsWith(".txt", ignoreCase = true) ||
                            file.name.endsWith(".cvault", ignoreCase = true) ||
                            file.name.endsWith(".cenc", ignoreCase = true) ||
                            file.name.endsWith(".enc", ignoreCase = true) ||
                            file.name.endsWith(".json", ignoreCase = true) ||
                            file.name.endsWith(".b64", ignoreCase = true) ||
                            file.name.endsWith(".dat", ignoreCase = true) ||
                            file.name.endsWith(".log", ignoreCase = true)
                        )
                    }?.let { result.addAll(it) }
                }
            } catch (_: Exception) {}
        }
        return result.distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
    }

    fun saveContentToFile(uri: Uri, content: String, contentResolver: ContentResolver, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                    ?: throw IOException("Cannot open output stream for selected file location.")
                outputStream.use { stream ->
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

    /**
     * Direct robust save to device's public Downloads directory.
     * Uses MediaStore.Downloads on Android 10+ (API 29+) without requiring runtime permissions.
     */
    fun saveContentToDownloads(
        context: Context,
        fileName: String,
        content: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw IOException("Failed to allocate file in Downloads folder.")
                    val outputStream = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("Failed to write to Downloads location.")
                    outputStream.use { stream ->
                        val writer = OutputStreamWriter(stream, Charsets.UTF_8)
                        writer.write(content)
                        writer.flush()
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val targetFile = File(downloadsDir, fileName)
                    targetFile.writeText(content, Charsets.UTF_8)
                }
                withContext(Dispatchers.Main) {
                    onComplete(true, "File saved to Downloads: $fileName")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Failed saving to Downloads: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Instant system share of text content.
     */
    fun shareTextContent(context: Context, content: String, title: String = "Share Encrypted Data") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_TITLE, title)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Log or ignore
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

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun initAppLock() {
        if (settings.value.biometricLockEnabled) {
            _isAppLocked.value = true
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (settings.value.biometricLockEnabled) {
            _isAppLocked.value = true
        }
    }

    fun updatePepper(newPepper: String) = settingsRepository.updatePepper(newPepper)
    fun resetPepper() = settingsRepository.resetPepperToDefault()
    fun setAutoClear(enabled: Boolean) = settingsRepository.setAutoClearMemory(enabled)
    fun setThreshold(threshold: Int) = settingsRepository.setOutputThreshold(threshold)
    fun setTheme(mode: com.kafappstore.ciphervault.data.CyberThemeMode) = settingsRepository.setThemeMode(mode)
    fun setBiometricLock(enabled: Boolean) = settingsRepository.setBiometricLockEnabled(enabled)
    fun setLockType(type: com.kafappstore.ciphervault.data.AppLockType) = settingsRepository.setLockType(type)
    fun savePasscode(passcode: String) = settingsRepository.savePasscode(passcode)
    fun verifyPasscode(passcode: String): Boolean = settingsRepository.verifyPasscode(passcode)
    fun clearPasscode() = settingsRepository.clearPasscode()
    fun setScreenSecurity(enabled: Boolean) = settingsRepository.setScreenSecurityEnabled(enabled)

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
