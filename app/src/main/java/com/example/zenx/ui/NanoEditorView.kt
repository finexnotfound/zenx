package com.example.zenx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zenx.model.TerminalTheme
import java.io.File

@Composable
fun NanoEditorView(
    file: File,
    initialContent: String,
    theme: TerminalTheme,
    onSaveAndExit: (String) -> Unit,
    onExitWithoutSaving: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var isModified by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
            .testTag("nano_editor_view")
    ) {
        // Nano Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2638))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GNU nano 7.2",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "File: ${file.name} ${if (isModified) "[Modified]" else ""}",
                color = if (isModified) Color(0xFFFFB000) else Color.Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "Lines: ${content.lines().size}",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        // Status banner if any
        if (statusMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E3A52))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusMessage,
                    color = Color(0xFF7EE787),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        // Text editing area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            BasicTextField(
                value = content,
                onValueChange = {
                    content = it
                    isModified = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("nano_text_input"),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = theme.foreground,
                    lineHeight = 18.sp
                ),
                cursorBrush = SolidColor(theme.accent)
            )
        }

        // Bottom Nano Shortcut Toolbar
        Surface(
            color = Color(0xFF161B26),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            onSaveAndExit(content)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .testTag("nano_save_button")
                    ) {
                        Text(
                            text = "^O Save & Exit",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            statusMessage = "[ Wrote ${content.length} bytes to ${file.name} ]"
                            file.writeText(content)
                            isModified = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("nano_writeout_button")
                    ) {
                        Text(
                            text = "^W WriteOut",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            onExitWithoutSaving()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDA3633)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .testTag("nano_exit_button")
                    ) {
                        Text(
                            text = "^X Discard",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
