package com.example.ui.screens.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lab.ExecutionResult
import com.example.data.lab.SafeSandboxedExecutionEngine
import com.example.ui.components.BadgePill
import com.example.ui.components.InteractiveCodeEditor
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.WarningColor
import kotlinx.coroutines.launch

@Composable
fun VirtualLabScreen(
    initialLanguage: String = "c",
    initialCode: String = "",
    onAskAI: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val executor = remember { SafeSandboxedExecutionEngine() }

    val languages = listOf("c", "cpp", "java", "android")
    var selectedLanguage by remember { mutableStateOf(initialLanguage.ifBlank { "c" }) }

    var code by remember(selectedLanguage) {
        mutableStateOf(
            if (initialCode.isNotBlank() && selectedLanguage == initialLanguage) initialCode
            else SafeSandboxedExecutionEngine.getTemplateForLanguage(selectedLanguage)
        )
    }

    var executionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Virtual Coding Lab",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Experiment, test algorithms, and observe compiler output",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "SANDBOX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Language Selector Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(languages) { lang ->
                    val displayName = when (lang) {
                        "c" -> "C"
                        "cpp" -> "C++"
                        "java" -> "Java"
                        else -> "Android / Kotlin"
                    }
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = {
                            selectedLanguage = lang
                            code = SafeSandboxedExecutionEngine.getTemplateForLanguage(lang)
                            executionResult = null
                        },
                        label = { Text(displayName, fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Action Toolbar: Template presets, Clear, Ask AI
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reset to template
                    OutlinedButton(
                        onClick = {
                            code = SafeSandboxedExecutionEngine.getTemplateForLanguage(selectedLanguage)
                            executionResult = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }

                    // Clear editor
                    OutlinedButton(
                        onClick = {
                            code = ""
                            executionResult = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }

                // Ask AI about this code
                Button(
                    onClick = { onAskAI(selectedLanguage, code) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ask AI Tutor", fontSize = 12.sp)
                }
            }
        }

        // Interactive Code Editor
        item {
            InteractiveCodeEditor(
                code = code,
                onCodeChange = { code = it },
                language = selectedLanguage,
                onRunCode = {
                    isRunning = true
                    coroutineScope.launch {
                        executionResult = executor.execute(code, selectedLanguage)
                        isRunning = false
                    }
                },
                onReset = {
                    code = SafeSandboxedExecutionEngine.getTemplateForLanguage(selectedLanguage)
                    executionResult = null
                },
                isRunning = isRunning,
                executionResult = executionResult,
                modifier = Modifier.testTag("virtual_lab_editor")
            )
        }

        // Learning Tips & Example Snippets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Sandbox Capabilities",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Fully sandboxed deterministic environment running locally on device.\n• Supports syntax diagnostics, loops, variable assignment, standard streams, and arithmetic evaluation.\n• Safe from dangerous operations and infinite freezes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
