package com.example.profilemanager.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(navController: NavController) {
    val context = LocalContext.current
    var wifiState by remember { mutableStateOf<String>("Unknown") }
    var wifiName by remember { mutableStateOf<String>("N/A") }
    var mobileDataState by remember { mutableStateOf<String>("Unknown") }
    var bytesSent by remember { mutableStateOf<Long>(0) }
    var bytesReceived by remember { mutableStateOf<Long>(0) }
    LaunchedEffect(Unit) {
        while (true) {
            bytesSent = TrafficStats.getTotalTxBytes()
            bytesReceived = TrafficStats.getTotalRxBytes()
            delay(10000) // Update every 10 seconds
        }
    }

    LaunchedEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network: Network? = connectivityManager.activeNetwork
            val networkCapabilities: NetworkCapabilities? =
                connectivityManager.getNetworkCapabilities(network)

            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiState = "Connected"
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val connectionInfo = wifiManager.connectionInfo
                        wifiName = connectionInfo.ssid.replace("\"", "")
                    } else {
                        wifiName = "Permission Denied"
                    }
                } else {
                    wifiState = "Disconnected"
                    wifiName = "N/A"
                }
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    mobileDataState = "Connected"
                } else {
                    mobileDataState = "Disconnected"
                }
            } else {
                wifiState = "Disconnected"
                wifiName = "N/A"
                mobileDataState = "Disconnected"
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    wifiState = "Connected"
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val connectionInfo = wifiManager.connectionInfo
                        wifiName = connectionInfo.ssid.replace("\"", "")
                    } else {
                        wifiName = "Permission Denied"
                    }
                } else {
                    wifiState = "Disconnected"
                    wifiName = "N/A"
                }
                if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                    mobileDataState = "Connected"
                } else {
                    mobileDataState = "Disconnected"
                }
            } else {
                wifiState = "Disconnected"
                wifiName = "N/A"
                mobileDataState = "Disconnected"
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NetworkWifi,
                        contentDescription = "Wi-Fi State",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Wi-Fi State: $wifiState")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NetworkWifi,
                        contentDescription = "Wi-Fi Name",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Wi-Fi Name: $wifiName")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CellTower,
                        contentDescription = "Mobile Data State",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Mobile Data State: $mobileDataState")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Upload,
                        contentDescription = "Bytes Sent",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Bytes Sent: ${formatBytes(bytesSent)}")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Bytes Received",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Bytes Received: ${formatBytes(bytesReceived)}")
                }
            }
            Button(
                onClick = {
                    val intent = Intent("com.tailscale.ipn.CONNECT")
                    intent.setPackage("com.tailscale.ipn")
                    val resolveInfo = context.packageManager.queryBroadcastReceivers(intent, 0)

                    if (resolveInfo.isNotEmpty()) {
                        val startIntent = Intent("com.tailscale.ipn.START")
                        startIntent.setPackage("com.tailscale.ipn")
                        context.sendBroadcast(startIntent)
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000)
                            context.sendBroadcast(intent)
                        }
                        Toast.makeText(
                            context,
                            "Attempting to connect to Tailscale...",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Tailscale app is not installed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect Tailscale")
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) {
        return "$bytes B"
    }
    val kb = bytes / 1024.0
    if (kb < 1024) {
        return String.format("%.2f kB", kb)
    }
    val mb = kb / 1024.0
    if (mb < 1024) {
        return String.format("%.2f MB", mb)
    }
    val gb = mb / 1024.0
    if (gb < 1024) {
        return String.format("%.2f GB", gb)
    }
    val tb = gb / 1024.0
    return String.format("%.2f TB", tb)
}