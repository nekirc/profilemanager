package com.example.profilemanager.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.profilemanager.data.DataStoreManager
import com.example.profilemanager.data.Profile
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

data class ProfileData(val name: String, var bytesSent: Long, var bytesReceived: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(navController: NavController) {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(context)
    val profiles: List<Profile> by dataStoreManager.getAllProfiles()
        .collectAsState(initial = emptyList())
    var wifiState by remember { mutableStateOf("Unknown") }
    var wifiName by remember { mutableStateOf("N/A") }
    var mobileDataState by remember { mutableStateOf("Unknown") }
    var ipAddresses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val systemUiController = rememberSystemUiController()
    val isDarkTheme by dataStoreManager.isDarkMode.collectAsState(initial = false)
    val bytesSentHistory = remember { mutableStateListOf<Long>() }
    val bytesReceivedHistory = remember { mutableStateListOf<Long>() }
    var currentBytesSent by remember { mutableStateOf(0L) }
    var currentBytesReceived by remember { mutableStateOf(0L) }
    val profileDataList = remember { mutableStateListOf<ProfileData>() }

    LaunchedEffect(systemUiController, isDarkTheme) {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = !isDarkTheme
        )
    }
    LaunchedEffect(profiles) {
        // Clear the list before adding new data
        profileDataList.clear()
        // Initialize profileDataList with real profile names
        profiles.forEach { profile ->
            profileDataList.add(ProfileData(profile.name, 0, 0))
        }
    }
    LaunchedEffect(Unit) {
        var previousBytesSent = TrafficStats.getTotalTxBytes()
        var previousBytesReceived = TrafficStats.getTotalRxBytes()
        while (true) {
            val currentTotalBytesSent = TrafficStats.getTotalTxBytes()
            val currentTotalBytesReceived = TrafficStats.getTotalRxBytes()

            val bytesSentDiff = currentTotalBytesSent - previousBytesSent
            val bytesReceivedDiff = currentTotalBytesReceived - previousBytesReceived

            currentBytesSent = bytesSentDiff
            currentBytesReceived = bytesReceivedDiff

            bytesSentHistory.add(bytesSentDiff)
            bytesReceivedHistory.add(bytesReceivedDiff)

            previousBytesSent = currentTotalBytesSent
            previousBytesReceived = currentTotalBytesReceived

            // Keep only the last 50 data points
            if (bytesSentHistory.size > 50) {
                bytesSentHistory.removeAt(0)
            }
            if (bytesReceivedHistory.size > 50) {
                bytesReceivedHistory.removeAt(0)
            }
            delay(1000) // Update every 1 second
        }
    }
    LaunchedEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        while (true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network: Network? = connectivityManager.activeNetwork
                val networkCapabilities: NetworkCapabilities? =
                    connectivityManager.getNetworkCapabilities(network)

                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        wifiState = "Connected"
                        // Check if the device is connected to a Wi-Fi network
                        if (wifiManager.connectionInfo.networkId != -1) {
                            // Check for location permissions
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                // Check if location service is enabled
                                if (isLocationServiceEnabled(context)) {
                                    val connectionInfo = wifiManager.connectionInfo
                                    // Handle <unknown ssid>
                                    wifiName = if (connectionInfo.ssid == "<unknown ssid>") {
                                        "Unknown Wi-Fi"
                                    } else {
                                        connectionInfo.ssid.replace("\"", "")
                                    }
                                } else {
                                    wifiName = "Location Service Disabled"
                                }
                            } else {
                                wifiName = "Permission Denied"
                            }
                        } else {
                            wifiName = "Not Connected"
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
                        // Check if the device is connected to a Wi-Fi network
                        if (wifiManager.connectionInfo.networkId != -1) {
                            // Check for location permissions
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                // Check if location service is enabled
                                if (isLocationServiceEnabled(context)) {
                                    val connectionInfo = wifiManager.connectionInfo
                                    // Handle <unknown ssid>
                                    wifiName = if (connectionInfo.ssid == "<unknown ssid>") {
                                        "Unknown Wi-Fi"
                                    } else {
                                        connectionInfo.ssid.replace("\"", "")
                                    }
                                } else {
                                    wifiName = "Location Service Disabled"
                                }
                            } else {
                                wifiName = "Permission Denied"
                            }
                        } else {
                            wifiName = "Not Connected"
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
            delay(5000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            ipAddresses = getIpAddresses(context)
            delay(5000) // Update IP addresses every 5 seconds
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            // Simulate updating profile data
            profileDataList.forEach { profile ->
                // In a real app, you would replace this with actual traffic monitoring logic
                // that tracks traffic for each profile's IP and port.
                profile.bytesSent = (0..1000).random().toLong()
                profile.bytesReceived = (0..1000).random().toLong()
            }
            delay(2000) // Update every 2 seconds
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NetworkWifi,
                        contentDescription = "Wi-Fi Name",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Wi-Fi Name: $wifiName")
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CellTower,
                        contentDescription = "Mobile Data State",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Mobile Data State: $mobileDataState")
                }
                Divider()
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "IP Address",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Column {
                        Text(text = "IP Addresses:")
                        ipAddresses.forEach { (ip, connectionType) ->
                            Text(text = "$connectionType: $ip")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileDataTable(profileDataList)
            BytesGraph(
                bytesSentHistory,
                bytesReceivedHistory,
                currentBytesSent,
                currentBytesReceived
            )
        }
    }
}
@Composable
fun ProfileDataTable(profileDataList: List<ProfileData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Profile Name",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Bytes Sent",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(100.dp)
            )
            Text(
                "Bytes Received",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(120.dp)
            )
        }
        Divider()
        // Table Rows
        LazyColumn {
            items(profileDataList) { profile ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(profile.name, modifier = Modifier.weight(1f))
                    Text(
                        formatBytes(profile.bytesSent),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(100.dp)
                    )
                    Text(
                        formatBytes(profile.bytesReceived),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(120.dp)
                    )
                }
                Divider()
            }
        }
    }
}

