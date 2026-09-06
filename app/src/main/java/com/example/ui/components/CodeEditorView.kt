package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lab.ExecutionResult
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeEditorBackground
import com.example.ui.theme.CodeEditorGutter
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeLineNumber
import com.example.ui.theme.CodeNumber
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeText
import com.example.ui.theme.CodeType
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.SuccessColor

@Composable
fun InteractiveCodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    language: String,
    onRunCode: () -> Unit,
    onReset: () -> Unit,
    isRunning: Boolean = false,
    executionResult: ExecutionResult? = null,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val context = LocalContext.current
    val lines = code.lines()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .background(CodeEditorBackground)
    ) {
        // Editor Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodeEditorGutter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = language.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${lines.size} lines",
                    color = CodeLineNumber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Copy button
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy code",
                        tint = CodeLineNumber,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Reset button
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset code",
                        tint = CodeLineNumber,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Run Action Button
                Button(
                    onClick = onRunCode,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("run_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) "Running..." else "Run",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Code Editor Body with Line Numbers Gutter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .width(38.dp)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lines.size) {
                    Text(
                        text = "$i",
                        color = CodeLineNumber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                }
            }

            // Code input or view
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(end = 12.dp)
            ) {
                if (readOnly) {
                    val annotated = highlightSyntax(code)
                    Text(
                        text = annotated,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp,
                        color = CodeText
                    )
                } else {
                    BasicTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = CodeText,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_text_field")
                    )
                }
            }
        }

        // Output Console Section (if execution result exists)
        if (executionResult != null) {
            OutputConsole(result = executionResult)
        }
    }
}

@Composable
fun OutputConsole(
    result: ExecutionResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF030712))
            .border(
                1.dp,
                if (result.success) Color(0xFF1E293B) else Color(0xFF7F1D1D),
                RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(12.dp)
    ) {
        // Output Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CONSOLE OUTPUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (result.success) SuccessColor.copy(alpha = 0.2f) else ErrorColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (result.success) "Exit 0 (${result.executionTimeMs}ms)" else "Error (Exit ${result.exitCode})",
                        color = if (result.success) SuccessColor else ErrorColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "SANDBOX",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stdout or Stderr
        if (result.stdout.isNotBlank()) {
            Text(
                text = result.stdout,
                color = Color(0xFFF1F5F9),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
        }

        if (result.stderr.isNotBlank()) {
            if (result.stdout.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = result.stderr,
                color = Color(0xFFFCA5A5),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Safe Sandbox Transparency Note
        Text(
            text = "• Execution safely verified via local deterministic client sandbox",
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            fontFamily = FontFamily.SansSerif
        )
    }
}

// Basic syntax colorizer for keywords, numbers, and strings
fun highlightSyntax(code: String) = buildAnnotatedString {
    val keywords = setOf(
        "include", "int", "float", "double", "char", "void", "return", "if", "else", "for",
        "while", "do", "class", "public", "private", "protected", "virtual", "override",
        "new", "delete", "import", "package", "val", "var", "fun", "null", "true", "false",
        "static", "auto", "String", "boolean", "const"
    )

    val tokens = code.split(Regex("(?<=[^a-zA-Z0-9_])|(?=[^a-zA-Z0-9_])"))
    var inString = false
    var inComment = false

    for (token in tokens) {
        when {
            token == "//" || inComment -> {
                inComment = !token.contains("\n")
                pushStyle(SpanStyle(color = CodeComment))
                append(token)
                pop()
            }
            token == "\"" -> {
                inString = !inString
                pushStyle(SpanStyle(color = CodeString))
                append(token)
                pop()
            }
            inString -> {
                pushStyle(SpanStyle(color = CodeString))
                append(token)
                pop()
            }
            token in keywords -> {
                pushStyle(SpanStyle(color = CodeKeyword, fontWeight = FontWeight.SemiBold))
                append(token)
                pop()
            }
            token.toIntOrNull() != null -> {
                pushStyle(SpanStyle(color = CodeNumber))
                append(token)
                pop()
            }
            else -> {
                pushStyle(SpanStyle(color = CodeText))
                append(token)
                pop()
            }
        }
    }
}
