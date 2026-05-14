package com.example.auctornotes.sync

import android.util.Log
import com.example.auctornotes.data.model.Note
import com.example.auctornotes.data.model.Project
import com.example.auctornotes.data.repository.NoteRepository
import com.example.auctornotes.sync.model.MessageTypes
import com.example.auctornotes.sync.model.SyncMessage
import com.example.auctornotes.sync.model.SyncNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncRepository(
    private val noteRepository: NoteRepository,
    private val nsdHelper: NsdHelper,
    private val udpDiscovery: UdpDiscoveryReceiver,
    private val webSocketManager: WebSocketManager,
    private val scope: CoroutineScope
) {
    private var syncJob: Job? = null
    private var activeProjectId: Long? = null

    val isConnected = webSocketManager.isConnected
    val connectionError = webSocketManager.connectionError
    private val _isSearching = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _activeProjectName = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val activeProjectName = _activeProjectName.asStateFlow()

    init {
        // UDP broadcast discovery (primary — works reliably on all LANs)
        scope.launch {
            udpDiscovery.discovered.collect { host ->
                if (host != null && !webSocketManager.isConnected.value) {
                    Log.d("SyncRepository", "UDP discovery → connecting to ${host.ip}:${host.port}")
                    webSocketManager.connect("ws://${host.ip}:${host.port}")
                }
            }
        }

        // mDNS discovery (fallback)
        scope.launch {
            nsdHelper.discoveredService.collect { serviceInfo ->
                if (serviceInfo != null && !webSocketManager.isConnected.value) {
                    val host = serviceInfo.host?.hostAddress
                    val port = serviceInfo.port
                    if (host != null) {
                        Log.d("SyncRepository", "mDNS discovery → connecting to $host:$port")
                        webSocketManager.connect("ws://$host:$port")
                    }
                }
            }
        }

        scope.launch {
            webSocketManager.incomingMessages.collect { message ->
                handleIncomingMessage(message)
            }
        }
    }

    private fun handleIncomingMessage(message: SyncMessage) {
        when (message.type) {
            MessageTypes.SET_PROJECT -> {
                val projectName = message.projectName ?: return
                _activeProjectName.value = projectName
                scope.launch(Dispatchers.IO) {
                    var project = noteRepository.getProjectByName(projectName)
                    if (project == null) {
                        val id = noteRepository.insertProject(Project(name = projectName))
                        project = Project(id = id, name = projectName)
                    }
                    startSyncingProject(project)
                }
            }
            MessageTypes.DELETE_NOTES -> {
                val noteIds = message.noteIds ?: return
                scope.launch(Dispatchers.IO) {
                    noteIds.forEach { id ->
                        val note = noteRepository.getNoteById(id)
                        note?.let { noteRepository.deleteNote(it) }
                    }
                }
            }
        }
    }

    private fun startSyncingProject(project: Project) {
        syncJob?.cancel()
        activeProjectId = project.id
        syncJob = scope.launch(Dispatchers.IO) {
            noteRepository.getNotesByProject(project.id).collectLatest { notes ->
                val syncNotes = notes.map { it.toSyncNote() }
                webSocketManager.sendMessage(
                    SyncMessage(
                        type = MessageTypes.SYNC_NOTES,
                        projectName = project.name,
                        notes = syncNotes
                    )
                )
            }
        }
    }

    fun start() {
        _isSearching.value = true
        udpDiscovery.start()
        nsdHelper.startDiscovery()
    }

    fun connectManually(host: String, port: Int) {
        webSocketManager.connect("ws://$host:$port")
    }

    fun getLocalIpAddress(): String = nsdHelper.getLocalIpAddress()

    fun stop() {
        _isSearching.value = false
        udpDiscovery.stop()
        nsdHelper.stopDiscovery()
        webSocketManager.disconnect()
        syncJob?.cancel()
    }

    private fun Note.toSyncNote() = SyncNote(
        id = id,
        title = title,
        content = content,
        timestamp = timestamp
    )
}
