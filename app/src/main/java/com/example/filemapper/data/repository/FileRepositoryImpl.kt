package com.example.filemapper.data.repository

import com.example.filemapper.data.local.dao.FileNodeDao
import com.example.filemapper.data.local.entity.FileConnection
import com.example.filemapper.data.local.entity.FileNode
import com.example.filemapper.data.local.entity.FileNodeType
import com.example.filemapper.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FileRepository.
 * Handles file system operations and database synchronization.
 */
@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileNodeDao: FileNodeDao
) : FileRepository {
    
    companion object {
        // Default spacing for auto-layout of new nodes
        private const val NODE_WIDTH = 120f
        private const val NODE_HEIGHT = 80f
        private const val NODE_SPACING_X = 40f
        private const val NODE_SPACING_Y = 40f
        private const val NODES_PER_ROW = 5
        private const val START_X = 50f
        private const val START_Y = 50f
    }
    
    // ==================== FileNode Operations ====================
    
    /**
     * Sync a directory - scans ONLY the immediate children (first level).
     * Does NOT recurse into subdirectories for performance.
     * 
     * @param path The directory path to sync
     * @return List of newly added FileNodes
     */
    override suspend fun syncDirectory(path: String): List<FileNode> {
        val directory = File(path)
        
        // Validate directory exists and is readable
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        
        // Get only immediate children (first level) - NOT recursive
        val files = directory.listFiles() ?: return emptyList()
        
        // Only get existing nodes for THIS specific parent path (optimization)
        val existingNodeIds = fileNodeDao.getNodeIdsByParent(path).toSet()
        val newNodes = mutableListOf<FileNode>()
        
        // Calculate starting position for new nodes based on existing count
        var nodeIndex = existingNodeIds.size
        
        files.forEach { file ->
            val filePath = file.absolutePath
            
            // Skip if already exists in database for this parent
            if (existingNodeIds.contains(filePath)) {
                return@forEach
            }
            
            // Calculate position in a grid layout
            val row = nodeIndex / NODES_PER_ROW
            val col = nodeIndex % NODES_PER_ROW
            val posX = START_X + col * (NODE_WIDTH + NODE_SPACING_X)
            val posY = START_Y + row * (NODE_HEIGHT + NODE_SPACING_Y)
            
            val newNode = FileNode(
                id = filePath,
                displayName = file.name,
                posX = posX,
                posY = posY,
                parentPath = path,
                type = if (file.isDirectory) FileNodeType.FOLDER else FileNodeType.FILE
            )
            
            newNodes.add(newNode)
            nodeIndex++
        }
        
        // Batch insert new nodes
        if (newNodes.isNotEmpty()) {
            fileNodeDao.insertNodes(newNodes)
        }
        
        return newNodes
    }
    
    override fun observeAllNodes(): Flow<List<FileNode>> {
        return fileNodeDao.observeAllNodes()
    }
    
    override fun observeNodesByParent(parentPath: String): Flow<List<FileNode>> {
        return fileNodeDao.observeNodesByParent(parentPath)
    }
    
    override fun observeRootNodes(): Flow<List<FileNode>> {
        return fileNodeDao.observeRootNodes()
    }
    
    override suspend fun getNodeById(nodeId: String): FileNode? {
        return fileNodeDao.getNodeById(nodeId)
    }
    
    override suspend fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        fileNodeDao.updateNodePosition(nodeId, x, y)
    }
    
    override suspend fun insertNode(node: FileNode) {
        fileNodeDao.insertNode(node)
    }
    
    override suspend fun deleteNode(nodeId: String) {
        // Also delete any connections involving this node
        fileNodeDao.deleteConnectionsForNode(nodeId)
        fileNodeDao.deleteNodeById(nodeId)
    }
    
    override suspend fun deleteAllNodes() {
        fileNodeDao.deleteAllConnections()
        fileNodeDao.deleteAllNodes()
    }
    
    // ==================== FileConnection Operations ====================
    
    override fun observeAllConnections(): Flow<List<FileConnection>> {
        return fileNodeDao.observeAllConnections()
    }
    
    override fun observeConnectionsForNode(nodePath: String): Flow<List<FileConnection>> {
        return fileNodeDao.observeConnectionsForNode(nodePath)
    }
    
    override suspend fun createConnection(sourcePath: String, targetPath: String) {
        // Check if connection already exists
        if (!fileNodeDao.connectionExists(sourcePath, targetPath)) {
            val connection = FileConnection(
                sourcePath = sourcePath,
                targetPath = targetPath
            )
            fileNodeDao.insertConnection(connection)
        }
    }
    
    override suspend fun deleteConnection(connectionId: Long) {
        fileNodeDao.deleteConnectionById(connectionId)
    }
    
    override suspend fun deleteConnectionsForNode(nodePath: String) {
        fileNodeDao.deleteConnectionsForNode(nodePath)
    }
    
    override suspend fun deleteConnectionBetween(path1: String, path2: String) {
        fileNodeDao.deleteConnectionBetween(path1, path2)
    }
    
    override suspend fun getConnectedNodeIds(nodePath: String): List<String> {
        return fileNodeDao.getConnectedNodeIds(nodePath)
    }
}
