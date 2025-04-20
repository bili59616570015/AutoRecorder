package com.example.autorecorder.screen.streamer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autorecorder.api.bili.DataState
import com.example.autorecorder.database.StreamerRepository
import com.example.autorecorder.entity.Streamer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StreamerViewModel: ViewModel() {
    private val repository = StreamerRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState
        .onSubscription {
            if (_uiState.value.items.isEmpty()) {
                onLoad()
            } else {
                _uiState.value
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState(),
        )

    fun upsertItem(item: Streamer) {
        viewModelScope.launch {
            kotlin.runCatching {
                repository.upsertItem(item)
                val items = repository.getAllItems()
                _uiState.update {
                    it.copy(
                        dataState = DataState.Loaded,
                        items = items
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        dataState = DataState.Error(error.message ?: "An error occurred")
                    )
                }
            }
        }
    }

    // Function to delete an item
    fun deleteItem(item: Streamer) {
        viewModelScope.launch {
            kotlin.runCatching {
                repository.deleteItem(item)
                val items = repository.getAllItems()
                _uiState.update {
                    it.copy(
                        dataState = DataState.Loaded,
                        items = items
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        dataState = DataState.Error(error.message ?: "An error occurred")
                    )
                }
            }
        }
    }

    // Function to fetch all items
    private fun fetchAllItems() {
        viewModelScope.launch {
            kotlin.runCatching {
                val items = repository.getAllItems()
                _uiState.update {
                    it.copy(
                        dataState = DataState.Loaded,
                        items = items,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        dataState = DataState.Error(error.message ?: "An error occurred")
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(
                dataState = DataState.Ready
            )
        }
    }

    fun onLoad() {
        fetchAllItems()
    }
}

data class HomeUiState(
    val items: List<Streamer> = emptyList(),
    val dataState: DataState = DataState.Ready,
    val adbConnected: Boolean = false,
)