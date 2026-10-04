package com.example.zenx.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zenx.ai.FlintAiService
import com.example.zenx.engine.CommandResult
import com.example.zenx.engine.ZenTerminalEngine
import com.example.zenx.filesystem.ZenFileSystem
import com.example.zenx.model.*
import com.example.zenx.pkg.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class TerminalSessionData(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lines: List<TerminalLine> = emptyList(),
    val mode: TerminalMode = TerminalMode.SHELL,
    val history: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val currentInput: String = "",
    val editingFile: File? = null,
    val editingContent: String = "",
    val isRunningCommand: Boolean = false
)

data class TerminalUiState(
    val sessions: List<TerminalSessionData> = emptyList(),
    val activeSessionId: String = "",
    val theme: TerminalTheme = TerminalThemes.ZenDark,
    val fontSizeSp: Float = 13f,
    val suggestions: List<String> = emptyList()
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    val fileSystem = ZenFileSystem(application)
    val packageManager = PackageManager(application)
    val flintAi = FlintAiService()
    val engine = ZenTerminalEngine(application, fileSystem, packageManager, flintAi)

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        // Create initial session with welcome banner
        val initialSession = createInitialSession("Session 1")
        _uiState.value = TerminalUiState(
            sessions = listOf(initialSession),
            activeSessionId = initialSession.id,
            theme = TerminalThemes.ZenDark,
            fontSizeSp = 13f
        )
    }

    private fun createInitialSession(title: String): TerminalSessionData {
        val bannerLines = listOf(
            TerminalLine(AsciiArt.ZENX_BANNER, LineType.SYSTEM),
            TerminalLine(AsciiArt.ZENX_WELCOME, LineType.INFO),
            TerminalLine("Logged in as zenx@android (${android.os.Build.MODEL})", LineType.SUCCESS)
        )
        return TerminalSessionData(
            title = title,
            lines = bannerLines
        )
    }

    val activeSession: TerminalSessionData?
        get() = _uiState.value.sessions.find { it.id == _uiState.value.activeSessionId }
            ?: _uiState.value.sessions.firstOrNull()

    fun updateInput(newText: String) {
        val currentSessionId = _uiState.value.activeSessionId
        _uiState.update { state ->
            val updatedSessions = state.sessions.map { sess ->
                if (sess.id == currentSessionId) {
                    sess.copy(currentInput = newText, historyIndex = -1)
                } else sess
            }
            val suggestions = if (newText.isNotBlank()) engine.completeInput(newText) else emptyList()
            state.copy(sessions = updatedSessions, suggestions = suggestions)
        }
    }

    fun submitCurrentCommand() {
        val session = activeSession ?: return
        val rawInput = session.currentInput
        val mode = session.mode

        // Add prompt line to buffer
        val promptPrefix = when (mode) {
            TerminalMode.FLINT_AI -> "flint> "
            TerminalMode.PYTHON_REPL -> ">>> "
            TerminalMode.NODE_REPL -> "> "
            else -> "zenx@android:${fileSystem.getDisplayPath()}$ "
        }

        val promptLine = TerminalLine(promptPrefix + rawInput, LineType.PROMPT)
        val newHistory = if (rawInput.isNotBlank() && (session.history.isEmpty() || session.history.last() != rawInput)) {
            session.history + rawInput
        } else {
            session.history
        }

        // Update session state before command execution
        updateSession(session.id) {
            it.copy(
                lines = it.lines + promptLine,
                currentInput = "",
                history = newHistory,
                historyIndex = -1,
                isRunningCommand = true
            )
        }
        _uiState.update { it.copy(suggestions = emptyList()) }

        viewModelScope.launch {
            val result = engine.execute(rawInput, mode) { streamingLine ->
                // Streaming feedback
                updateSession(session.id) { current ->
                    current.copy(lines = current.lines + streamingLine)
                }
            }

            when (result) {
                is CommandResult.Clear -> {
                    updateSession(session.id) { it.copy(lines = emptyList(), isRunningCommand = false) }
                }
                is CommandResult.Output -> {
                    updateSession(session.id) { current ->
                        current.copy(lines = current.lines + result.lines, isRunningCommand = false)
                    }
                }
                is CommandResult.ModeChange -> {
                    updateSession(session.id) { current ->
                        current.copy(
                            mode = result.newMode,
                            lines = current.lines + result.initialOutput,
                            isRunningCommand = false
                        )
                    }
                }
                is CommandResult.OpenNano -> {
                    updateSession(session.id) { current ->
                        current.copy(
                            mode = TerminalMode.NANO_EDITOR,
                            editingFile = result.file,
                            editingContent = result.content,
                            isRunningCommand = false
                        )
                    }
                }
            }
        }
    }

    fun navigateHistory(delta: Int) {
        val session = activeSession ?: return
        if (session.history.isEmpty()) return

        val newIndex = if (session.historyIndex == -1) {
            if (delta < 0) session.history.size - 1 else -1
        } else {
            (session.historyIndex + delta).coerceIn(-1, session.history.size - 1)
        }

        val nextInput = if (newIndex in session.history.indices) {
            session.history[newIndex]
        } else {
            ""
        }

        updateSession(session.id) {
            it.copy(historyIndex = newIndex, currentInput = nextInput)
        }
    }

    fun applyAutocomplete(suggestion: String? = null) {
        val session = activeSession ?: return
        val text = session.currentInput
        if (suggestion != null) {
            val tokens = text.split(" ").toMutableList()
            if (tokens.isNotEmpty()) {
                tokens[tokens.lastIndex] = suggestion
                val completed = tokens.joinToString(" ")
                updateInput(completed + if (suggestion.endsWith("/")) "" else " ")
            } else {
                updateInput(suggestion + " ")
            }
            return
        }

        val completions = engine.completeInput(text)
        if (completions.size == 1) {
            val completion = completions.first()
            val tokens = text.split(" ").toMutableList()
            tokens[tokens.lastIndex] = completion
            val completed = tokens.joinToString(" ")
            updateInput(completed + if (completion.endsWith("/")) "" else " ")
        } else if (completions.size > 1) {
            // Print available completions to terminal like bash
            val joined = completions.joinToString("   ")
            updateSession(session.id) {
                it.copy(lines = it.lines + TerminalLine(joined, LineType.INFO))
            }
        }
    }

    fun handleQuickKey(key: String) {
        val session = activeSession ?: return
        when (key) {
            "TAB" -> applyAutocomplete()
            "UP" -> navigateHistory(-1)
            "DOWN" -> navigateHistory(1)
            "CLR" -> {
                updateSession(session.id) { it.copy(lines = emptyList()) }
            }
            "^C" -> {
                // Cancel current command or reset REPL
                updateSession(session.id) {
                    it.copy(
                        mode = TerminalMode.SHELL,
                        currentInput = "",
                        lines = it.lines + TerminalLine("^C", LineType.ERROR),
                        isRunningCommand = false
                    )
                }
            }
            "FLINT" -> {
                // Quickly launch Flint
                updateInput("pkg open flint")
                submitCurrentCommand()
            }
            "ESC" -> {
                updateInput("")
            }
            else -> {
                updateInput(session.currentInput + key)
            }
        }
    }

    fun createNewSession() {
        val count = _uiState.value.sessions.size + 1
        val newSess = createInitialSession("Session $count")
        _uiState.update {
            it.copy(
                sessions = it.sessions + newSess,
                activeSessionId = newSess.id
            )
        }
    }

    fun switchSession(sessionId: String) {
        _uiState.update { it.copy(activeSessionId = sessionId) }
    }

    fun closeSession(sessionId: String) {
        val currentSessions = _uiState.value.sessions
        if (currentSessions.size <= 1) {
            // Don't close the last session, just clear it
            updateSession(sessionId) {
                createInitialSession("Session 1")
            }
            return
        }
        val remaining = currentSessions.filter { it.id != sessionId }
        val nextActive = remaining.first().id
        _uiState.update {
            it.copy(sessions = remaining, activeSessionId = nextActive)
        }
    }

    fun saveNanoFile(content: String) {
        val session = activeSession ?: return
        val file = session.editingFile
        if (file != null) {
            file.writeText(content)
        }
        updateSession(session.id) {
            it.copy(
                mode = TerminalMode.SHELL,
                editingFile = null,
                editingContent = "",
                lines = it.lines + TerminalLine("Saved ${file?.name ?: "file"}. [${content.length} bytes]", LineType.SUCCESS)
            )
        }
    }

    fun exitNanoWithoutSaving() {
        val session = activeSession ?: return
        updateSession(session.id) {
            it.copy(
                mode = TerminalMode.SHELL,
                editingFile = null,
                editingContent = "",
                lines = it.lines + TerminalLine("Nano editor closed (discarded changes).", LineType.INFO)
            )
        }
    }

    fun changeTheme(theme: TerminalTheme) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun adjustFontSize(delta: Float) {
        _uiState.update {
            it.copy(fontSizeSp = (it.fontSizeSp + delta).coerceIn(10f, 20f))
        }
    }

    fun copyTerminalBuffer() {
        val session = activeSession ?: return
        val text = session.lines.joinToString("\n") { it.text }
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ZEN X Terminal Output", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun updateSession(sessionId: String, transform: (TerminalSessionData) -> TerminalSessionData) {
        _uiState.update { state ->
            val newSessions = state.sessions.map { s ->
                if (s.id == sessionId) transform(s) else s
            }
            state.copy(sessions = newSessions)
        }
    }
}
