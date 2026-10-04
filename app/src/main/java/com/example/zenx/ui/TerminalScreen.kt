package com.example.zenx.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zenx.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeSession = viewModel.activeSession ?: return
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Auto-scroll to bottom on new lines
    LaunchedEffect(activeSession.lines.size, activeSession.isRunningCommand) {
        if (activeSession.lines.isNotEmpty()) {
            listState.animateScrollToItem(activeSession.lines.size - 1)
        }
    }

    if (activeSession.mode == TerminalMode.NANO_EDITOR && activeSession.editingFile != null) {
        NanoEditorView(
            file = activeSession.editingFile,
            initialContent = activeSession.editingContent,
            theme = uiState.theme,
            onSaveAndExit = { content -> viewModel.saveNanoFile(content) },
            onExitWithoutSaving = { viewModel.exitNanoWithoutSaving() }
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(uiState.theme.background),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ZEN X",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 17.sp,
                            color = uiState.theme.accent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = uiState.theme.accent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "aarch64",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = uiState.theme.accent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Quick Flint Button
                    IconButton(
                        onClick = { viewModel.handleQuickKey("FLINT") },
                        modifier = Modifier.testTag("flint_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Open Flint AI",
                            tint = uiState.theme.aiAccent
                        )
                    }

                    // Copy buffer
                    IconButton(
                        onClick = { viewModel.copyTerminalBuffer() },
                        modifier = Modifier.testTag("copy_buffer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy terminal output",
                            tint = uiState.theme.foreground
                        )
                    }

                    // Menu for themes, fonts, clear
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("terminal_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Terminal Options",
                                tint = uiState.theme.foreground
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Color Theme") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showThemeDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Increase Font (A+)") },
                                leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null) },
                                onClick = {
                                    viewModel.adjustFontSize(1f)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Decrease Font (A-)") },
                                leadingIcon = { Icon(Icons.Default.ZoomOut, contentDescription = null) },
                                onClick = {
                                    viewModel.adjustFontSize(-1f)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Terminal") },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.handleQuickKey("CLR")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Session Tab") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.createNewSession()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = uiState.theme.toolbarBg,
                    titleContentColor = uiState.theme.foreground
                )
            )
        },
        containerColor = uiState.theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(uiState.theme.background)
        ) {
            // Session Tabs Bar
            SessionTabsRow(
                sessions = uiState.sessions,
                activeSessionId = uiState.activeSessionId,
                theme = uiState.theme,
                onSelectSession = { viewModel.switchSession(it) },
                onCloseSession = { viewModel.closeSession(it) },
                onNewSession = { viewModel.createNewSession() }
            )

            // Main Terminal Output Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("terminal_output_list"),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(activeSession.lines, key = { it.id }) { line ->
                        TerminalLineItem(line = line, theme = uiState.theme, fontSize = uiState.fontSizeSp)
                    }

                    if (activeSession.isRunningCommand) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = uiState.theme.accent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "executing...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (uiState.fontSizeSp - 1).sp,
                                    color = uiState.theme.accent.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Autocomplete Suggestions Chip Row
            AnimatedVisibility(
                visible = uiState.suggestions.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(uiState.theme.toolbarBg)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.suggestions.take(8).forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = uiState.theme.background,
                            border = androidx.compose.foundation.BorderStroke(1.dp, uiState.theme.accent.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                viewModel.applyAutocomplete(suggestion)
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = uiState.theme.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Command Input Row
            Surface(
                color = uiState.theme.toolbarBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val promptPrefix = when (activeSession.mode) {
                        TerminalMode.FLINT_AI -> "flint> "
                        TerminalMode.PYTHON_REPL -> ">>> "
                        TerminalMode.NODE_REPL -> "> "
                        else -> "$"
                    }
                    val promptColor = when (activeSession.mode) {
                        TerminalMode.FLINT_AI -> uiState.theme.aiAccent
                        TerminalMode.PYTHON_REPL -> Color(0xFFFFD43B)
                        TerminalMode.NODE_REPL -> Color(0xFF68A063)
                        else -> uiState.theme.promptPath
                    }

                    Text(
                        text = promptPrefix,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = uiState.fontSizeSp.sp,
                        color = promptColor,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    BasicTextField(
                        value = activeSession.currentInput,
                        onValueChange = { viewModel.updateInput(it) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("terminal_input_field"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = uiState.fontSizeSp.sp,
                            color = uiState.theme.foreground
                        ),
                        cursorBrush = SolidColor(uiState.theme.accent),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                viewModel.submitCurrentCommand()
                            }
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = { viewModel.submitCurrentCommand() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Execute Command",
                            tint = uiState.theme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Termux-Style Quick Keys Row (ESC, TAB, CTRL, ALT, ~, /, -, |, ^C, UP, DOWN, CLR, FLINT)
            TermuxQuickKeysBar(
                theme = uiState.theme,
                onKeyPress = { key -> viewModel.handleQuickKey(key) }
            )
        }
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "Select Terminal Theme",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TerminalThemes.allThemes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.changeTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(theme.background, CircleShape)
                                    .border(1.dp, theme.accent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = theme.name,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (theme.name == uiState.theme.name) FontWeight.Bold else FontWeight.Normal,
                                color = if (theme.name == uiState.theme.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SessionTabsRow(
    sessions: List<TerminalSessionData>,
    activeSessionId: String,
    theme: TerminalTheme,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onNewSession: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.toolbarBg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sessions.forEach { session ->
            val isActive = session.id == activeSessionId
            val chipBg = if (isActive) theme.background else Color.Transparent
            val chipBorder = if (isActive) theme.accent else Color.Gray.copy(alpha = 0.3f)

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = chipBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onSelectSession(session.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) theme.accent else theme.foreground.copy(alpha = 0.7f)
                    )
                    if (sessions.size > 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close session",
                            tint = theme.foreground.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onCloseSession(session.id) }
                        )
                    }
                }
            }
        }

        // New Session Button
        IconButton(
            onClick = onNewSession,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new session",
                tint = theme.accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TerminalLineItem(
    line: TerminalLine,
    theme: TerminalTheme,
    fontSize: Float
) {
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.35f).sp
    )

    when (line.type) {
        LineType.PROMPT -> {
            val text = line.text
            val annotated = buildAnnotatedString {
                if (text.startsWith("zenx@android:")) {
                    withStyle(SpanStyle(color = theme.promptUser, fontWeight = FontWeight.Bold)) {
                        append("zenx@android")
                    }
                    withStyle(SpanStyle(color = theme.foreground)) {
                        append(":")
                    }
                    val rest = text.removePrefix("zenx@android:")
                    val pathPart = rest.substringBefore("$ ")
                    val cmdPart = rest.substringAfter("$ ", "")

                    withStyle(SpanStyle(color = theme.promptPath, fontWeight = FontWeight.Bold)) {
                        append(pathPart)
                    }
                    withStyle(SpanStyle(color = theme.accent, fontWeight = FontWeight.Bold)) {
                        append("$ ")
                    }
                    withStyle(SpanStyle(color = theme.foreground)) {
                        append(cmdPart)
                    }
                } else if (text.startsWith("flint> ")) {
                    withStyle(SpanStyle(color = theme.aiAccent, fontWeight = FontWeight.Bold)) {
                        append("flint> ")
                    }
                    withStyle(SpanStyle(color = theme.foreground)) {
                        append(text.removePrefix("flint> "))
                    }
                } else {
                    withStyle(SpanStyle(color = theme.accent, fontWeight = FontWeight.Bold)) {
                        append(text)
                    }
                }
            }
            Text(text = annotated, style = style)
        }

        LineType.AI_RESPONSE -> {
            Surface(
                color = theme.aiAccent.copy(alpha = 0.08f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Flint AI",
                            tint = theme.aiAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Flint AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = theme.aiAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = line.text,
                        style = style,
                        color = theme.foreground
                    )
                }
            }
        }

        LineType.ERROR -> {
            Text(
                text = line.text,
                style = style,
                color = theme.error,
                fontWeight = FontWeight.Medium
            )
        }

        LineType.SUCCESS -> {
            Text(
                text = line.text,
                style = style,
                color = theme.success,
                fontWeight = FontWeight.Medium
            )
        }

        LineType.INFO -> {
            Text(
                text = line.text,
                style = style,
                color = theme.accent
            )
        }

        LineType.SYSTEM -> {
            Text(
                text = line.text,
                style = style.copy(fontSize = (fontSize - 1).sp, lineHeight = fontSize.sp),
                color = theme.accent,
                fontWeight = FontWeight.Bold
            )
        }

        LineType.DIRECTORY -> {
            Text(
                text = line.text,
                style = style,
                color = Color(0xFF66D9EF),
                fontWeight = FontWeight.Bold
            )
        }

        else -> {
            Text(
                text = line.text,
                style = style,
                color = theme.foreground
            )
        }
    }
}

@Composable
fun TermuxQuickKeysBar(
    theme: TerminalTheme,
    onKeyPress: (String) -> Unit
) {
    val keys = listOf(
        "ESC", "TAB", "CTRL", "ALT",
        "~", "/", "-", "|",
        "^C", "UP", "DOWN", "CLR", "FLINT"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.toolbarBg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            val isFlint = key == "FLINT"
            val isCtrlC = key == "^C"
            val btnColor = when {
                isFlint -> theme.aiAccent.copy(alpha = 0.25f)
                isCtrlC -> theme.error.copy(alpha = 0.25f)
                else -> Color(0xFF21262D)
            }
            val textColor = when {
                isFlint -> theme.aiAccent
                isCtrlC -> theme.error
                else -> theme.foreground
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = btnColor,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, textColor.copy(alpha = 0.3f)),
                modifier = Modifier
                    .height(34.dp)
                    .clickable { onKeyPress(key) }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
