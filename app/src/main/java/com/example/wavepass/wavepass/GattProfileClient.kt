package com.example.wavepass.wavepass

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import java.util.UUID

class GattProfileClient(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun readProfile(device: BluetoothDevice, onResult: (String?) -> Unit) {
        var gatt: BluetoothGatt? = null

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    g.requestMtu(512)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    g.close()
                }
            }

            override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                g.discoverServices()
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                val service = g.getService(UUID.fromString(BleAdvertiser.SERVICE_UUID))
                val characteristic = service?.getCharacteristic(
                    UUID.fromString(GattProfileServer.PROFILE_CHARACTERISTIC_UUID)
                )

                if (characteristic == null) {
                    onResult(null)
                    g.disconnect()
                    return
                }

                g.readCharacteristic(characteristic)
            }

            override fun onCharacteristicRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                val result = if (status == BluetoothGatt.GATT_SUCCESS) String(value, Charsets.UTF_8) else null
                onResult(result)
                g.disconnect()
            }

            @Deprecated("Deprecated in Java", ReplaceWith(""))
            override fun onCharacteristicRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                val result = if (status == BluetoothGatt.GATT_SUCCESS) {
                    String(characteristic.value ?: ByteArray(0), Charsets.UTF_8)
                } else null
                onResult(result)
                g.disconnect()
            }
        }

        gatt = device.connectGatt(context, false, callback)
    }
}