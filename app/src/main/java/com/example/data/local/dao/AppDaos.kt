package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CertificateEntity
import com.example.data.local.entity.LabSnippetEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM user_progress WHERE courseId = :courseId")
    fun getProgressForCourse(courseId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress")
    fun getAllProgress(): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE courseId = :courseId AND lessonId = :lessonId LIMIT 1")
    suspend fun getLessonProgress(courseId: String, lessonId: String): UserProgressEntity?

    @Query("SELECT * FROM user_progress ORDER BY lastAccessedTimestamp DESC LIMIT 1")
    fun getMostRecentProgress(): Flow<UserProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: UserProgressEntity)

    @Query("UPDATE user_progress SET isCompleted = 1, lastAccessedTimestamp = :timestamp WHERE courseId = :courseId AND lessonId = :lessonId")
    suspend fun markLessonCompleted(courseId: String, lessonId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_progress SET practiceCompleted = 1 WHERE courseId = :courseId AND lessonId = :lessonId")
    suspend fun markPracticeCompleted(courseId: String, lessonId: String)

    @Query("UPDATE user_progress SET quizScore = :score, quizTotal = :total WHERE courseId = :courseId AND lessonId = :lessonId")
    suspend fun recordQuizResult(courseId: String, lessonId: String, score: Int, total: Int)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun getProfile(id: String = "primary_user"): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET name = :name WHERE id = 'primary_user'")
    suspend fun updateName(name: String)

    @Query("UPDATE user_profile SET streakDays = streakDays + 1, lastActiveDate = :timestamp WHERE id = 'primary_user'")
    suspend fun incrementStreak(timestamp: Long = System.currentTimeMillis())
}

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates ORDER BY certificateId DESC")
    fun getAllCertificates(): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE courseId = :courseId LIMIT 1")
    suspend fun getCertificateForCourse(courseId: String): CertificateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: CertificateEntity)
}

@Dao
interface LabDao {
    @Query("SELECT * FROM lab_snippets ORDER BY savedTimestamp DESC")
    fun getAllSnippets(): Flow<List<LabSnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: LabSnippetEntity)

    @Query("DELETE FROM lab_snippets WHERE id = :id")
    suspend fun deleteSnippet(id: Int)
}
