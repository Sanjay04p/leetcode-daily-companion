package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_profile")
data class ResumeProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeText: String,
    val lastUpdated: Long
)
