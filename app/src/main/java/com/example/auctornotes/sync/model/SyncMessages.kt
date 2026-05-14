package com.example.auctornotes.sync.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncMessage(
    val type: String,
    val projectName: String? = null,
    val notes: List<SyncNote>? = null,
    val note: SyncNote? = null,
    val noteIds: List<Long>? = null
)

@JsonClass(generateAdapter = true)
data class SyncNote(
    val id: Long,
    val title: String,
    val content: String,
    val timestamp: Long
)

object MessageTypes {
    const val SET_PROJECT = "SET_PROJECT"
    const val SYNC_NOTES = "SYNC_NOTES"
    const val UPDATE_NOTE = "UPDATE_NOTE"
    const val DELETE_NOTES = "DELETE_NOTES"
}
