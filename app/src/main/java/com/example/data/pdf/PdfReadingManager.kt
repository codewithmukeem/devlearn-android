package com.example.data.pdf

import android.content.Context
import android.content.SharedPreferences

class PdfReadingManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("devlearn_pdf_reading_prefs", Context.MODE_PRIVATE)

    fun getLastReadPage(courseId: String): Int {
        return prefs.getInt("last_page_$courseId", 0)
    }

    fun saveLastReadPage(courseId: String, pageIndex: Int) {
        prefs.edit().putInt("last_page_$courseId", pageIndex).apply()
    }

    fun getBookmarks(courseId: String): List<Int> {
        val raw = prefs.getString("bookmarks_$courseId", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()
    }

    fun toggleBookmark(courseId: String, pageIndex: Int): Boolean {
        val current = getBookmarks(courseId).toMutableList()
        val isNowBookmarked: Boolean
        if (current.contains(pageIndex)) {
            current.remove(pageIndex)
            isNowBookmarked = false
        } else {
            current.add(pageIndex)
            isNowBookmarked = true
        }
        val serialized = current.sorted().joinToString(",")
        prefs.edit().putString("bookmarks_$courseId", serialized).apply()
        return isNowBookmarked
    }

    fun isBookmarked(courseId: String, pageIndex: Int): Boolean {
        return getBookmarks(courseId).contains(pageIndex)
    }
}
