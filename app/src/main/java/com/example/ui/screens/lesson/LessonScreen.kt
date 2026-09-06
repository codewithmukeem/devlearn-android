package com.example.ui.screens.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.local.entity.UserProgressEntity
import com.example.data.model.Course
import com.example.data.model.Lesson
import com.example.ui.components.BadgePill
import com.example.ui.components.InteractiveCodeEditor
import com.example.ui.components.OutputConsole
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CodeEditorBackground
import com.example.ui.theme.CodeText
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.WarningColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    course: Course,
    lesson: Lesson,
    progress: UserProgressEntity?,
    onBack: () -> Unit,
    onCompleteLesson: () -> Unit,
    onOpenLabWithCode: (String, String) -> Unit,
    onAskAI: (String, String, String) -> Unit,
    onOpenPdf: (String) -> Unit,
    onRecordQuizResult: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val executor = remember { SafeSandboxedExecutionEngine() }

    // Segment tab: 0 = Read & Code, 1 = Practice Lab, 2 = Quiz
    var selectedTab by remember { mutableIntStateOf(0) }

    // Example code runner state
    var exampleCode by remember(lesson) { mutableStateOf(lesson.code) }
    var exampleResult by remember(lesson) { mutableStateOf<ExecutionResult?>(null) }
    var isRunningExample by remember { mutableStateOf(false) }

    // Practice problem state
    var practiceCode by remember(lesson) { mutableStateOf(lesson.practice.starterCode) }
    var practiceResult by remember(lesson) { mutableStateOf<ExecutionResult?>(null) }
    var isRunningPractice by remember { mutableStateOf(false) }
    var isPracticePassed by remember(progress) { mutableStateOf(progress?.practiceCompleted == true) }
    var showHint by remember { mutableStateOf(false) }

    // Quiz state
    val selectedAnswers = remember(lesson) { mutableStateMapOf<Int, Int>() }
    var quizSubmitted by remember(progress) { mutableStateOf(progress?.quizTotal ?: 0 > 0) }
    var quizScore by remember(progress) { mutableIntStateOf(progress?.quizScore ?: 0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${course.title} • ${lesson.readTimeMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onOpenPdf(course.id) },
                        modifier = Modifier.testTag("lesson_open_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Open Handbook PDF",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            val activeSnippet = if (selectedTab == 1) practiceCode else exampleCode
                            onAskAI(course.title, lesson.title, activeSnippet)
                        },
                        modifier = Modifier.testTag("lesson_ask_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask AI Tutor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Three-segment learning tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("1. Concept", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("2. Practice", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("3. Quiz", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Concept & Code Example
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = lesson.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Structured Content
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Lesson Guide",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = lesson.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }

                        // Code Example with Live Execution
                        item {
                            Text(
                                text = "Code Example",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            InteractiveCodeEditor(
                                code = exampleCode,
                                onCodeChange = { exampleCode = it },
                                language = course.id,
                                onRunCode = {
                                    isRunningExample = true
                                    coroutineScope.launch {
                                        exampleResult = executor.execute(exampleCode, course.id)
                                        isRunningExample = false
                                    }
                                },
                                onReset = {
                                    exampleCode = lesson.code
                                    exampleResult = null
                                },
                                isRunning = isRunningExample,
                                executionResult = exampleResult,
                                readOnly = false
                            )
                        }

                        // Action: Proceed to Practice
                        item {
                            PrimaryButton(
                                text = "Continue to Practice Problem",
                                onClick = { selectedTab = 1 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                1 -> {
                    // Practice Lab Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BadgePill(
                                            text = "HANDS-ON EXERCISE",
                                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )

                                        if (isPracticePassed) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SuccessColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Solved",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SuccessColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = lesson.practice.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = lesson.practice.instructions,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Code Editor for Practice
                        item {
                            InteractiveCodeEditor(
                                code = practiceCode,
                                onCodeChange = { practiceCode = it },
                                language = course.id,
                                onRunCode = {
                                    isRunningPractice = true
                                    coroutineScope.launch {
                                        val res = executor.execute(practiceCode, course.id)
                                        practiceResult = res
                                        isRunningPractice = false

                                        // Check if output satisfies exercise
                                        val expected = lesson.practice.expectedOutput.trim()
                                        if (res.stdout.trim() == expected) {
                                            isPracticePassed = true
                                        }
                                    }
                                },
                                onReset = {
                                    practiceCode = lesson.practice.starterCode
                                    practiceResult = null
                                },
                                isRunning = isRunningPractice,
                                executionResult = practiceResult
                            )
                        }

                        // Test case result badge
                        if (practiceResult != null) {
                            item {
                                val expected = lesson.practice.expectedOutput.trim()
                                val passed = practiceResult?.stdout?.trim() == expected

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (passed) SuccessColor.copy(alpha = 0.1f) else WarningColor.copy(alpha = 0.1f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (passed) SuccessColor.copy(alpha = 0.4f) else WarningColor.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = if (passed) "Test Passed! Output matches expected result." else "Output Mismatch",
                                            fontWeight = FontWeight.Bold,
                                            color = if (passed) SuccessColor else WarningColor,
                                            fontSize = 13.sp
                                        )
                                        if (!passed) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Expected:\n$expected",
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hint Expander
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SecondaryButton(
                                    text = if (showHint) "Hide Hint" else "Need a Hint?",
                                    onClick = { showHint = !showHint },
                                    icon = Icons.Default.Lightbulb
                                )

                                SecondaryButton(
                                    text = "Ask AI Tutor",
                                    onClick = { onAskAI(course.title, lesson.title, practiceCode) },
                                    icon = Icons.Default.AutoAwesome
                                )
                            }
                        }

                        if (showHint) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💡 Hint: ${lesson.practice.hint}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        // Action: Next to Quiz
                        item {
                            PrimaryButton(
                                text = "Take Knowledge Check (Quiz)",
                                onClick = { selectedTab = 2 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                2 -> {
                    // Knowledge Check (Quiz)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(lesson.quizzes.size) { qIdx ->
                            val quiz = lesson.quizzes[qIdx]
                            val selectedOpt = selectedAnswers[qIdx]

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Question ${qIdx + 1} of ${lesson.quizzes.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = quiz.question,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Options
                                    quiz.options.forEachIndexed { optIdx, optionText ->
                                        val isChosen = selectedOpt == optIdx
                                        val isCorrectAnswer = optIdx == quiz.correctIndex

                                        val (cardColor, borderColor, textColor) = when {
                                            quizSubmitted && isCorrectAnswer -> Triple(
                                                SuccessColor.copy(alpha = 0.12f),
                                                SuccessColor,
                                                SuccessColor
                                            )
                                            quizSubmitted && isChosen && !isCorrectAnswer -> Triple(
                                                ErrorColor.copy(alpha = 0.12f),
                                                ErrorColor,
                                                ErrorColor
                                            )
                                            isChosen -> Triple(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary
                                            )
                                            else -> Triple(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.outline,
                                                MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable(enabled = !quizSubmitted) {
                                                    selectedAnswers[qIdx] = optIdx
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = cardColor,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isChosen) borderColor else Color.Transparent)
                                                        .border(1.dp, borderColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ('A' + optIdx).toString(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isChosen) Color.White else textColor
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Text(
                                                    text = optionText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Rationale after submit
                                    if (quizSubmitted) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Explanation: ${quiz.explanation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Submit Quiz & Complete Lesson Buttons
                        item {
                            if (!quizSubmitted) {
                                PrimaryButton(
                                    text = "Submit Quiz Answers",
                                    enabled = selectedAnswers.size == lesson.quizzes.size,
                                    onClick = {
                                        var correctCount = 0
                                        lesson.quizzes.forEachIndexed { idx, q ->
                                            if (selectedAnswers[idx] == q.correctIndex) {
                                                correctCount++
                                            }
                                        }
                                        quizScore = correctCount
                                        quizSubmitted = true
                                        onRecordQuizResult(correctCount, lesson.quizzes.size)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SuccessColor.copy(alpha = 0.1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessColor.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Quiz Result: $quizScore / ${lesson.quizzes.size} correct!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessColor,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }

                                    PrimaryButton(
                                        text = "Complete Lesson & Return to Syllabus",
                                        onClick = onCompleteLesson,
                                        icon = Icons.Default.CheckCircle,
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "finish_lesson_button"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
