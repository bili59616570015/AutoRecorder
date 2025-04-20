package com.example.autorecorder.screen.up

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autorecorder.AutoRecorderApp
import com.example.autorecorder.common.Utils
import com.example.autorecorder.database.TemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class UpViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(UpUiState())
    val uiState = _uiState.asStateFlow()

    fun onLoad() {
        viewModelScope.launch {
            val list = Utils.getCookieJsonNames()
            _uiState.update {
                it.copy(
                    list = list
                )
            }
        }
    }

    fun onDeleteClick(item: String) {
        viewModelScope.launch {
            kotlin.runCatching {
                val file = File(AutoRecorderApp.appContext.filesDir, "${item}.json")
                Utils.deleteFile(file)
                _uiState.update {
                    it.copy(
                        list = Utils.getCookieJsonNames()
                    )
                }
            }.onFailure {
                Toast.makeText(AutoRecorderApp.appContext, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class UpUiState(
    val list: List<String> = emptyList()
)