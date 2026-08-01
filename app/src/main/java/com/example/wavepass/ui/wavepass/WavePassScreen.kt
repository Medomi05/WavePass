package com.example.wavepass.ui.wavepass

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wavepass.wavepass.WavePassViewModel

@Composable
fun WavePassScreen() {
    val viewModel: WavePassViewModel = viewModel()
    val isDetectionEnabled by viewModel.isDetectionEnabled.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var hasPermissions by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) viewModel.toggleDetection()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("WavePass", style = MaterialTheme.typography.headlineSmall)
        Text(statusMessage, style = MaterialTheme.typography.bodyMedium)

        Button(onClick = {
            if (isDetectionEnabled) {
                viewModel.toggleDetection()
            } else {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            }
        }) {
            Text(if (isDetectionEnabled) "Turn off detection" else "Turn on detection")
        }
    }
}