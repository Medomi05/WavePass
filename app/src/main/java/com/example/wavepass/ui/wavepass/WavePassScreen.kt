package com.example.wavepass.ui.wavepass

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wavepass.wavepass.WavePassViewModel

@Composable
fun WavePassScreen() {

    val context = LocalContext.current
    val viewModel: WavePassViewModel = viewModel()

    val isDetectionEnabled by viewModel.isDetectionEnabled.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val nearbyDevices by viewModel.nearbyDevices.collectAsState()

    val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    fun allPermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    var hasPermissions by remember {
        mutableStateOf(allPermissionsGranted())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->

        hasPermissions = results.values.all { it }

        if (hasPermissions && !isDetectionEnabled) {
            viewModel.toggleDetection()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text = "StreetPass",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isDetectionEnabled) {
                    viewModel.toggleDetection()
                } else {
                    if (allPermissionsGranted()) {
                        hasPermissions = true
                        viewModel.toggleDetection()
                    } else {
                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                    }
                }
            }
        ) {
            Text(
                if (isDetectionEnabled)
                    "Turn off detection"
                else
                    "Turn on detection"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (nearbyDevices.isEmpty()) {
            Text("No nearby devices detected yet.")
        } else {

            Text(
                text = "Nearby devices: ${nearbyDevices.size}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(nearbyDevices.values.toList()) { device ->
                    Text(
                        text = "Device ${device.remoteAnonymousId} — RSSI: ${device.rssi}"
                    )
                }
            }
        }
    }
}