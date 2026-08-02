package com.example.wavepass.wavepass

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

data class DiscoveredDevice(
    val remoteAnonymousId: String,
    val rssi: Int,
    val lastSeenAtMillis: Long,
    val device: BluetoothDevice
)

class BleScanner(context: Context) {

    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE)
            as BluetoothManager).adapter

    private var scanCallback: ScanCallback? = null

    var isScanning = false
        private set

    @SuppressLint("MissingPermission")
    fun startScanning(onDeviceFound: (DiscoveredDevice) -> Unit, onStatus: (Boolean, String) -> Unit) {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            onStatus(false, "Bluetooth LE scanning is not supported on this device.")
            return
        }

        val serviceUuid = ParcelUuid(UUID.fromString(BleAdvertiser.SERVICE_UUID))

        val filter = ScanFilter.Builder()
            .setServiceUuid(serviceUuid)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord?.getServiceData(serviceUuid) ?: return
                val remoteId = String(serviceData, Charsets.UTF_8)

                onDeviceFound(
                    DiscoveredDevice(
                        remoteAnonymousId = remoteId,
                        rssi = result.rssi,
                        lastSeenAtMillis = System.currentTimeMillis(),
                        device = result.device
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                isScanning = false
                onStatus(false, "Scan failed (error $errorCode)")
            }
        }

        scanner.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
        onStatus(true, "Scanning started")
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanCallback?.let { scanner?.stopScan(it) }
        isScanning = false
    }
}