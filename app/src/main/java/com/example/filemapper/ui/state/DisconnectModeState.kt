package com.example.filemapper.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * State for disconnect mode with multi-select.
 * 
 * Flow:
 * 1. User long-presses a node -> Context menu appears
 * 2. User selects "Bağlantıyı Kes" -> Disconnect mode activates
 * 3. Source node highlighted, only connected nodes are selectable
 * 4. User taps connected nodes to SELECT them for deletion
 * 5. User taps "Kes" button -> All selected connections are deleted
 * 6. User taps "İptal" -> Nothing is deleted, mode exits
 */
@Stable
class DisconnectModeState {
    
    /** Whether disconnect mode is currently active */
    var isActive by mutableStateOf(false)
        private set
    
    /** The source node ID */
    var sourceNodeId by mutableStateOf<String?>(null)
        private set
    
    /** IDs of nodes connected to the source (these are selectable) */
    private val _connectedNodeIds = mutableStateListOf<String>()
    val connectedNodeIds: SnapshotStateList<String> = _connectedNodeIds
    
    /** IDs of nodes selected for disconnection */
    private val _selectedTargets = mutableStateListOf<String>()
    val selectedTargets: SnapshotStateList<String> = _selectedTargets
    
    /**
     * Start disconnect mode from a source node.
     */
    fun startDisconnectMode(sourceId: String, connectedIds: List<String>) {
        isActive = true
        sourceNodeId = sourceId
        _connectedNodeIds.clear()
        _connectedNodeIds.addAll(connectedIds)
        _selectedTargets.clear()
    }
    
    /**
     * Check if a node is a connected neighbor (selectable in disconnect mode).
     */
    fun isConnectedNeighbor(nodeId: String): Boolean {
        return _connectedNodeIds.contains(nodeId)
    }
    
    /**
     * Check if a node is selected for disconnection.
     */
    fun isSelectedForDisconnect(nodeId: String): Boolean {
        return _selectedTargets.contains(nodeId)
    }
    
    /**
     * Toggle selection of a node for disconnection.
     */
    fun toggleSelection(nodeId: String) {
        if (_selectedTargets.contains(nodeId)) {
            _selectedTargets.remove(nodeId)
        } else if (_connectedNodeIds.contains(nodeId)) {
            _selectedTargets.add(nodeId)
        }
    }
    
    /**
     * Check if a node should be dimmed (not source and not connected).
     */
    fun shouldDimNode(nodeId: String): Boolean {
        if (!isActive) return false
        return nodeId != sourceNodeId && !isConnectedNeighbor(nodeId)
    }
    
    /**
     * Complete disconnect mode and return pairs to delete.
     * Returns list of (sourceId, targetId) pairs.
     */
    fun complete(): List<Pair<String, String>> {
        val source = sourceNodeId ?: return emptyList()
        val pairs = _selectedTargets.map { target -> Pair(source, target) }
        cancel()
        return pairs
    }
    
    /**
     * Exit disconnect mode without deleting.
     */
    fun cancel() {
        isActive = false
        sourceNodeId = null
        _connectedNodeIds.clear()
        _selectedTargets.clear()
    }
    
    /** Number of selected targets */
    val selectedCount: Int
        get() = _selectedTargets.size
    
    /** Whether any targets are selected */
    val hasSelectedTargets: Boolean
        get() = _selectedTargets.isNotEmpty()
}
