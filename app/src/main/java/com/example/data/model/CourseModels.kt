package com.example.data.model

data class Course(
    val id: String,
    val title: String,
    val tagline: String,
    val level: String,
    val estimatedHours: Int,
    val badge: String,
    val accentColor: String,
    val description: String,
    val chapters: List<Chapter>
)

data class Chapter(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val lessons: List<Lesson>
)

data class Lesson(
    val id: String,
    val title: String,
    val order: Int,
    val readTimeMinutes: Int,
    val summary: String,
    val content: String,
    val code: String,
    val expectedOutput: String,
    val practice: PracticeProblem,
    val quizzes: List<QuizItem>
)

data class PracticeProblem(
    val title: String,
    val instructions: String,
    val starterCode: String,
    val hint: String,
    val solutionCode: String,
    val expectedOutput: String
)

data class QuizItem(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class CourseProgressSummary(
    val courseId: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val percentCompleted: Int,
    val lastLessonId: String? = null,
    val lastLessonTitle: String? = null
)
