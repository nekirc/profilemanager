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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(navController: NavController) {
    val context = LocalContext.current
    var networkDevices by remember { mutableStateOf<List<NetworkDevice>>(emptyList()) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        }
    )

    SideEffect {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasLocationPermission = fineLocationPermission || coarseLocationPermission

        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            networkDevices = getNetworkDevices(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Devices") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            LazyColumn {
                items(networkDevices) { device ->
                    NetworkDeviceItem(device = device)
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun NetworkDeviceItem(device: NetworkDevice) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "Adapter: ${device.adapterName}")
        Text(text = "IP Address: ${device.ipAddress}")
        Text(text = "MAC Address: ${device.macAddress}")
        Text(text = "SSID: ${device.ssid}")
        Text(text = "Bytes Received: ${device.bytesReceived}")
        Text(text = "Bytes Sent: ${device.bytesSent}")
        Spacer(modifier = Modifier.padding(4.dp))
    }
}
fun getNetworkDevices(context: Context): List<NetworkDevice> {
    val networkDevices = mutableListOf<NetworkDevice>()
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (!isConnectedToWifi(context)) {
        networkDevices.add(
            NetworkDevice(
                adapterName = "Not connected to Wi-Fi",
                ipAddress = "N/A",
                macAddress = "N/A",
                ssid = "N/A",
                bytesReceived = 0,
                bytesSent = 0
            )
        )
        return networkDevices
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networks: Array<Network> = connectivityManager.allNetworks
        for (network in networks) {
            val networkCapabilities: NetworkCapabilities? =
                connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    if (networkInterface.isUp && networkInterface.isLoopback.not()) {
                        val inetAddresses = networkInterface.inetAddresses
                        while (inetAddresses.hasMoreElements()) {
                            val inetAddress = inetAddresses.nextElement()
                            if (inetAddress is Inet4Address) {
                                val ipAddress = inetAddress.hostAddress ?: ""
                                val macAddress = getMacAddress(networkInterface)
                                val ssid = getSSID(context, networkCapabilities)
                                val bytesReceived = TrafficStats.getTotalRxBytes()
                                val bytesSent = TrafficStats.getTotalTxBytes()
                                networkDevices.add(
                                    NetworkDevice(
                                        adapterName = networkInterface.displayName ?: "",
                                        ipAddress = ipAddress,
                                        macAddress = macAddress,
                                        ssid = ssid,
                                        bytesReceived = bytesReceived,
                                        bytesSent = bytesSent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in Collections.list(networkInterfaces)) {
                if (networkInterface.isUp && networkInterface.isLoopback.not()) {
                    val inetAddresses = networkInterface.inetAddresses
                    for (inetAddress in Collections.list(inetAddresses)) {
                        if (inetAddress is Inet4Address) {
                            val ipAddress = inetAddress.hostAddress ?: ""
                            val macAddress = getMacAddress(networkInterface)
                            val ssid = getSSID(context, null)
                            val bytesReceived = TrafficStats.getTotalRxBytes()
                            val bytesSent = TrafficStats.getTotalTxBytes()
                            networkDevices.add(
                                NetworkDevice(
                                    adapterName = networkInterface.displayName ?: "",
                                    ipAddress = ipAddress,
                                    macAddress = macAddress,
                                    ssid = ssid,
                                    bytesReceived = bytesReceived,
                                    bytesSent = bytesSent
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return networkDevices
}
fun getMacAddress(networkInterface: NetworkInterface): String {
    return try {
        val macBytes = networkInterface.hardwareAddress
        if (macBytes != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return "Randomized MAC"
            } else {
                return macBytes.joinToString(":") { String.format("%02X", it) }
            }
        } else {
            "N/A"
        }
    } catch (e: Exception) {
        return "N/A"
    }
}

fun getSSID(context: Context, networkCapabilities: NetworkCapabilities?): String {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
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
            connectionInfo.ssid.replace("\"", "")
        } else {
            "Permission Denied"
        }
    } else {
        "N/A"
    }
}

fun isConnectedToWifi(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } else {
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        return networkInfo.type == ConnectivityManager.TYPE_WIFI
    }
}
