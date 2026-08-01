package com.example.wavepass.wavepass

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

// Wraps BLE advertising: broadcasts a small "I'm here" packet containing
// this device's anonymous id, so nearby devices running WavePass can detect it.
class BleAdvertiser(context: Context) {

    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE)
            as android.bluetooth.BluetoothManager).adapter

    private var advertiseCallback: AdvertiseCallback? = null

    var isAdvertising = false
        private set

    @SuppressLint("MissingPermission") // Permission is checked by the caller before invoking this.
    fun startAdvertising(anonymousId: String, onStatus: (Boolean, String) -> Unit) {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            onStatus(false, "Bluetooth LE advertising is not supported on this device.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        // Truncate the UUID down to its first 16 hex chars (8 bytes) to keep the packet small.
        // This is enough entropy to distinguish devices for our purposes.
        val shortId = anonymousId.replace("-", "").take(16)
        val serviceUuid = ParcelUuid(UUID.fromString(SERVICE_UUID))

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(serviceUuid)
            .addServiceData(serviceUuid, shortId.toByteArray(Charsets.UTF_8))
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                isAdvertising = true
                onStatus(true, "Advertising started")
            }

            override fun onStartFailure(errorCode: Int) {
                isAdvertising = false
                onStatus(false, "Failed to start advertising (error $errorCode)")
            }
        }

        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        advertiseCallback?.let { advertiser?.stopAdvertising(it) }
        isAdvertising = false
    }

    companion object {
        // Custom UUID identifying WavePass devices specifically, so we don't
        // pick up unrelated BLE devices/beacons during scanning later.
        const val SERVICE_UUID = "0000b299-0000-1000-8000-00805f9b34fb"
    }
}