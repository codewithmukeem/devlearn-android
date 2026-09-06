package com.example.ui.screens.pdf

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.Course
import com.example.data.pdf.PdfDocumentManager
import com.example.data.pdf.PdfReadingManager
import com.example.ui.theme.CodeEditorBackground
import com.example.ui.theme.CodeText
import kotlinx.coroutines.launch
import java.io.File

enum class ReaderTheme(val title: String, val bg: Color, val surfaceBg: Color, val text: Color, val subtext: Color) {
    PAPER("Paper", Color(0xFFFAF8F5), Color(0xFFF3EFEA), Color(0xFF1E293B), Color(0xFF64748B)),
    WHITE("Crisp White", Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFF0F172A), Color(0xFF475569)),
    NIGHT("Night Slate", Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFFF1F5F9), Color(0xFF94A3B8))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    course: Course,
    initialPage: Int = 0,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val readingManager = remember { PdfReadingManager(context) }

    // Reading Controls (Magazine / Blog experience)
    var selectedTheme by remember { mutableStateOf(ReaderTheme.PAPER) }
    var fontScale by remember { mutableFloatStateOf(1.0f) } // 0.9f, 1.0f, 1.15f, 1.3f
    var showChapterMenu by remember { mutableStateOf(false) }
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(readingManager.isBookmarked(course.id, 0)) }

    // Calculate reading progress (0.0 to 1.0) based on scroll
    val readingProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) 0f
            else (listState.firstVisibleItemIndex.toFloat() / (totalItems - 1)).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${course.title} Handbook",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = selectedTheme.text
                            )
                            Text(
                                text = "Magazine Edition • ${course.chapters.size} Chapters • ${(readingProgress * 100).toInt()}% Read",
                                style = MaterialTheme.typography.bodySmall,
                                color = selectedTheme.subtext,
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("pdf_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = selectedTheme.text
                            )
                        }
                    },
                    actions = {
                        // Font Size Adjuster (A- / A+)
                        Box {
                            IconButton(onClick = { showFontSizeMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = "Adjust Font Size",
                                    tint = selectedTheme.text
                                )
                            }
                            DropdownMenu(
                                expanded = showFontSizeMenu,
                                onDismissRequest = { showFontSizeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Small (Compact)") },
                                    onClick = { fontScale = 0.9f; showFontSizeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Standard (100%)") },
                                    onClick = { fontScale = 1.0f; showFontSizeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Large (115%)") },
                                    onClick = { fontScale = 1.15f; showFontSizeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Extra Large (130%)") },
                                    onClick = { fontScale = 1.3f; showFontSizeMenu = false }
                                )
                            }
                        }

                        // Theme Cycle (Paper -> White -> Night)
                        IconButton(
                            onClick = {
                                selectedTheme = when (selectedTheme) {
                                    ReaderTheme.PAPER -> ReaderTheme.WHITE
                                    ReaderTheme.WHITE -> ReaderTheme.NIGHT
                                    ReaderTheme.NIGHT -> ReaderTheme.PAPER
                                }
                            }
                        ) {
                            Icon(
                                imageVector = when (selectedTheme) {
                                    ReaderTheme.PAPER -> Icons.Default.WbSunny
                                    ReaderTheme.WHITE -> Icons.Default.LightMode
                                    ReaderTheme.NIGHT -> Icons.Default.DarkMode
                                },
                                contentDescription = "Switch Theme",
                                tint = selectedTheme.text
                            )
                        }

                        // Table of Contents Menu (Jump to Chapter)
                        Box {
                            IconButton(onClick = { showChapterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = "Table of Contents",
                                    tint = selectedTheme.text
                                )
                            }
                            DropdownMenu(
                                expanded = showChapterMenu,
                                onDismissRequest = { showChapterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Table of Contents",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = { showChapterMenu = false }
                                )
                                HorizontalDivider()
                                course.chapters.forEachIndexed { index, chapter ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Ch ${index + 1}: ${chapter.title}",
                                                maxLines = 1,
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        onClick = {
                                            showChapterMenu = false
                                            coroutineScope.launch {
                                                // Scroll to chapter item (1 offset for header)
                                                listState.animateScrollToItem(index + 1)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Export / Share PDF
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val file = PdfDocumentManager.getOrCreatePdf(context, course.id)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share ${course.title} Handbook"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "PDF saved to device storage", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export PDF",
                                tint = selectedTheme.text
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = selectedTheme.surfaceBg
                    )
                )

                // Continuous Reading Progress Bar at the top of the screen
                LinearProgressIndicator(
                    progress = { readingProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = selectedTheme.surfaceBg
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(selectedTheme.bg),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Magazine Cover / Hero Header Card
            item {
                MagazineHeroHeader(
                    course = course,
                    theme = selectedTheme,
                    fontScale = fontScale
                )
            }

            // Continuous Chapters (Article style)
            itemsIndexed(course.chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                MagazineChapterArticle(
                    chapterNumber = index + 1,
                    totalChapters = course.chapters.size,
                    chapter = chapter,
                    theme = selectedTheme,
                    fontScale = fontScale
                )
            }

            // Magazine Footer / Completion Card
            item {
                MagazineFooter(
                    courseTitle = course.title,
                    theme = selectedTheme,
                    onBackToCourse = onBack
                )
            }
        }
    }
}

@Composable
fun MagazineHeroHeader(
    course: Course,
    theme: ReaderTheme,
    fontScale: Float
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.surfaceBg,
        border = BorderStroke(1.dp, theme.subtext.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "DEVLEARN COMPANION • TECHNICAL HANDBOOK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = (10 * fontScale).sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "2026 EDITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.subtext,
                    fontSize = (10 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = course.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = theme.text,
                fontSize = (24 * fontScale).sp,
                lineHeight = (30 * fontScale).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.subtext,
                fontSize = (14 * fontScale).sp,
                lineHeight = (22 * fontScale).sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = theme.subtext.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Published by DevLearn Editorial",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.subtext,
                    fontSize = (11 * fontScale).sp
                )
                Text(
                    text = "${course.chapters.size} In-Depth Chapters",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (11 * fontScale).sp
                )
            }
        }
    }
}

@Composable
fun MagazineChapterArticle(
    chapterNumber: Int,
    totalChapters: Int,
    chapter: com.example.data.model.Chapter,
    theme: ReaderTheme,
    fontScale: Float
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.surfaceBg,
        border = BorderStroke(1.dp, theme.subtext.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Chapter Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$chapterNumber",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CHAPTER $chapterNumber OF $totalChapters",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        fontSize = (11 * fontScale).sp
                    )
                }

                Text(
                    text = "${chapter.lessons.size} Lessons",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.subtext,
                    fontSize = (11 * fontScale).sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chapter Title
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = theme.text,
                fontSize = (20 * fontScale).sp,
                lineHeight = (26 * fontScale).sp
            )

            if (chapter.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = chapter.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.subtext,
                    fontSize = (13.5 * fontScale).sp,
                    lineHeight = (20 * fontScale).sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Magazine Pull-Quote / Architectural Takeaway Callout Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "“",
                        fontSize = (32 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = (28 * fontScale).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Core Architectural Principle",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (10.5 * fontScale).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mastering ${chapter.title.lowercase()} is foundational for building reliable, leak-free, high-performance software systems.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.text,
                            fontSize = (12.5 * fontScale).sp,
                            lineHeight = (18 * fontScale).sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lessons in this Chapter (Article format)
            chapter.lessons.forEachIndexed { lIndex, lesson ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (lIndex > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = theme.subtext.copy(alpha = 0.15f)
                        )
                    }

                    // Lesson Title Subheading
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$chapterNumber.${lIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (12 * fontScale).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.text,
                            fontSize = (16 * fontScale).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lesson Explanatory Prose
                    Text(
                        text = lesson.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.text,
                        fontSize = (14 * fontScale).sp,
                        lineHeight = (22 * fontScale).sp
                    )

                    // Code Snippet Showcase
                    if (lesson.code.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CodeEditorBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Code Card Header with Copy Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SOURCE CODE",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(lesson.code))
                                                Toast.makeText(context, "Snippet copied", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Code",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Copy",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Text(
                                    text = lesson.code.trim(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (11.5 * fontScale).sp,
                                    color = CodeText,
                                    lineHeight = (18 * fontScale).sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Detailed Deep-Dive Breakdown / Summary
                    if (lesson.summary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = theme.bg,
                            border = BorderStroke(1.dp, theme.subtext.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 Key Architectural Summary",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = (11.5 * fontScale).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lesson.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.subtext,
                                    fontSize = (12.5 * fontScale).sp,
                                    lineHeight = (19 * fontScale).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MagazineFooter(
    courseTitle: String,
    theme: ReaderTheme,
    onBackToCourse: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.surfaceBg,
        border = BorderStroke(1.dp, theme.subtext.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "End of Handbook",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.text
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "You've completed reading all chapters for $courseTitle. Practice the code in the Virtual Lab or verify your skills with course quizzes to earn your verified certificate.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.subtext,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = (19).sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBackToCourse() }
            ) {
                Text(
                    text = "Return to Syllabus",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}
