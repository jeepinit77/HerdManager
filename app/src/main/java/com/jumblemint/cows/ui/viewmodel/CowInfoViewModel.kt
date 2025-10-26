package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.AnimalIdentifierMode
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Pasture // Keep if pastures are used directly, which they are for name
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.* 
import kotlinx.coroutines.flow.flatMapLatest // Added specific import
import kotlinx.coroutines.launch

// Updated UiState
data class CowInfoUiState(
    val cow: Cow? = null,
    val mother: Cow? = null,
    val father: Cow? = null,
    val children: List<Cow> = emptyList(),
    val maternalSiblings: List<Cow> = emptyList(), // New
    val paternalSiblings: List<Cow> = emptyList(), // New
    val pastureName: String? = null,
    val activities: List<Activity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val identifierMode: AnimalIdentifierMode = AnimalIdentifierMode.BOTH
)

// Removed @OptIn from class level
class CowInfoViewModel(
    private val repository: CattleRepository,
    private val cowId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(CowInfoUiState())
    val uiState: StateFlow<CowInfoUiState> = _uiState.asStateFlow()

    init {
        loadCowInfo()
    }

    @OptIn(ExperimentalCoroutinesApi::class) // Moved @OptIn to function level
    private fun loadCowInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val cowFlow: Flow<Cow?> = repository.getCowByIdFlow(cowId)

            // Flow for mother: emits Cow? based on cowFlow's motherId
            val motherFlow: Flow<Cow?> = cowFlow.flatMapLatest { cow ->
                cow?.motherId?.let { motherId -> repository.getCowByIdFlow(motherId) } ?: flowOf(null)
            }.distinctUntilChanged()

            // Flow for father: emits Cow? based on cowFlow's fatherId
            val fatherFlow: Flow<Cow?> = cowFlow.flatMapLatest { cow ->
                cow?.fatherId?.let { fatherId -> repository.getCowByIdFlow(fatherId) } ?: flowOf(null)
            }.distinctUntilChanged()
            
            // Flow for maternal siblings
            val maternalSiblingsFlow: Flow<List<Cow>> = cowFlow.flatMapLatest { cow ->
                if (cow?.motherId != null) {
                    // Ensure cow.id is used for the cowId parameter to exclude the current cow
                    repository.getMaternalSiblings(cow.id, cow.motherId!!)
                } else {
                    flowOf(emptyList())
                }
            }.distinctUntilChanged()

            // Flow for paternal siblings
            val paternalSiblingsFlow: Flow<List<Cow>> = cowFlow.flatMapLatest { cow ->
                if (cow?.fatherId != null) {
                    // Ensure cow.id is used for the cowId parameter to exclude the current cow
                    repository.getPaternalSiblings(cow.id, cow.fatherId!!)
                } else {
                    flowOf(emptyList())
                }
            }.distinctUntilChanged()

            // Flows for children (more efficient than filtering allCows)
            val childrenAsMotherFlow: Flow<List<Cow>> = repository.getCalvesByMother(cowId)
            val childrenAsFatherFlow: Flow<List<Cow>> = repository.getCalvesByFather(cowId)

            combine(
                listOf( // Pass flows as a List for this combine overload
                    cowFlow,                    // Flow<Cow?>
                    motherFlow,                 // Flow<Cow?>
                    fatherFlow,                 // Flow<Cow?>
                    childrenAsMotherFlow,       // Flow<List<Cow>>
                    childrenAsFatherFlow,       // Flow<List<Cow>>
                    repository.getAllPastures(),// Flow<List<Pasture>>
                    repository.getActivitiesForCow(cowId), // Flow<List<Activity>>
                    maternalSiblingsFlow,       // Flow<List<Cow>>
                    paternalSiblingsFlow,        // Flow<List<Cow>>
                    repository.getAnimalIdentifierModeFlow()
                )
            ) { values: Array<Any?> -> // Lambda now takes an Array<Any?>
                // Extract and cast values by index
                @Suppress("UNCHECKED_CAST")
                val currentCowNullable = values[0] as Cow?
                @Suppress("UNCHECKED_CAST")
                val mother = values[1] as Cow?
                @Suppress("UNCHECKED_CAST")
                val father = values[2] as Cow?
                @Suppress("UNCHECKED_CAST")
                val childrenFromMother = values[3] as List<Cow>
                @Suppress("UNCHECKED_CAST")
                val childrenFromFather = values[4] as List<Cow>
                @Suppress("UNCHECKED_CAST")
                val pastures = values[5] as List<Pasture>
                @Suppress("UNCHECKED_CAST")
                val activities = values[6] as List<Activity>
                @Suppress("UNCHECKED_CAST")
                val matSiblings = values[7] as List<Cow>
                @Suppress("UNCHECKED_CAST")
                val patSiblings = values[8] as List<Cow>
                val identifierMode = values[9] as AnimalIdentifierMode

                val currentCow = currentCowNullable ?: return@combine CowInfoUiState(
                    isLoading = false,
                    error = "Cow not found"
                )

                val combinedChildren = (childrenFromMother + childrenFromFather)
                    .distinctBy { it.id }
                    .sortedByDescending { it.birthDate }

                val pastureName = currentCow.pastureId?.let { pId -> pastures.find { it.id == pId }?.name }

                CowInfoUiState(
                    cow = currentCow,
                    mother = mother,
                    father = father,
                    children = combinedChildren,
                    maternalSiblings = matSiblings,
                    paternalSiblings = patSiblings,
                    pastureName = pastureName,
                    activities = activities.sortedByDescending { it.date },
                    isLoading = false,
                    error = null,
                    identifierMode = identifierMode
                )
            }.catch { e -> 
                _uiState.value = CowInfoUiState(isLoading = false, error = "Error loading cow information: ${e.message}")
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun toggleWatch() {
        viewModelScope.launch {
            _uiState.value.cow?.let { cow ->
                try {
                    repository.updateCow(cow.copy(isWatched = !cow.isWatched))
                    // The reactive flows should update the UI automatically.
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = "Error updating watch status: ${e.message}")
                    }
                }
            }
        }
    }
}
// CowInfoData class is no longer needed.
