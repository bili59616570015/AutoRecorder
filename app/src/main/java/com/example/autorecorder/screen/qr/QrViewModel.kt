package com.example.autorecorder.screen.qr

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autorecorder.AutoRecorderApp
import com.example.autorecorder.api.bili.BilibiliRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QrViewModel: ViewModel() {
    private val bilibiliRepository = BilibiliRepository()
    private val _uiState = MutableStateFlow(QrUiState())
    val uiState = _uiState.asStateFlow()

    fun onLoad() {
        viewModelScope.launch {
            val qrData = kotlin.runCatching {
                bilibiliRepository.getAuthCode()
            }.onFailure { error ->
                Toast.makeText(AutoRecorderApp.appContext, error.message, Toast.LENGTH_SHORT).show()
            }.getOrNull()
            _uiState.update {
                it.copy(
                    qrData = qrData
                )
            }
        }
    }

    fun onQrActivateButtonClick(onNext: () -> Unit) {
        viewModelScope.launch {
            _uiState.value.qrData?.let {
                kotlin.runCatching {
                    bilibiliRepository.loginByQrCode(it.first)
                }.onSuccess {
                    onNext()
                }.onFailure {
                    Toast.makeText(AutoRecorderApp.appContext, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onClear() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    qrData = null
                )
            }
        }
    }
}

data class QrUiState(
    val qrData: Pair<String, String>? = null
)