package com.example.auctornotes

import android.app.Application
import com.example.auctornotes.data.AppDatabase
import com.example.auctornotes.data.repository.NoteRepository
import com.example.auctornotes.sync.NsdHelper
import com.example.auctornotes.sync.SyncRepository
import com.example.auctornotes.sync.UdpDiscoveryReceiver
import com.example.auctornotes.sync.WebSocketManager
import kotlinx.coroutines.MainScope

class AuctorNotesApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database.projectDao(), database.noteDao()) }
    
    val syncRepository by lazy {
        SyncRepository(
            noteRepository = repository,
            nsdHelper = NsdHelper(this),
            udpDiscovery = UdpDiscoveryReceiver(MainScope()),
            webSocketManager = WebSocketManager(),
            scope = MainScope()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // syncRepository.start() // Removed to prevent automatic pairing
    }
}
