package com.kafappstore.ciphervault.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM cipher_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<CipherProject>>

    @Query("SELECT * FROM cipher_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): CipherProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: CipherProject): Long

    @Update
    suspend fun updateProject(project: CipherProject)

    @Delete
    suspend fun deleteProject(project: CipherProject)

    @Query("DELETE FROM cipher_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}