@Composable
fun BytesGraph(
    bytesSentHistory: List<Long>,
    bytesReceivedHistory: List<Long>,
    currentBytesSent: Long,
    currentBytesReceived: Long
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Overall Bandwidth", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(16.dp)
                .border(1.dp, Color.Gray)
        ) {
            val width = size.width
            val height = size.height

            // Find the maximum value for scaling
            val maxBytes = maxOf(
                bytesSentHistory.maxOrNull() ?: 0,
                bytesReceivedHistory.maxOrNull() ?: 0
            )

            // Draw the graph if there is data
            if (maxBytes > 0) {
                val pathSent = Path()
                val pathReceived = Path()

                for (i in bytesSentHistory.indices) {
                    val x = i * width / (bytesSentHistory.size - 1).coerceAtLeast(1)
                    val ySent = height - (bytesSentHistory[i] * height / maxBytes)
                    val yReceived = height - (bytesReceivedHistory[i] * height / maxBytes)

                    if (i == 0) {
                        pathSent.moveTo(x, ySent)
                        pathReceived.moveTo(x, yReceived)
                    } else {
                        pathSent.lineTo(x, ySent)
                        pathReceived.lineTo(x, yReceived)
                    }
                }

                drawPath(
                    path = pathSent,
                    color = Color.Blue,
                    style = Stroke(width = 2.dp.toPx())
                )

                drawPath(
                    path = pathReceived,
                    color = Color.Red,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // Draw X-axis
            drawLine(
                color = Color.Green,
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2.dp.toPx()
            )
            // Draw X-axis label
            drawContext.canvas.nativeCanvas.drawText(
                "Time",
                width / 2,
                height + 40,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GREEN
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
        // Legend
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color.Blue)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bytes Sent: ${formatBytes(currentBytesSent)}")
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color.Red)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bytes Received: ${formatBytes(currentBytesReceived)}")
        }
    }
}
fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1073741824 -> String.format("%.2f GB", bytes.toFloat() / 1073741824)
        bytes >= 1048576 -> String.format("%.2f MB", bytes.toFloat() / 1048576)
        bytes >= 1024 -> String.format("%.2f KB", bytes.toFloat() / 1024)
        else -> "$bytes B"
    }
}

fun getIpAddresses(context: Context): Map<String, String> {
    val ipAddresses = mutableMapOf<String, String>()
    try {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()
        while (networkInterfaces.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            val inetAddresses = networkInterface.inetAddresses
            while (inetAddresses.hasMoreElements()) {
                val inetAddress = inetAddresses.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    val ip = inetAddress.hostAddress ?: "N/A"
                    val connectionType = when {
                        networkInterface.name.contains("wlan") -> "wifi"
                        networkInterface.name.contains("rmnet") -> "mobile"
                        networkInterface.name.contains("tun") -> "vpn connection"
                        else -> "unknown"
                    }
                    ipAddresses[ip] = connectionType
                }
            }
        }
    } catch (e: SocketException) {
        e.printStackTrace()
    }
    return ipAddresses
}

fun isLocationServiceEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}