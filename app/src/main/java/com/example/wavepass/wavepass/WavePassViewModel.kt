package com.example.wavepass.wavepass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wavepass.data.local.Encounter
import com.example.wavepass.data.local.WavePassDatabase
import com.example.wavepass.data.repository.EncounterRepository
import com.example.wavepass.data.repository.WaveProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WavePassViewModel(application: Application) : AndroidViewModel(application) {

    private val bleAdvertiser = BleAdvertiser(application)
    private val bleScanner = BleScanner(application)
    private val gattServer = GattProfileServer(application)
    private val gattClient = GattProfileClient(application)
    private val anonymousId = DeviceIdentity.getOrCreateAnonymousId(application)

    private val database = WavePassDatabase.getInstance(application)
    private val waveProfileRepository = WaveProfileRepository(database.waveProfileDao())
    private val encounterRepository = EncounterRepository(database.encounterDao())

    private val _isDetectionEnabled = MutableStateFlow(false)
    val isDetectionEnabled: StateFlow<Boolean> = _isDetectionEnabled.asStateFlow()

    private val _statusMessage = MutableStateFlow("Detection is off")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _nearbyDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val nearbyDevices: StateFlow<Map<String, DiscoveredDevice>> = _nearbyDevices.asStateFlow()

    val encounters: StateFlow<List<Encounter>> = encounterRepository.getAllEncounters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks remote ids we're already connecting to / have already read this session,
    // so the same nearby device doesn't trigger dozens of redundant GATT connections
    // while it keeps re-advertising every second.
    private val handledRemoteIds = mutableSetOf<String>()

    init {
        // Keep the GATT server's served profile in sync with our own local profile.
        viewModelScope.launch {
            waveProfileRepository.getProfile().collect { profile ->
                val payload = if (profile != null) {
                    ProfilePayload(
                        anonymousId = profile.anonymousId,
                        displayAlias = profile.displayAlias,
                        favoriteArtist = profile.favoriteArtist,
                        favoriteSongTitles = profile.favoriteSongTitles
                    )
                } else {
                    // No profile configured yet — serve a harmless placeholder.
                    ProfilePayload(
                        anonymousId = anonymousId,
                        displayAlias = "Anonymous Phantom",
                        favoriteArtist = "Not set yet",
                        favoriteSongTitles = emptyList()
                    )
                }
                gattServer.updateServedProfile(payload.toJson())
            }
        }
    }

    fun toggleDetection() {
        if (_isDetectionEnabled.value) {
            bleAdvertiser.stopAdvertising()
            bleScanner.stopScanning()
            gattServer.stop()
            _isDetectionEnabled.value = false
            _statusMessage.value = "Detection is off"
            _nearbyDevices.value = emptyMap()
            handledRemoteIds.clear()
        } else {
            gattServer.start()

            bleAdvertiser.startAdvertising(anonymousId) { advertiseSuccess, advertiseMessage ->
                _statusMessage.value = advertiseMessage

                if (advertiseSuccess) {
                    bleScanner.startScanning(
                        onDeviceFound = { discovered -> handleDeviceFound(discovered) },
                        onStatus = { scanSuccess, scanMessage ->
                            if (!scanSuccess) _statusMessage.value = scanMessage
                        }
                    )
                    _isDetectionEnabled.value = true
                }
            }
        }
    }

    private fun handleDeviceFound(discovered: DiscoveredDevice) {
        _nearbyDevices.value = _nearbyDevices.value.toMutableMap().apply {
            put(discovered.remoteAnonymousId, discovered)
        }

        // Only attempt one GATT read per remote id per detection session.
        if (discovered.remoteAnonymousId in handledRemoteIds) return
        handledRemoteIds.add(discovered.remoteAnonymousId)

        gattClient.readProfile(discovered.device) { jsonOrNull ->
            val payload = jsonOrNull?.let { ProfilePayload.fromJson(it) } ?: return@readProfile

            viewModelScope.launch {
                encounterRepository.recordEncounter(
                    Encounter(
                        remoteAnonymousId = payload.anonymousId,
                        remoteDisplayAlias = payload.displayAlias,
                        favoriteArtist = payload.favoriteArtist,
                        favoriteSongTitles = payload.favoriteSongTitles,
                        receivedAtTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleAdvertiser.stopAdvertising()
        bleScanner.stopScanning()
        gattServer.stop()
    }
}