package com.example.filemapper.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * State for connection creation mode.
 * 
 * Flow:
 * 1. User long-presses a node -> Context menu appears
 * 2. User selects "Create Connection" -> Selection mode activates
 * 3. Source node is highlighted, user can tap other nodes to select as targets
 * 4. User taps "Connect" button -> Connections are created
 * 5. User taps "Cancel" or connection completes -> Mode exits
 */
@Stable
class ConnectionSelectionState {
    
    /** Whether selection mode is currently active */
    var isActive by mutableStateOf(false)
        private set
    
    /** The source node ID (the node user long-pressed) */
    var sourceNodeId by mutableStateOf<String?>(null)
        private set
    
    /** List of selected target node IDs */
    private val _selectedTargets = mutableStateListOf<String>()
    val selectedTargets: SnapshotStateList<String> = _selectedTargets
    
    /**
     * Start connection selection mode from a source node.
     */
    fun startSelectionMode(sourceId: String) {
        isActive = true
        sourceNodeId = sourceId
        _selectedTargets.clear()
    }
    
    /**
     * Toggle a target node's selection.
     * Cannot select the source node as a target.
     */
    fun toggleTargetSelection(nodeId: String) {
        if (nodeId == sourceNodeId) return // Can't connect to self
        
        if (_selectedTargets.contains(nodeId)) {
            _selectedTargets.remove(nodeId)
        } else {
            _selectedTargets.add(nodeId)
        }
    }
    
    /**
     * Check if a node is selected as a target.
     */
    fun isTargetSelected(nodeId: String): Boolean {
        return _selectedTargets.contains(nodeId)
    }
    
    /**
     * Get all source-target pairs for connection creation.
     * Returns list of (sourceId, targetId) pairs.
     */
    fun getConnectionPairs(): List<Pair<String, String>> {
        val source = sourceNodeId ?: return emptyList()
        return _selectedTargets.map { targetId -> source to targetId }
    }
    
    /**
     * Exit selection mode and clear all state.
     */
    fun cancel() {
        isActive = false
        sourceNodeId = null
        _selectedTargets.clear()
    }
    
    /**
     * Complete the selection and return connection pairs.
     * Automatically exits selection mode.
     */
    fun complete(): List<Pair<String, String>> {
        val pairs = getConnectionPairs()
        cancel()
        return pairs
    }
    
    /**
     * Check if we have any targets selected.
     */
    val hasSelectedTargets: Boolean
        get() = _selectedTargets.isNotEmpty()
    
    /**
     * Number of selected targets.
     */
    val selectedCount: Int
        get() = _selectedTargets.size
}
