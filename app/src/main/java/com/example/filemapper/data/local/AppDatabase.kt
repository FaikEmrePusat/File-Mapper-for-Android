package com.example.filemapper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.filemapper.data.local.dao.FileNodeDao
import com.example.filemapper.data.local.entity.FileConnection
import com.example.filemapper.data.local.entity.FileNode

/**
 * Room Database for Spatial File Manager.
 * Contains tables for file nodes and their connections.
 */
@Database(
    entities = [FileNode::class, FileConnection::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to FileNode and FileConnection DAOs.
     */
    abstract fun fileNodeDao(): FileNodeDao
    
    companion object {
        const val DATABASE_NAME = "spatial_file_manager_db"
    }
}
