package com.example.autorecorder.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autorecorder.adb.AdbRepository
import com.example.autorecorder.api.bili.BilibiliUseCase
import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.common.adbCommands
import com.example.autorecorder.database.StreamerRepository
import com.example.autorecorder.entity.Streamer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
    private val useCase = BilibiliUseCase()
    private val adbRepository = AdbRepository()
    private val streamerRepository = StreamerRepository()
    private val _uiState = MutableStateFlow<HomeUiState?>(null)
    val uiState = _uiState.asStateFlow()

    fun onLoad() {
        viewModelScope.launch {
            kotlin.runCatching {
                val adbConnected = adbRepository.isConnected()
                val items = try {
                    streamerRepository.getAllItems()
                } catch (e: Exception) {
                    emptyList()
                }
                _uiState.update {
                    (it ?: HomeUiState()).copy(
                        adbConnected = adbConnected,
                        streamerList = items,
                    )
                }
            }
        }
    }

    fun onAdbConnectButtonClick() {
        viewModelScope.launch {
            val connected = _uiState.value?.adbConnected ?: false
            if (connected) {
                adbRepository.disconnect()
            } else {
                adbRepository.reset()
                adbRepository.connect("127.0.0.1", SharedPreferencesHelper.adbPort)
            }
            val adbConnected = adbRepository.isConnected()
            _uiState.update {
                (it ?: HomeUiState()).copy(
                    adbConnected = adbConnected,
                )
            }
        }
    }

    fun onTestCommandClick(command: String) {
        viewModelScope.launch {
            val adbCommands = command.adbCommands()
            if (adbCommands.isEmpty()) return@launch
            kotlin.runCatching {
                adbRepository.connect("127.0.0.1", SharedPreferencesHelper.adbPort)
                adbCommands.forEach {
                    adbRepository.execute(it)
                }
            }.onFailure { error ->

            }
        }
    }

    fun onPingClick(onResult: (String) -> Unit) {
        viewModelScope.launch {
            kotlin.runCatching {
                _uiState.update {
                    (it ?: HomeUiState()).copy(
                        isLoading = true
                    )
                }
                val cdn = useCase.ping()?.name?.lowercase() ?: return@runCatching
                SharedPreferencesHelper.upCdn = cdn
                onResult(cdn)
                _uiState.update {
                    (it ?: HomeUiState()).copy(
                        isLoading = false
                    )
                }
            }.onFailure { error ->

            }
        }
    }
}

data class HomeUiState(
    val adbConnected: Boolean = false,
    val streamerList: List<Streamer> = emptyList(),
    val isLoading: Boolean = false,
)