package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.auth.AuthService
import com.jumblemint.cows.data.model.Herd
import com.jumblemint.cows.data.model.HerdMember
import com.jumblemint.cows.data.model.HerdRole
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class HerdViewModel(
    private val repository: CattleRepository,
    private val authService: AuthService,
    private val syncService: SyncService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HerdUiState())
    val uiState: StateFlow<HerdUiState> = _uiState.asStateFlow()
    
    init {
        loadUserHerds()
    }
    
    private fun loadUserHerds() {
        viewModelScope.launch {
            // Get current user (Firebase or local)
            authService.currentUser.collect { currentUser ->
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@collect
                }
                
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                try {
                    // Get herds where user is a member
                    combine(
                        repository.getHerdsByUser(currentUser.uid),
                        repository.getAllActiveHerds()
                    ) { memberships, allHerds ->
                        val herdsWithRoles = memberships.mapNotNull { membership ->
                            val herd = allHerds.find { it.id == membership.herdId }
                            if (herd != null) {
                                HerdWithRole(
                                    herd = herd,
                                    role = membership.role,
                                    memberCount = 1 // TODO: Get actual member count
                                )
                            } else null
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            herds = herdsWithRoles,
                            isLoading = false,
                            error = null
                        )
                    }.collect { }
                    
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load herds"
                    )
                }
            }
        }
    }
    
    fun createHerd(name: String, description: String) {
        viewModelScope.launch {
            // Get current user (Firebase or local)
            val currentUser = authService.currentUser.first()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val herdId = UUID.randomUUID().toString()
                val herd = Herd(
                    id = herdId,
                    name = name,
                    description = description.takeIf { it.isNotBlank() },
                    ownerId = currentUser.uid,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                // Create herd
                repository.insertHerd(herd)
                
                // Add creator as owner
                val membership = HerdMember(
                    herdId = herdId,
                    userId = currentUser.uid,
                    role = HerdRole.OWNER,
                    joinedAt = System.currentTimeMillis()
                )
                repository.insertHerdMember(membership)
                
                // Sync to Firestore
                syncService.syncUserData(currentUser.uid)
                
                // Reload herds
                loadUserHerds()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create herd"
                )
            }
        }
    }
    
    fun selectHerd(herdId: String) {
        viewModelScope.launch {
            val currentUser = authService.currentUser.first()
            if (currentUser == null) return@launch
            
            _uiState.value = _uiState.value.copy(selectedHerdId = herdId)
            
            // Start real-time sync for user
            syncService.startRealtimeSync(currentUser.uid)
            
            // Perform initial sync
            syncService.syncUserData(currentUser.uid)
        }
    }
    
    fun inviteUser(herdId: String, email: String) {
        // TODO: Implement user invitation system
        // This would typically involve:
        // 1. Send invitation email/notification
        // 2. Create pending invitation record
        // 3. Handle invitation acceptance
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class HerdUiState(
    val herds: List<HerdWithRole> = emptyList(),
    val selectedHerdId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HerdWithRole(
    val herd: Herd,
    val role: HerdRole,
    val memberCount: Int
)