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

        // If a real API key is configured in AI Studio Secrets, query Gemini 3.5 Flash
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

        // Built-in intelligent pedagogical fallback engine
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
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
You are DevLearn AI, a master programming tutor and curriculum architect.
Your teaching philosophy:
1. Explain concepts simply with structured clarity.
2. Teach the underlying mental model rather than just spitting out solutions.
3. If an exercise or practice problem is mentioned, give progressive hints first.
4. Keep explanations concise, encouraging, and technically rigorous.
Context:
Course: ${context.courseTitle ?: "Programming"}
Lesson: ${context.lessonTitle ?: "General"}
Active Code: ${context.activeCode ?: "None"}
Compiler Diagnostic: ${context.compilerError ?: "None"}
""".trimIndent()

        val contentsArray = JSONArray()

        // Include recent history (last 4 messages for token discipline)
        val recent = history.takeLast(4)
        for (msg in recent) {
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            val partObj = JSONObject().put("text", msg.text)
            val partsArr = JSONArray().put(partObj)
            contentsArray.put(JSONObject().put("role", role).put("parts", partsArr))
        }

        // Current user message
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
        val lesson = context.lessonTitle ?: "the current topic"
        val course = context.courseTitle ?: "programming"

        return when {
            lower.contains("hint") || lower.contains("stuck") || lower.contains("help me solve") -> {
                """
💡 **Tutor Hint for $lesson:**

1. **Step Back & Observe:** Break the problem down into input, transformation, and output.
2. **Variable Alignment:** Check whether your types (e.g. `int` vs `float` or `String`) match what you're printing.
3. **Execution Trace:** Walk line-by-line mentally or click **Run in Lab** to inspect the intermediate values.

Would you like me to guide you through the first line step-by-step?
""".trimIndent()
            }

            lower.contains("error") || lower.contains("why am i getting") || context.compilerError != null -> {
                val err = context.compilerError ?: "syntax or typing discrepancy"
                """
🔍 **Error Diagnostic Breakdown:**

- **Issue:** The compiler flagged: `$err`.
- **Root Cause:** In $course, statements require strict syntax:
  • Ensure every statement ends with a semicolon (`;`).
  • Verify all open brackets `{ ( [` have corresponding closing matches `} ) ]`.
  • Check that all variables are declared before they are referenced.

**Next Action:** Check the highlighted line, update the syntax, and press **Run** in the Virtual Lab!
""".trimIndent()
            }

            lower.contains("what does this code do") || lower.contains("explain this code") -> {
                val codeSnippet = context.activeCode?.lines()?.take(5)?.joinToString("\n") ?: "your program"
                """
📖 **Code Walkthrough:**

Here is what this code does in $course:
1. **Entry Point:** Execution begins at the main routine, initializing memory.
2. **State & Logic:** It evaluates expressions and assigns results to scoped variables.
3. **Output Stream:** It pushes formatted text to the standard console output so the user can verify results.

```
$codeSnippet
```

Notice how clean and predictable the execution flow is!
""".trimIndent()
            }

            lower.contains("beginner") || lower.contains("explain like i'm 5") || lower.contains("eli5") -> {
                """
🌱 **Beginner Explanation:**

Think of code like a cooking recipe:
- **Variables** are labelled jars storing ingredients (like numbers or words).
- **Functions** are kitchen appliances that take ingredients, do one job (like baking), and hand you back a finished dish.
- **Loops** are repeating an instruction (like: "stir the soup 5 times").
- **Conditionals (if/else)** are decisions (like: "if soup is hot, serve it; else heat for 1 minute").

You're doing great—practice is what makes these patterns second nature!
""".trimIndent()
            }

            lower.contains("infinite loop") || lower.contains("loop") -> {
                """
🔄 **Understanding Loop Termination:**

A loop becomes infinite when its **continuation condition** never becomes `false`.
- In a `for (int i = 0; i < N; i++)` loop, make sure `i` is being incremented towards `N`.
- In a `while (condition)` loop, verify that the condition variables are modified inside the loop body!
""".trimIndent()
            }

            else -> {
                """
👋 Hello! I am your **DevLearn AI Tutor** for **$course**.

I'm here to help you master **$lesson**. You can ask me to:
• Break down any code line by line
• Explain compiler or runtime errors
• Give progressive hints on practice challenges
• Clarify memory or architecture concepts

What concept would you like to explore together?
""".trimIndent()
            }
        }
    }
}
