package com.example.wavepass.wavepass

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import java.util.UUID

// Hosts a local GATT server exposing this device's WavePass profile as a readable characteristic.
// Nearby devices connect and read this to receive our profile.
class GattProfileServer(private val context: Context) {

    private var gattServer: BluetoothGattServer? = null

    // Updated whenever the local profile changes; served as-is to anyone who reads the characteristic.
    @Volatile
    private var currentProfileBytes: ByteArray = ByteArray(0)

    fun updateServedProfile(profileJson: String) {
        currentProfileBytes = profileJson.toByteArray(Charsets.UTF_8)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                val fullValue = currentProfileBytes
                val chunk = if (offset < fullValue.size) fullValue.copyOfRange(offset, fullValue.size) else ByteArray(0)
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk)
            }
        }

        gattServer = bluetoothManager.openGattServer(context, callback)

        val characteristic = BluetoothGattCharacteristic(
            UUID.fromString(PROFILE_CHARACTERISTIC_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val service = BluetoothGattService(
            UUID.fromString(BleAdvertiser.SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        service.addCharacteristic(characteristic)

        gattServer?.addService(service)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        gattServer?.close()
        gattServer = null
    }

    companion object {
        const val PROFILE_CHARACTERISTIC_UUID = "0000b29a-0000-1000-8000-00805f9b34fb"
    }
}