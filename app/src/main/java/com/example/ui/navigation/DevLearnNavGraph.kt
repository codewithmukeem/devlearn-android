package com.example.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CertificateEntity
import com.example.data.model.Course
import com.example.data.repository.CourseRepository
import com.example.data.repository.ProgressRepository
import com.example.ui.screens.aitutor.AITutorScreen
import com.example.ui.screens.courses.CourseDetailScreen
import com.example.ui.screens.courses.CoursesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.lab.VirtualLabScreen
import com.example.ui.screens.lesson.LessonScreen
import com.example.ui.screens.pdf.PdfReaderScreen
import com.example.ui.screens.progress.ProgressAndProfileScreen
import com.example.ui.theme.WarningColor
import kotlinx.coroutines.launch

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun DevLearnApp() {
    DevLearnNavGraph()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevLearnNavGraph(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    // Repositories
    val database = remember { AppDatabase.getDatabase(context) }
    val courseRepo = remember { CourseRepository(context) }
    val progressRepo = remember {
        ProgressRepository(
            database.progressDao(),
            database.profileDao(),
            database.certificateDao()
        )
    }

    // State flows
    val progressList by progressRepo.allProgress.collectAsState(initial = emptyList())
    val userProfile by progressRepo.userProfile.collectAsState(initial = null)
    val certificates by progressRepo.allCertificates.collectAsState(initial = emptyList())

    // Cached course curriculum
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var activeCertificateDialog by remember { mutableStateOf<CertificateEntity?>(null) }

    LaunchedEffect(Unit) {
        courses = courseRepo.getAllCourses()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Default.Home, "nav_home"),
        BottomNavItem("Courses", Screen.Courses.route, Icons.AutoMirrored.Filled.MenuBook, "nav_courses"),
        BottomNavItem("Lab", Screen.VirtualLab.route, Icons.Default.Code, "nav_lab"),
        BottomNavItem("AI Tutor", Screen.AITutor.route, Icons.Default.AutoAwesome, "nav_ai_tutor"),
        BottomNavItem("Progress", Screen.Progress.route, Icons.Default.WorkspacePremium, "nav_progress")
    )

    // Base route extracted cleanly (ignores query params like ?initialLang=c)
    val currentBaseRoute = currentDestination?.route?.substringBefore("?")?.substringBefore("/")
    val isImeOpen = WindowInsets.isImeVisible

    // Hide bottom bar on reading/lesson/pdf view or when keyboard is open
    val shouldShowBottomBar = !isImeOpen && currentBaseRoute in listOf(
        Screen.Home.route,
        Screen.Courses.route,
        Screen.VirtualLab.route,
        Screen.AITutor.route,
        Screen.Progress.route
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentBaseRoute == item.route

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(text = item.title, fontSize = 11.sp) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Home Dashboard
            composable(Screen.Home.route) {
                HomeScreen(
                    courses = courses,
                    progressList = progressList,
                    profile = userProfile,
                    certificates = certificates,
                    onCourseClick = { courseId ->
                        navController.navigate(Screen.CourseDetail.createRoute(courseId))
                    },
                    onResumeLesson = { courseId, lessonId ->
                        navController.navigate(Screen.Lesson.createRoute(courseId, lessonId))
                    },
                    onOpenLab = {
                        navController.navigate(Screen.VirtualLab.route)
                    },
                    onOpenAITutor = {
                        navController.navigate(Screen.AITutor.route)
                    }
                )
            }

            // Courses Catalog
            composable(Screen.Courses.route) {
                CoursesScreen(
                    courses = courses,
                    progressList = progressList,
                    onCourseClick = { courseId ->
                        navController.navigate(Screen.CourseDetail.createRoute(courseId))
                    }
                )
            }

            // Course Detail / Syllabus
            composable(
                route = Screen.CourseDetail.route,
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                val course = courses.find { it.id == courseId }
                val courseCertificate = certificates.find { it.courseId == courseId }

                if (course != null) {
                    CourseDetailScreen(
                        course = course,
                        progressList = progressList.filter { it.courseId == courseId },
                        certificate = courseCertificate,
                        onBack = { navController.popBackStack() },
                        onLessonClick = { lessonId ->
                            navController.navigate(Screen.Lesson.createRoute(course.id, lessonId))
                        },
                        onOpenPdf = { cId ->
                            navController.navigate(Screen.PdfReader.createRoute(cId))
                        },
                        onClaimCertificate = {
                            coroutineScope.launch {
                                val cert = progressRepo.claimCertificate(
                                    courseId = course.id,
                                    courseTitle = course.title,
                                    learnerName = userProfile?.name ?: "Aspiring Developer"
                                )
                                activeCertificateDialog = cert
                            }
                        },
                        onViewCertificate = { cert ->
                            activeCertificateDialog = cert
                        }
                    )
                }
            }

            // Lesson Interactive Reader
            composable(
                route = Screen.Lesson.route,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("lessonId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
                val course = courses.find { it.id == courseId }
                val lesson = course?.chapters?.flatMap { it.lessons }?.find { it.id == lessonId }
                val lessonProgress = progressList.find { it.courseId == courseId && it.lessonId == lessonId }

                LaunchedEffect(courseId, lessonId) {
                    progressRepo.recordLessonOpened(courseId, lessonId)
                }

                if (course != null && lesson != null) {
                    LessonScreen(
                        course = course,
                        lesson = lesson,
                        progress = lessonProgress,
                        onBack = { navController.popBackStack() },
                        onCompleteLesson = {
                            coroutineScope.launch {
                                progressRepo.markLessonComplete(courseId, lessonId)
                                navController.popBackStack()
                            }
                        },
                        onOpenLabWithCode = { lang, codeSnippet ->
                            navController.navigate(Screen.VirtualLab.createRoute(lang, codeSnippet))
                        },
                        onAskAI = { cTitle, lTitle, codeSnippet ->
                            navController.navigate(Screen.AITutor.createRoute(cTitle, lTitle, codeSnippet))
                        },
                        onOpenPdf = { cId ->
                            navController.navigate(Screen.PdfReader.createRoute(cId))
                        },
                        onRecordQuizResult = { score, total ->
                            coroutineScope.launch {
                                progressRepo.recordQuizScore(courseId, lessonId, score, total)
                            }
                        }
                    )
                }
            }

            // In-App PDF Reader
            composable(
                route = Screen.PdfReader.routeDefinition,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("page") { type = NavType.IntType; defaultValue = 0 }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                val page = backStackEntry.arguments?.getInt("page") ?: 0
                val course = courses.find { it.id == courseId }
                if (course != null) {
                    PdfReaderScreen(
                        course = course,
                        initialPage = page,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Virtual Coding Lab
            composable(
                route = Screen.VirtualLab.routeDefinition,
                arguments = listOf(
                    navArgument("initialLang") {
                        type = NavType.StringType
                        defaultValue = "c"
                    },
                    navArgument("initialCode") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initialLang = backStackEntry.arguments?.getString("initialLang") ?: "c"
                val rawCode = backStackEntry.arguments?.getString("initialCode") ?: ""
                val initialCode = try { java.net.URLDecoder.decode(rawCode, "UTF-8") } catch (e: Exception) { rawCode }

                VirtualLabScreen(
                    initialLanguage = initialLang,
                    initialCode = initialCode,
                    onAskAI = { lang, codeSnippet ->
                        navController.navigate(Screen.AITutor.createRoute(lang, "Lab Code", codeSnippet))
                    }
                )
            }

            // AI Tutor Chatbot
            composable(
                route = Screen.AITutor.routeDefinition,
                arguments = listOf(
                    navArgument("course") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("lesson") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("code") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val rawCourse = backStackEntry.arguments?.getString("course") ?: ""
                val rawLesson = backStackEntry.arguments?.getString("lesson") ?: ""
                val rawCode = backStackEntry.arguments?.getString("code") ?: ""

                val course = try { java.net.URLDecoder.decode(rawCourse, "UTF-8") } catch (e: Exception) { rawCourse }
                val lesson = try { java.net.URLDecoder.decode(rawLesson, "UTF-8") } catch (e: Exception) { rawLesson }
                val code = try { java.net.URLDecoder.decode(rawCode, "UTF-8") } catch (e: Exception) { rawCode }

                AITutorScreen(
                    initialCourse = course,
                    initialLesson = lesson,
                    initialCode = code
                )
            }

            // Progress, Certificates & Profile
            composable(Screen.Progress.route) {
                ProgressAndProfileScreen(
                    courses = courses,
                    progressList = progressList,
                    profile = userProfile,
                    certificates = certificates,
                    onUpdateName = { newName ->
                        coroutineScope.launch {
                            progressRepo.updateLearnerName(newName)
                        }
                    },
                    onViewCertificate = { cert ->
                        activeCertificateDialog = cert
                    }
                )
            }
        }
    }

    // Modal Certificate Dialog
    activeCertificateDialog?.let { cert ->
        CertificateDialog(
            certificate = cert,
            onDismiss = { activeCertificateDialog = null }
        )
    }
}

@Composable
fun CertificateDialog(
    certificate: CertificateEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(2.dp, WarningColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = WarningColor,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CERTIFICATE OF ACHIEVEMENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarningColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This certifies that",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = certificate.learnerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "has successfully mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = certificate.courseTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Credential ID: ${certificate.verificationHash}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Issued: ${certificate.issueDateFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
