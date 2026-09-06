package com.example.ui.screens.pdf

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.data.pdf.PdfDocumentManager
import com.example.data.pdf.PdfReadingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val readingManager = remember { PdfReadingManager(context) }

    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    var totalPages by remember { mutableIntStateOf(1) }
    var currentPageIndex by remember {
        mutableIntStateOf(
            if (initialPage >= 0) initialPage
            else readingManager.getLastReadPage(course.id)
        )
    }

    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }

    // Reading Controls
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isNightMode by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    var showBookmarksMenu by remember { mutableStateOf(false) }
    var bookmarksList by remember { mutableStateOf(emptyList<Int>()) }

    // Initialize PDF file & renderer
    LaunchedEffect(course.id) {
        isRendering = true
        val file = PdfDocumentManager.getOrCreatePdf(context, course.id)
        pdfFile = file
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor = pfd
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            totalPages = renderer.pageCount.coerceAtLeast(1)

            val savedPage = if (initialPage > 0) initialPage else readingManager.getLastReadPage(course.id)
            currentPageIndex = savedPage.coerceIn(0, totalPages - 1)
            bookmarksList = readingManager.getBookmarks(course.id)
            isBookmarked = readingManager.isBookmarked(course.id, currentPageIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Render current page to high-res Bitmap
    LaunchedEffect(currentPageIndex, pdfRenderer, isNightMode) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        if (currentPageIndex !in 0 until renderer.pageCount) return@LaunchedEffect

        isRendering = true
        withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(currentPageIndex)
                // Render at 2x density for crisp text readability
                val width = page.width * 2
                val height = page.height * 2
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val finalBmp = if (isNightMode) {
                    val invertedBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(invertedBmp)
                    val paint = Paint().apply {
                        val colorMatrix = ColorMatrix(
                            floatArrayOf(
                                -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                                 0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                                 0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                                 0.0f,  0.0f,  0.0f, 1.0f,   0.0f
                            )
                        )
                        colorFilter = ColorMatrixColorFilter(colorMatrix)
                    }
                    canvas.drawBitmap(bmp, 0f, 0f, paint)
                    bmp.recycle()
                    invertedBmp
                } else {
                    bmp
                }

                withContext(Dispatchers.Main) {
                    currentBitmap = finalBmp
                    isRendering = false
                    readingManager.saveLastReadPage(course.id, currentPageIndex)
                    isBookmarked = readingManager.isBookmarked(course.id, currentPageIndex)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isRendering = false
                }
            }
        }
    }

    // Clean up native renderer resources on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                // Closed safely
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${course.title} Handbook",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "Page ${currentPageIndex + 1} of $totalPages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pdf_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Course"
                        )
                    }
                },
                actions = {
                    // Night mode toggle
                    IconButton(onClick = { isNightMode = !isNightMode }, modifier = Modifier.testTag("pdf_theme_toggle")) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Reader Theme"
                        )
                    }

                    // Bookmark toggle
                    IconButton(
                        onClick = {
                            val bookmarked = readingManager.toggleBookmark(course.id, currentPageIndex)
                            isBookmarked = bookmarked
                            bookmarksList = readingManager.getBookmarks(course.id)
                        },
                        modifier = Modifier.testTag("pdf_bookmark_toggle")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark this page",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bookmarks list dropdown
                    Box {
                        IconButton(onClick = { showBookmarksMenu = true }) {
                            Text(
                                text = "${bookmarksList.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showBookmarksMenu,
                            onDismissRequest = { showBookmarksMenu = false }
                        ) {
                            if (bookmarksList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No bookmarks yet") },
                                    onClick = { showBookmarksMenu = false }
                                )
                            } else {
                                bookmarksList.forEach { page ->
                                    DropdownMenuItem(
                                        text = { Text("Jump to Page ${page + 1}") },
                                        onClick = {
                                            currentPageIndex = page
                                            showBookmarksMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Zoom toolbar and page buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Page button
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex--
                                    zoomScale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.testTag("pdf_prev_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                        }

                        // Zoom Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                            }

                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                            }

                            if (zoomScale != 1f) {
                                IconButton(
                                    onClick = {
                                        zoomScale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Zoom", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Next Page button
                        IconButton(
                            onClick = {
                                if (currentPageIndex < totalPages - 1) {
                                    currentPageIndex++
                                    zoomScale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.testTag("pdf_next_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                        }
                    }

                    // Scrubber Slider
                    if (totalPages > 1) {
                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = { target ->
                                currentPageIndex = target.toInt().coerceIn(0, totalPages - 1)
                                zoomScale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            valueRange = 0f..(totalPages - 1).toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth().testTag("pdf_page_slider")
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isNightMode) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.75f, 3.0f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isRendering && currentBitmap == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "Page ${currentPageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            if (isRendering && currentBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rendering page...", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
