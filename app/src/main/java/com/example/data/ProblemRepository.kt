package com.example.data

import kotlinx.coroutines.flow.Flow

class ProblemRepository(private val problemDao: ProblemDao) {
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
}
