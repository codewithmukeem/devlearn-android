package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.lab.SafeSandboxedExecutionEngine
import com.example.data.repository.CourseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `verify app name is DevLearn`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DevLearn", appName)
  }

  @Test
  fun `verify all 4 core curriculum courses load from assets`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = CourseRepository(context)
    val courses = repo.getAllCourses()

    assertEquals(4, courses.size)
    val ids = courses.map { it.id }
    assertTrue(ids.contains("c"))
    assertTrue(ids.contains("cpp"))
    assertTrue(ids.contains("java"))
    assertTrue(ids.contains("android"))

    val cCourse = courses.find { it.id == "c" }
    assertNotNull(cCourse)
    assertTrue(cCourse!!.chapters.isNotEmpty())
    assertTrue(cCourse.chapters[0].lessons.isNotEmpty())
  }

  @Test
  fun `verify virtual sandboxed runtime execution`() = runBlocking {
    val engine = SafeSandboxedExecutionEngine()
    val cCode = """
      #include <stdio.h>
      int main() {
          int count = 5;
          printf("Count: %d\n", count);
          return 0;
      }
    """.trimIndent()

    val result = engine.execute(cCode, "c")
    assertTrue(result.success)
    assertEquals("Count: 5", result.stdout.trim())
    assertEquals(0, result.exitCode)
  }
}

