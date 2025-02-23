package com.example.profilemanager.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.SignalCellularOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.profilemanager.data.DataStoreManager
import com.example.profilemanager.data.NetworkConnectionState
import com.example.profilemanager.data.NetworkConnectivityManager
import com.example.profilemanager.ui.screens.AddProfileScreen
import com.example.profilemanager.ui.screens.ConnectionProblemScreen
import com.example.profilemanager.ui.screens.EditProfileScreen
import com.example.profilemanager.ui.screens.MainScreen
import com.example.profilemanager.ui.screens.NetworkScreen
import com.example.profilemanager.ui.screens.ProfileDetailScreen
import com.example.profilemanager.ui.screens.ProfileListScreen
import com.example.profilemanager.ui.screens.SettingsScreen
import kotlinx.coroutines.delay
import com.example.profilemanager.R
import com.example.profilemanager.ui.screens.getIpAddresses

enum class ConnectionState {
    CONNECTED, DISCONNECTED, UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation(
    dataStoreManager: DataStoreManager,
    networkConnectivityManager: NetworkConnectivityManager
) {
    val navController = rememberNavController()
    val networkConnectionState by networkConnectivityManager.networkConnectionState.collectAsState(
        initial = NetworkConnectionState.UNKNOWN
    )
    val isDarkMode by dataStoreManager.isDarkMode.collectAsState(initial = false)
    val context = LocalContext.current
    var wifiState by remember { mutableStateOf(ConnectionState.UNKNOWN) }
    var mobileDataState by remember { mutableStateOf(ConnectionState.UNKNOWN) }
    var vpnState by remember { mutableStateOf(ConnectionState.UNKNOWN) }
    suspend fun checkNetworkStatus(connectivityManager: ConnectivityManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network: Network? = connectivityManager.activeNetwork
            val networkCapabilities: NetworkCapabilities? =
                connectivityManager.getNetworkCapabilities(network)

            if (networkCapabilities != null) {
                wifiState =
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        ConnectionState.CONNECTED
                    } else {
                        ConnectionState.DISCONNECTED
                    }
                mobileDataState =
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        ConnectionState.CONNECTED
                    } else {
                        ConnectionState.DISCONNECTED
                    }
            } else {
                wifiState = ConnectionState.DISCONNECTED
                mobileDataState = ConnectionState.DISCONNECTED
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                wifiState = if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    ConnectionState.CONNECTED
                } else {
                    ConnectionState.DISCONNECTED
                }
                mobileDataState = if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                    ConnectionState.CONNECTED
                } else {
                    ConnectionState.DISCONNECTED
                }
            } else {
                wifiState = ConnectionState.DISCONNECTED
                mobileDataState = ConnectionState.DISCONNECTED
            }
        }
    }
    LaunchedEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        while (true) {
            checkNetworkStatus(connectivityManager)
            val ipAddresses = getIpAddresses(context)
            vpnState = if (ipAddresses.values.contains("vpn connection")) {
                ConnectionState.CONNECTED
            } else {
                ConnectionState.DISCONNECTED
            }
            delay(5000) // Delay for 5 seconds
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_app),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                            Text("Profile Manager")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NetworkStatusIcon(state = wifiState, type = "Wi-Fi")
                                Spacer(modifier = Modifier.size(8.dp))
                                NetworkStatusIcon(state = mobileDataState, type = "Mobile Data")
                                Spacer(modifier = Modifier.size(8.dp))
                                NetworkStatusIcon(state = vpnState, type = "VPN")
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("main") { MainScreen(navController) }
            composable("profiles") { ProfileListScreen(navController) }
            composable("network") { NetworkScreen(navController) }
            composable("addProfile") { AddProfileScreen(navController) }
            composable("settings") { SettingsScreen(navController, dataStoreManager) }
            composable(
                "editProfile/{profileId}",
                arguments = listOf(navArgument("profileId") { type = NavType.IntType })
            ) { backStackEntry ->
                val profileId = backStackEntry.arguments?.getInt("profileId") ?: 0
                EditProfileScreen(navController, profileId)
            }
            composable(
                "profileDetail/{url}/{name}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val name = backStackEntry.arguments?.getString("name") ?: ""
                ProfileDetailScreen(navController, url, name)
            }
            composable(
                "connectionProblem/{httpsUrl}/{httpUrl}/{profileName}/{isDarkMode}",
                arguments = listOf(
                    navArgument("httpsUrl") { type = NavType.StringType },
                    navArgument("httpUrl") { type = NavType.StringType },
                    navArgument("profileName") { type = NavType.StringType },
                    navArgument("isDarkMode") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val httpsUrl = backStackEntry.arguments?.getString("httpsUrl") ?: ""
                val httpUrl = backStackEntry.arguments?.getString("httpUrl") ?: ""
                val profileName = backStackEntry.arguments?.getString("profileName") ?: ""
                val isDarkMode = backStackEntry.arguments?.getBoolean("isDarkMode") ?: false
                ConnectionProblemScreen(navController, profileName, httpsUrl, httpUrl, isDarkMode)
            }
        }
    }
}

@Composable
fun NetworkStatusIcon(state: ConnectionState, type: String) {
    val icon: ImageVector
    val contentDescription: String
    val iconColor: Color

    when (state) {
        ConnectionState.CONNECTED -> {
            iconColor = Color.Green
            when (type) {
                "Wi-Fi" -> {
                    icon = Icons.Filled.Wifi
                    contentDescription = "Wi-Fi Connected"
                }

                "Mobile Data" -> {
                    icon = Icons.Filled.CellTower
                    contentDescription = "Mobile Data Connected"
                }

                "VPN" -> {
                    icon = Icons.Filled.Lock
                    contentDescription = "VPN Connected"
                }

                else -> {
                    icon = Icons.Outlined.Error
                    contentDescription = "Unknown Connection"
                }
            }
        }

        ConnectionState.DISCONNECTED -> {
            iconColor = Color.Red
            when (type) {
                "Wi-Fi" -> {
                    icon = Icons.Outlined.WifiOff
                    contentDescription = "Wi-Fi Disconnected"
                }

                "Mobile Data" -> {
                    icon = Icons.Outlined.SignalCellularOff
                    contentDescription = "Mobile Data Disconnected"
                }

                "VPN" -> {
                    icon = Icons.Outlined.LockOpen
                    contentDescription = "VPN Disconnected"
                }

                else -> {
                    icon = Icons.Outlined.Error
                    contentDescription = "Unknown Connection"
                }
            }
        }

        else -> {
            iconColor = Color.Gray
            icon = Icons.Outlined.Error
            contentDescription = "Unknown Connection"
        }
    }
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = iconColor,
        modifier = Modifier.size(16.dp) // Changed size to 16.dp
    )
}
