package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemDao {
    @Query("SELECT * FROM leetcode_problems ORDER BY dateLogged DESC")
    fun getAllProblems(): Flow<List<ProblemEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: ProblemEntry): Long

    @Delete
    suspend fun deleteProblem(problem: ProblemEntry)

    @Query("DELETE FROM leetcode_problems WHERE id = :id")
    suspend fun deleteProblemById(id: Int)
}
