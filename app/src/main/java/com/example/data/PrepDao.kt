package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrepDao {
    // Resume Profile Methods
    @Query("SELECT * FROM resume_profile LIMIT 1")
    fun getResumeProfile(): Flow<ResumeProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResumeProfile(profile: ResumeProfile): Long

    @Query("DELETE FROM resume_profile")
    suspend fun deleteResumeProfile()

    // Interview Questions Methods
    @Query("SELECT * FROM interview_questions ORDER BY dateGenerated DESC")
    fun getAllQuestions(): Flow<List<InterviewQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<InterviewQuestion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: InterviewQuestion): Long

    @Update
    suspend fun updateQuestion(question: InterviewQuestion)

    @Query("DELETE FROM interview_questions")
    suspend fun clearAllQuestions()
}
