package com.example.filemapper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a visual connection between two file nodes.
 * Used to draw edges/lines between connected files on the canvas.
 */
@Entity(tableName = "file_connections")
data class FileConnection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                  // Auto-generated ID
    val sourcePath: String,            // Source file/folder path
    val targetPath: String             // Target file/folder path
)
