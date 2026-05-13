package com.example.auctornotes.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["name"], unique = true)]
)
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
