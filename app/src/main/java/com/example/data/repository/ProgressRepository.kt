package com.example.data.repository

import com.example.data.local.dao.CertificateDao
import com.example.data.local.dao.ProfileDao
import com.example.data.local.dao.ProgressDao
import com.example.data.local.entity.CertificateEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.UserProgressEntity
import com.example.data.model.CourseProgressSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProgressRepository(
    private val progressDao: ProgressDao,
    private val profileDao: ProfileDao,
    private val certificateDao: CertificateDao
) {
    val allProgress: Flow<List<UserProgressEntity>> = progressDao.getAllProgress()
    val userProfile: Flow<UserProfileEntity?> = profileDao.getProfile()
    val allCertificates: Flow<List<CertificateEntity>> = certificateDao.getAllCertificates()

    fun getCourseProgress(courseId: String): Flow<List<UserProgressEntity>> =
        progressDao.getProgressForCourse(courseId)

    suspend fun recordLessonOpened(courseId: String, lessonId: String) {
        val existing = progressDao.getLessonProgress(courseId, lessonId)
        if (existing == null) {
            progressDao.upsertProgress(
                UserProgressEntity(
                    courseId = courseId,
                    lessonId = lessonId,
                    isCompleted = false,
                    lastAccessedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            progressDao.upsertProgress(
                existing.copy(lastAccessedTimestamp = System.currentTimeMillis())
            )
        }
    }

    suspend fun markLessonComplete(courseId: String, lessonId: String) {
        val existing = progressDao.getLessonProgress(courseId, lessonId)
        if (existing == null) {
            progressDao.upsertProgress(
                UserProgressEntity(
                    courseId = courseId,
                    lessonId = lessonId,
                    isCompleted = true,
                    lastAccessedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            progressDao.upsertProgress(
                existing.copy(isCompleted = true, lastAccessedTimestamp = System.currentTimeMillis())
            )
        }
    }

    suspend fun markPracticeCompleted(courseId: String, lessonId: String) {
        val existing = progressDao.getLessonProgress(courseId, lessonId)
        if (existing == null) {
            progressDao.upsertProgress(
                UserProgressEntity(
                    courseId = courseId,
                    lessonId = lessonId,
                    practiceCompleted = true,
                    lastAccessedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            progressDao.upsertProgress(existing.copy(practiceCompleted = true))
        }
    }

    suspend fun recordQuizScore(courseId: String, lessonId: String, score: Int, total: Int) {
        val existing = progressDao.getLessonProgress(courseId, lessonId)
        if (existing == null) {
            progressDao.upsertProgress(
                UserProgressEntity(
                    courseId = courseId,
                    lessonId = lessonId,
                    quizScore = score,
                    quizTotal = total,
                    isCompleted = (score >= (total / 2.0)),
                    lastAccessedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            progressDao.upsertProgress(
                existing.copy(
                    quizScore = score,
                    quizTotal = total,
                    isCompleted = existing.isCompleted || (score >= (total / 2.0))
                )
            )
        }
    }

    suspend fun updateLearnerName(name: String) {
        val profile = profileDao.getProfile().firstOrNull()
        if (profile == null) {
            profileDao.upsertProfile(UserProfileEntity(name = name))
        } else {
            profileDao.updateName(name)
        }
    }

    suspend fun claimCertificate(courseId: String, courseTitle: String, learnerName: String): CertificateEntity {
        val existing = certificateDao.getCertificateForCourse(courseId)
        if (existing != null) return existing

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        val dateStr = dateFormat.format(Date())
        val certId = "DL-" + courseId.uppercase(Locale.US) + "-" + UUID.randomUUID().toString().take(8).uppercase(Locale.US)
        val rawHashData = "$certId-$courseId-$learnerName-${System.currentTimeMillis()}"
        val hash = sha256(rawHashData).take(16).uppercase(Locale.US)

        val cert = CertificateEntity(
            certificateId = certId,
            courseId = courseId,
            courseTitle = courseTitle,
            learnerName = learnerName.ifBlank { "DevLearn Scholar" },
            issueDateFormatted = dateStr,
            verificationHash = hash
        )
        certificateDao.insertCertificate(cert)
        return cert
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
