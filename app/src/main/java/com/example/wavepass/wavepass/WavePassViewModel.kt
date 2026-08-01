package com.example.wavepass.wavepass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WavePassViewModel(application: Application) : AndroidViewModel(application) {

    private val bleAdvertiser = BleAdvertiser(application)
    private val anonymousId = DeviceIdentity.getOrCreateAnonymousId(application)

    private val _isDetectionEnabled = MutableStateFlow(false)
    val isDetectionEnabled: StateFlow<Boolean> = _isDetectionEnabled.asStateFlow()

    private val _statusMessage = MutableStateFlow("Detection is off")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun toggleDetection() {
        if (_isDetectionEnabled.value) {
            bleAdvertiser.stopAdvertising()
            _isDetectionEnabled.value = false
            _statusMessage.value = "Detection is off"
        } else {
            bleAdvertiser.startAdvertising(anonymousId) { success, message ->
                _isDetectionEnabled.value = success
                _statusMessage.value = message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleAdvertiser.stopAdvertising()
    }
}