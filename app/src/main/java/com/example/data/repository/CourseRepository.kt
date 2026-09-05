package com.example.data.repository

import android.content.Context
import com.example.data.model.Chapter
import com.example.data.model.Course
import com.example.data.model.Lesson
import com.example.data.model.PracticeProblem
import com.example.data.model.QuizItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CourseRepository(private val context: Context) {
    private var cachedCourses: List<Course>? = null

    suspend fun getAllCourses(): List<Course> = withContext(Dispatchers.IO) {
        cachedCourses?.let { return@withContext it }

        val courses = mutableListOf<Course>()
        val courseFiles = listOf(
            "courses/c_course.json",
            "courses/cpp_course.json",
            "courses/java_course.json",
            "courses/android_course.json"
        )

        for (file in courseFiles) {
            try {
                val jsonString = context.assets.open(file).bufferedReader().use { it.readText() }
                val courseObj = JSONObject(jsonString)
                val course = parseCourse(courseObj)
                courses.add(course)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        cachedCourses = courses
        courses
    }

    suspend fun getCourseById(courseId: String): Course? {
        val all = getAllCourses()
        return all.find { it.id.equals(courseId, ignoreCase = true) }
    }

    suspend fun getLesson(courseId: String, lessonId: String): Pair<Course, Lesson>? {
        val course = getCourseById(courseId) ?: return null
        for (chapter in course.chapters) {
            for (lesson in chapter.lessons) {
                if (lesson.id == lessonId) {
                    return Pair(course, lesson)
                }
            }
        }
        return null
    }

    private fun parseCourse(obj: JSONObject): Course {
        val id = obj.getString("id")
        val title = obj.getString("title")
        val tagline = obj.optString("tagline", "")
        val level = obj.optString("level", "Beginner")
        val estimatedHours = obj.optInt("estimatedHours", 15)
        val badge = obj.optString("badge", "DEV")
        val accentColor = obj.optString("accentColor", "#2563EB")
        val description = obj.optString("description", "")

        val chaptersList = mutableListOf<Chapter>()
        val chaptersArray = obj.optJSONArray("chapters") ?: JSONArray()
        for (i in 0 until chaptersArray.length()) {
            val chObj = chaptersArray.getJSONObject(i)
            val chId = chObj.getString("id")
            val chTitle = chObj.getString("title")
            val chDesc = chObj.optString("description", "")
            val chOrder = chObj.optInt("order", i + 1)

            val lessonsList = mutableListOf<Lesson>()
            val lessonsArray = chObj.optJSONArray("lessons") ?: JSONArray()
            for (j in 0 until lessonsArray.length()) {
                val lObj = lessonsArray.getJSONObject(j)
                val lId = lObj.getString("id")
                val lTitle = lObj.getString("title")
                val lOrder = lObj.optInt("order", j + 1)
                val readTime = lObj.optInt("readTimeMinutes", 5)
                val summary = lObj.optString("summary", "")
                val content = lObj.optString("content", "")
                val code = lObj.optString("code", "")
                val expectedOutput = lObj.optString("expectedOutput", "")

                val practiceObj = lObj.optJSONObject("practice")
                val practice = if (practiceObj != null) {
                    PracticeProblem(
                        title = practiceObj.optString("title", "Practice Exercise"),
                        instructions = practiceObj.optString("instructions", ""),
                        starterCode = practiceObj.optString("starterCode", ""),
                        hint = practiceObj.optString("hint", ""),
                        solutionCode = practiceObj.optString("solutionCode", ""),
                        expectedOutput = practiceObj.optString("expectedOutput", "")
                    )
                } else {
                    PracticeProblem(
                        title = "Practice",
                        instructions = "Run the example code to verify output.",
                        starterCode = code,
                        hint = "Review the explanation above.",
                        solutionCode = code,
                        expectedOutput = expectedOutput
                    )
                }

                val quizzesList = mutableListOf<QuizItem>()
                val quizzesArray = lObj.optJSONArray("quizzes") ?: JSONArray()
                for (k in 0 until quizzesArray.length()) {
                    val qObj = quizzesArray.getJSONObject(k)
                    val qId = qObj.optString("id", "q_${lId}_$k")
                    val question = qObj.getString("question")
                    val optionsArray = qObj.getJSONArray("options")
                    val options = mutableListOf<String>()
                    for (optIdx in 0 until optionsArray.length()) {
                        options.add(optionsArray.getString(optIdx))
                    }
                    val correctIndex = qObj.getInt("correctIndex")
                    val explanation = qObj.optString("explanation", "")
                    quizzesList.add(
                        QuizItem(
                            id = qId,
                            question = question,
                            options = options,
                            correctIndex = correctIndex,
                            explanation = explanation
                        )
                    )
                }

                lessonsList.add(
                    Lesson(
                        id = lId,
                        title = lTitle,
                        order = lOrder,
                        readTimeMinutes = readTime,
                        summary = summary,
                        content = content,
                        code = code,
                        expectedOutput = expectedOutput,
                        practice = practice,
                        quizzes = quizzesList
                    )
                )
            }

            chaptersList.add(
                Chapter(
                    id = chId,
                    title = chTitle,
                    description = chDesc,
                    order = chOrder,
                    lessons = lessonsList
                )
            )
        }

        return Course(
            id = id,
            title = title,
            tagline = tagline,
            level = level,
            estimatedHours = estimatedHours,
            badge = badge,
            accentColor = accentColor,
            description = description,
            chapters = chaptersList
        )
    }
}
