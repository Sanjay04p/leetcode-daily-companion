package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interview_questions")
data class InterviewQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val suggestedAnswer: String,
    val category: String, // "PROJECT", "TECHNICAL", "BEHAVIORAL", "HR"
    val sourceProject: String?,
    val difficulty: String, // "EASY", "MEDIUM", "HARD"
    val isPracticed: Boolean = false,
    val selfRating: Int = 0, // 0 = not rated, 1–5
    val userAnswer: String? = null,
    val dateGenerated: Long
)
