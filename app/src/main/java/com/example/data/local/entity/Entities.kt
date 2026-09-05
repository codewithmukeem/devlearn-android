package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress", primaryKeys = ["courseId", "lessonId"])
data class UserProgressEntity(
    val courseId: String,
    val lessonId: String,
    val isCompleted: Boolean = false,
    val quizScore: Int = 0,
    val quizTotal: Int = 0,
    val practiceCompleted: Boolean = false,
    val lastAccessedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "primary_user",
    val name: String = "Aspiring Developer",
    val goal: String = "Full Stack Engineer",
    val streakDays: Int = 3,
    val lastActiveDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val certificateId: String,
    val courseId: String,
    val courseTitle: String,
    val learnerName: String,
    val issueDateFormatted: String,
    val verificationHash: String
)

@Entity(tableName = "lab_snippets")
data class LabSnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    val code: String,
    val savedTimestamp: Long = System.currentTimeMillis()
)
