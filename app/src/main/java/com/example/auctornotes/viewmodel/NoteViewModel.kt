package com.example.auctornotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auctornotes.data.model.Note
import com.example.auctornotes.data.model.Project
import com.example.auctornotes.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    val allProjects: Flow<List<Project>> = repository.allProjects

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _notesForCurrentProject = MutableStateFlow<List<Note>>(emptyList())
    val notesForCurrentProject: StateFlow<List<Note>> = _notesForCurrentProject.asStateFlow()

    fun selectProject(project: Project) {
        _currentProject.value = project
        viewModelScope.launch {
            repository.getNotesByProject(project.id).collect {
                _notesForCurrentProject.value = it
            }
        }
    }

    fun addProject(name: String) {
        viewModelScope.launch {
            repository.insertProject(Project(name = name))
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun addNote(projectId: Long, title: String, content: String) {
        viewModelScope.launch {
            repository.insertNote(Note(projectId = projectId, title = title, content = content))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    suspend fun getNoteById(id: Long): Note? {
        return repository.getNoteById(id)
    }
    
    suspend fun getProjectById(id: Long): Project? {
        return repository.getProjectById(id)
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
