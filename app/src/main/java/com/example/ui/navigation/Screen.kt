package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Courses : Screen("courses")
    data object CourseDetail : Screen("course_detail/{courseId}") {
        fun createRoute(courseId: String) = "course_detail/$courseId"
    }
    data object Lesson : Screen("lesson/{courseId}/{lessonId}") {
        fun createRoute(courseId: String, lessonId: String) = "lesson/$courseId/$lessonId"
    }
    data object VirtualLab : Screen("lab?initialLang={initialLang}&initialCode={initialCode}") {
        fun createRoute(lang: String = "c", code: String = "") =
            "lab?initialLang=$lang&initialCode=${java.net.URLEncoder.encode(code, "UTF-8")}"
    }
    data object AITutor : Screen("ai_tutor?course={course}&lesson={lesson}&code={code}") {
        fun createRoute(course: String = "", lesson: String = "", code: String = "") =
            "ai_tutor?course=${java.net.URLEncoder.encode(course, "UTF-8")}&lesson=${java.net.URLEncoder.encode(lesson, "UTF-8")}&code=${java.net.URLEncoder.encode(code, "UTF-8")}"
    }
    data object Progress : Screen("progress")
}
