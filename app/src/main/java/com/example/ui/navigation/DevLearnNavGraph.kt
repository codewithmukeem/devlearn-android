package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.screens.progress.CertificateDialog
import com.example.ui.screens.progress.ProgressAndProfileScreen
import kotlinx.coroutines.launch

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun DevLearnApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()

    // Initialize Database & Repositories
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

    // Hide bottom bar on full-screen reading/lesson view
    val shouldShowBottomBar = currentDestination?.route in listOf(
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
                        val selected = currentDestination?.route?.startsWith(item.route.substringBefore("?")) == true

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
                        onRecordQuizResult = { score, total ->
                            coroutineScope.launch {
                                progressRepo.recordQuizScore(courseId, lessonId, score, total)
                            }
                        }
                    )
                }
            }

            // Virtual Coding Lab
            composable(
                route = Screen.VirtualLab.route,
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
                val initialCode = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("initialCode") ?: "",
                    "UTF-8"
                )

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
                route = Screen.AITutor.route,
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
                val course = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("course") ?: "", "UTF-8")
                val lesson = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("lesson") ?: "", "UTF-8")
                val code = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("code") ?: "", "UTF-8")

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
