package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leetcode_problems")
data class ProblemEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leetcodeNumber: Int,
    val title: String,
    val difficulty: String, // "EASY", "MEDIUM", "HARD"
    val status: String,     // "SOLVED", "ATTEMPTED", "SKIPPED"
    val tags: String,       // comma-separated
    val timeTakenSeconds: Int,
    val language: String,
    val notes: String,
    val confidenceRating: Int, // 1 to 5
    val dateLogged: Long
)
