package com.example.auctornotes.data.repository

import com.example.auctornotes.data.dao.NoteDao
import com.example.auctornotes.data.dao.ProjectDao
import com.example.auctornotes.data.model.Note
import com.example.auctornotes.data.model.Project
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val projectDao: ProjectDao,
    private val noteDao: NoteDao
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): Project? = projectDao.getProjectById(id)

    suspend fun getProjectByName(name: String): Project? = projectDao.getProjectByName(name)

    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: Project) = projectDao.updateProject(project)

    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)

    fun getNotesByProject(projectId: Long): Flow<List<Note>> = noteDao.getNotesByProject(projectId)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
}
