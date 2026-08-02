package com.example.wavepass.wavepass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WavePassViewModel(application: Application) : AndroidViewModel(application) {

    private val bleAdvertiser = BleAdvertiser(application)
    private val bleScanner = BleScanner(application)
    private val anonymousId = DeviceIdentity.getOrCreateAnonymousId(application)

    private val _isDetectionEnabled = MutableStateFlow(false)
    val isDetectionEnabled: StateFlow<Boolean> = _isDetectionEnabled.asStateFlow()

    private val _statusMessage = MutableStateFlow("Detection is off")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Nearby devices seen recently, keyed by their anonymous id to avoid duplicates.
    private val _nearbyDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val nearbyDevices: StateFlow<Map<String, DiscoveredDevice>> = _nearbyDevices.asStateFlow()

    fun toggleDetection() {
        if (_isDetectionEnabled.value) {
            bleAdvertiser.stopAdvertising()
            bleScanner.stopScanning()
            _isDetectionEnabled.value = false
            _statusMessage.value = "Detection is off"
            _nearbyDevices.value = emptyMap()
        } else {
            bleAdvertiser.startAdvertising(anonymousId) { advertiseSuccess, advertiseMessage ->
                _statusMessage.value = advertiseMessage

                if (advertiseSuccess) {
                    bleScanner.startScanning(
                        onDeviceFound = { device ->
                            // Ignore our own signal, and update/insert by id (dedupe).
                            if (device.remoteAnonymousId != anonymousId.replace("-", "").take(16)) {
                                _nearbyDevices.value = _nearbyDevices.value.toMutableMap().apply {
                                    put(device.remoteAnonymousId, device)
                                }
                            }
                        },
                        onStatus = { scanSuccess, scanMessage ->
                            if (!scanSuccess) _statusMessage.value = scanMessage
                        }
                    )
                    _isDetectionEnabled.value = true
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleAdvertiser.stopAdvertising()
        bleScanner.stopScanning()
    }
}