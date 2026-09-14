package com.kafappstore.ciphervault.data.db

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<CipherProject>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): CipherProject? = projectDao.getProjectById(id)

    suspend fun insertProject(project: CipherProject): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: CipherProject) = projectDao.updateProject(project)

    suspend fun deleteProject(project: CipherProject) = projectDao.deleteProject(project)

    suspend fun deleteProjectById(id: Long) = projectDao.deleteProjectById(id)
}
