package com.example.zenx.model

import androidx.compose.ui.graphics.Color

enum class LineType {
    PROMPT,
    OUTPUT,
    ERROR,
    SUCCESS,
    INFO,
    SYSTEM,
    AI_QUERY,
    AI_RESPONSE,
    DIRECTORY,
    COMMAND
}

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT,
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class TerminalMode {
    SHELL,
    FLINT_AI,
    PYTHON_REPL,
    NODE_REPL,
    NANO_EDITOR
}

data class TerminalTheme(
    val name: String,
    val background: Color,
    val foreground: Color,
    val promptUser: Color,
    val promptPath: Color,
    val accent: Color,
    val error: Color,
    val success: Color,
    val aiAccent: Color,
    val toolbarBg: Color
)

object TerminalThemes {
    val ZenDark = TerminalTheme(
        name = "Zen Dark",
        background = Color(0xFF0D1117),
        foreground = Color(0xFFE6EDF3),
        promptUser = Color(0xFF58A6FF),
        promptPath = Color(0xFF7EE787),
        accent = Color(0xFF58A6FF),
        error = Color(0xFFFF7B72),
        success = Color(0xFF7EE787),
        aiAccent = Color(0xFFD2A8FF),
        toolbarBg = Color(0xFF161B22)
    )

    val MatrixGreen = TerminalTheme(
        name = "Matrix",
        background = Color(0xFF030A04),
        foreground = Color(0xFF00FF66),
        promptUser = Color(0xFF33FF88),
        promptPath = Color(0xFF00DD44),
        accent = Color(0xFF00FF66),
        error = Color(0xFFFF3333),
        success = Color(0xFF00FF66),
        aiAccent = Color(0xFF66FFB2),
        toolbarBg = Color(0xFF061408)
    )

    val Cyberpunk = TerminalTheme(
        name = "Cyberpunk",
        background = Color(0xFF0F051D),
        foreground = Color(0xFF00F0FF),
        promptUser = Color(0xFFFF007F),
        promptPath = Color(0xFFFFE600),
        accent = Color(0xFFFF007F),
        error = Color(0xFFFF3366),
        success = Color(0xFF00FF9F),
        aiAccent = Color(0xFFD000FF),
        toolbarBg = Color(0xFF1B0B33)
    )

    val Monokai = TerminalTheme(
        name = "Monokai",
        background = Color(0xFF272822),
        foreground = Color(0xFFF8F8F2),
        promptUser = Color(0xFFA6E22E),
        promptPath = Color(0xFF66D9EF),
        accent = Color(0xFFFD971F),
        error = Color(0xFFF92672),
        success = Color(0xFFA6E22E),
        aiAccent = Color(0xFFAE81FF),
        toolbarBg = Color(0xFF1E1F1C)
    )

    val AmberHacker = TerminalTheme(
        name = "Hacker Amber",
        background = Color(0xFF100C08),
        foreground = Color(0xFFFFB000),
        promptUser = Color(0xFFFFCC00),
        promptPath = Color(0xFFFF8800),
        accent = Color(0xFFFFB000),
        error = Color(0xFFFF4444),
        success = Color(0xFFFFCC00),
        aiAccent = Color(0xFFFFE066),
        toolbarBg = Color(0xFF1F1810)
    )

    val allThemes = listOf(ZenDark, MatrixGreen, Cyberpunk, Monokai, AmberHacker)
}
