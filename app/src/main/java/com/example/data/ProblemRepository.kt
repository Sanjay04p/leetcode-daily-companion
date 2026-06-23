package com.example.data

import kotlinx.coroutines.flow.Flow

class ProblemRepository(
    private val problemDao: ProblemDao,
    private val prepDao: PrepDao
) {
    val allProblems: Flow<List<ProblemEntry>> = problemDao.getAllProblems()

    suspend fun insert(problem: ProblemEntry): Long {
        return problemDao.insertProblem(problem)
    }

    suspend fun delete(problem: ProblemEntry) {
        problemDao.deleteProblem(problem)
    }

    suspend fun deleteById(id: Int) {
        problemDao.deleteProblemById(id)
    }

    // Prep methods
    val resumeProfile: Flow<ResumeProfile?> = prepDao.getResumeProfile()
    val allQuestions: Flow<List<InterviewQuestion>> = prepDao.getAllQuestions()

    suspend fun insertResumeProfile(profile: ResumeProfile): Long {
         return prepDao.insertResumeProfile(profile)
    }

    suspend fun deleteResumeProfile() {
         prepDao.deleteResumeProfile()
    }

    suspend fun insertQuestions(questions: List<InterviewQuestion>) {
         prepDao.insertQuestions(questions)
    }

    suspend fun insertQuestion(question: InterviewQuestion): Long {
         return prepDao.insertQuestion(question)
    }

    suspend fun updateQuestion(question: InterviewQuestion) {
         prepDao.updateQuestion(question)
    }

    suspend fun clearAllQuestions() {
         prepDao.clearAllQuestions()
    }
}
