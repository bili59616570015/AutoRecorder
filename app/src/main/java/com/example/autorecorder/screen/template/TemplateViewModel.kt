package com.example.autorecorder.screen.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autorecorder.api.bili.DataState
import com.example.autorecorder.database.TemplateRepository
import com.example.autorecorder.entity.Template
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TemplateViewModel:ViewModel() {
    private val repository = TemplateRepository()

    private val _uiState = MutableStateFlow(TemplateUiState())
    val uiState = _uiState.asStateFlow()

    fun upsertItem(item: Template) {
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

    fun deleteItem(item: Template) {
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

    fun deleteItem(mid: Long) {
        viewModelScope.launch {
            kotlin.runCatching {
                repository.deleteByMid(mid)
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

    fun onLoad() {
        viewModelScope.launch {
            kotlin.runCatching {
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

    fun clearError() {
        _uiState.update {
            it.copy(
                dataState = DataState.Ready
            )
        }
    }
}

data class TemplateUiState(
    val items: List<Template> = emptyList(),
    val dataState: DataState = DataState.Ready,
)