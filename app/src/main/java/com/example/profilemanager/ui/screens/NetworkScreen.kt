package com.example.profilemanager.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

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
        bytesSent = TrafficStats.getTotalTxBytes()
        bytesReceived = TrafficStats.getTotalRxBytes()
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
                .padding(16.dp)
        ) {
            Text(text = "Wi-Fi State: $wifiState")
            Text(text = "Wi-Fi Name: $wifiName")
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = "Mobile Data State: $mobileDataState")
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = "Bytes Sent: $bytesSent")
            Text(text = "Bytes Received: $bytesReceived")
        }
    }
}