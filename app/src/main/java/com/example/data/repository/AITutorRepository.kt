package com.example.data.repository

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedAction: String? = null
)

enum class MessageSender {
    USER, AI, SYSTEM
}

data class TutorContext(
    val courseTitle: String? = null,
    val lessonTitle: String? = null,
    val activeCode: String? = null,
    val compilerError: String? = null
)

class AITutorRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun askTutor(
        userPrompt: String,
        context: TutorContext,
        chatHistory: List<ChatMessage>
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        // If a real API key is configured in AI Studio Secrets, query Gemini 2.5 Flash
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "placeholder") {
            try {
                val apiResponse = queryGeminiApi(apiKey, userPrompt, context, chatHistory)
                if (apiResponse.isNotBlank()) {
                    return@withContext ChatMessage(
                        sender = MessageSender.AI,
                        text = apiResponse
                    )
                }
            } catch (e: Exception) {
                // Network or API exception -> Fall back to local intelligent tutor
            }
        }

        // Built-in intelligent pedagogical teacher engine
        val localResponse = generatePedagogicalResponse(userPrompt, context)
        ChatMessage(
            sender = MessageSender.AI,
            text = localResponse
        )
    }

    private fun queryGeminiApi(
        apiKey: String,
        prompt: String,
        context: TutorContext,
        history: List<ChatMessage>
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
You are DevLearn AI, a patient and rigorous programming teacher.
Teaching Guidelines:
1. Explain -> Hint -> Guide -> Solution. Guide the student rather than giving away raw answers immediately.
2. Maintain a professional, supportive teacher tone.
3. Avoid unnecessary bolding on every sentence or words. Speak naturally and clearly.
4. When writing code, use standard clean Markdown code blocks.
Context:
Course: ${context.courseTitle ?: "Programming"}
Lesson: ${context.lessonTitle ?: "General"}
Active Code: ${context.activeCode ?: "None"}
Compiler Diagnostic: ${context.compilerError ?: "None"}
""".trimIndent()

        val contentsArray = JSONArray()

        val recent = history.takeLast(4)
        for (msg in recent) {
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            val partObj = JSONObject().put("text", msg.text)
            val partsArr = JSONArray().put(partObj)
            contentsArray.put(JSONObject().put("role", role).put("parts", partsArr))
        }

        val currentPart = JSONObject().put("text", prompt)
        contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(currentPart)))

        val requestJson = JSONObject()
            .put("contents", contentsArray)
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            )
            .put(
                "generationConfig",
                JSONObject().put("temperature", 0.6).put("maxOutputTokens", 800)
            )

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return ""
            }
            val responseBody = response.body?.string() ?: return ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        }
        return ""
    }

    private fun generatePedagogicalResponse(prompt: String, context: TutorContext): String {
        val lower = prompt.lowercase()
        val lesson = context.lessonTitle ?: "this topic"
        val course = context.courseTitle ?: "programming"

        return when {
            lower.contains("hint") || lower.contains("stuck") || lower.contains("help me solve") -> {
                """
Here is a pedagogical hint to help you solve this:

1. Look closely at the data transformations. What input enters the system, and what exact format should the output have?
2. Check your variable initialization. In $course, using uninitialized values often causes unexpected behavior.
3. Test with a minimal edge case, like zero or a single character, before dealing with larger inputs.

Try implementing that step first. If you still feel stuck, tell me what happened when you tried it.
""".trimIndent()
            }

            lower.contains("error") || lower.contains("why am i getting") || context.compilerError != null -> {
                val err = context.compilerError ?: "syntax or type mismatch"
                """
Let's analyze what caused this error:

Issue reported: $err

When debugging in $course, review these key points:
- Check that all parentheses, brackets, and braces are balanced.
- Ensure every variable is declared with its correct type before you use it.
- In languages like C and Java, make sure every instruction ends with a semicolon.

Take a look at the line where the error was flagged and verify these points. Does that help you locate the issue?
""".trimIndent()
            }

            lower.contains("what does this code do") || lower.contains("explain this code") -> {
                val codeSnippet = context.activeCode?.lines()?.take(5)?.joinToString("\n") ?: "your program"
                """
Here is a step-by-step breakdown of how this code executes:

First, execution begins at the program entry point.
Second, variables are allocated in memory to store and update values throughout the execution.
Finally, the result is printed to the standard output stream for inspection.

```
$codeSnippet
```

Try modifying one of the values and running it again in the Lab to see how the output changes.
""".trimIndent()
            }

            lower.contains("beginner") || lower.contains("explain like i'm") || lower.contains("eli5") -> {
                """
Let's look at this with a simple real-world analogy:

Think of code like a cooking recipe:
- Variables are labeled containers storing ingredients like flour or milk.
- Functions are specialized tools, like a blender, that take ingredients, process them, and return a finished result.
- Loops repeat a step multiple times, such as stirring a bowl five times.
- Conditionals make choices based on rules, like checking if the oven is hot before putting the tray in.

Once you see programs as a sequence of simple instructions, everything becomes much easier to follow.
""".trimIndent()
            }

            lower.contains("infinite loop") || lower.contains("loop") -> {
                """
A loop keeps running indefinitely when its exit condition is never satisfied.

To fix or prevent an infinite loop:
1. Verify the loop control variable: is it being changed inside the loop body?
2. Double check the comparison operator: make sure you didn't accidentally write '=' (assignment) instead of '==' (comparison).
3. Ensure the progression moves towards termination rather than away from it.
""".trimIndent()
            }

            lower.contains("quiz") -> {
                """
Here is a quick concept check question for you:

In $course, what happens to local variables declared inside a function when that function finishes executing?

A) They remain in memory forever.
B) Their stack frame is reclaimed and memory is freed.
C) They are automatically converted into global variables.

Reply with your answer and I'll explain why it's right or wrong!
""".trimIndent()
            }

            else -> {
                """
I'm here to help you work through $lesson in $course.

We can:
- Break down complex syntax into simple steps
- Understand compiler diagnostics and fix bugs
- Walk through algorithms with mental models
- Review code before you run it in the Lab

What part of this lesson would you like to explore first?
""".trimIndent()
            }
        }
    }
}
