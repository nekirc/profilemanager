package com.example.profilemanager.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.profilemanager.data.ConnectionState
import com.example.profilemanager.data.DataStoreManager
import com.example.profilemanager.data.NetworkConnectionState
import com.example.profilemanager.data.NetworkConnectivityManager
import com.example.profilemanager.data.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(context)
    val coroutineScope = rememberCoroutineScope()
    val profiles: List<Profile> by dataStoreManager.getAllProfiles()
        .collectAsState(initial = emptyList())
    var refreshing by remember { mutableStateOf(false) }
    var showRefreshingDialog by remember { mutableStateOf(false) }
    var profileToRefresh by remember { mutableStateOf<Profile?>(null) }
    var bulkRefreshing by remember { mutableStateOf(false) }
    var showPingDialog by remember { mutableStateOf(false) }

    // Initialize NetworkConnectivityManager
    val networkConnectivityManager = remember { NetworkConnectivityManager(context) }
    // Collect the network connection state as a state
    val networkConnectionState by networkConnectivityManager.networkConnectionState.collectAsState(
        initial = NetworkConnectionState.UNKNOWN
    )

    LaunchedEffect(refreshing) {
        if (refreshing) {
            showRefreshingDialog = true
            // Refresh each profile individually and update profileToRefresh
            profiles.forEach { profile ->
                profileToRefresh = profile
                dataStoreManager.pingProfile(profile)
                delay(500) // Short delay between each profile refresh
            }
            refreshing = false
            showRefreshingDialog = false
            profileToRefresh = null
        }
    }
    LaunchedEffect(bulkRefreshing) {
        if (bulkRefreshing) {
            showRefreshingDialog = true
            // Refresh each profile individually and update profileToRefresh
            profiles.forEach { profile ->
                profileToRefresh = profile
                dataStoreManager.pingProfile(profile)
                delay(500) // Short delay between each profile refresh
            }
            bulkRefreshing = false
            showRefreshingDialog = false
            profileToRefresh = null
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Main",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Network Connection Status Indicator
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .align(Alignment.CenterVertically),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NetworkStatusIndicator(networkConnectionState)
                    }
                    Button(onClick = {
                        bulkRefreshing = true
                    }) {
                        Text("Refresh All")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // Use a Box to overlay the network indicator on top of the Column
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content of the screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                if (profiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No profiles configured",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(profiles) { profile ->
                            ProfileItem(profile = profile, navController = navController, onPingClicked = {
                                coroutineScope.launch {
                                    showPingDialog = true
                                    profileToRefresh = profile
                                    dataStoreManager.pingProfile(profile)
                                    delay(500)
                                    showPingDialog = false
                                    profileToRefresh = null
                                }
                            })
                        }
                    }
                }
            }
        }
    }
    if (showRefreshingDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by tapping outside */ },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Refreshing profiles...")
                }
            },
            text = {
                Column {
                    Text("Please wait while we refresh the profiles.")
                    if (profileToRefresh != null) {
                        Text("Refreshing: ${profileToRefresh!!.name}")
                    }
                }
            },
            confirmButton = {
                // No button needed here
            }
        )
    }
    if (showPingDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by tapping outside */ },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Pinging profile...")
                }
            },
            text = {
                Column {
                    Text("Please wait while we ping the profile.")
                    if (profileToRefresh != null) {
                        Text("Pinging: ${profileToRefresh!!.name}")
                    }
                }
            },
            confirmButton = {
                // No button needed here
            }
        )
    }
}
@Composable
fun ProfileItem(
    profile: Profile,
    navController: NavController,
    onPingClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // In MainScreen.kt, inside the ProfileItem composable, within the clickable block:
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    val httpsUrl = if (profile.port.isNotEmpty()) {
                        "https://${profile.ipAddress}:${profile.port}"
                    } else {
                        "https://${profile.ipAddress}"
                    }
                    val httpUrl = if (profile.port.isNotEmpty()) {
                        "http://${profile.ipAddress}:${profile.port}"
                    } else {
                        "http://${profile.ipAddress}"
                    }
                    val encodedHttpsUrl = Uri.encode(httpsUrl)
                    val encodedHttpUrl = Uri.encode(httpUrl)
                    val encodedProfileName = Uri.encode(profile.name)
                    navController.navigate("profileDetail/$encodedHttpsUrl/$encodedProfileName")
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection status indicator
            val connectionColor = when (profile.connectionState) {
                ConnectionState.CONNECTED -> Color.Green
                ConnectionState.NOT_CONNECTED -> Color.Red
                else -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(connectionColor)
                    .clip(RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = 12.sp, color = Color.Gray)) {
                    Text(text = "Last Checked: ${profile.lastChecked}")
                }
            }
            IconButton(onClick = onPingClicked) {
                Icon(Icons.Filled.Refresh, contentDescription = "Ping")
            }
        }
    }
}

@Composable
fun NetworkStatusIndicator(networkConnectionState: NetworkConnectionState) {
    val connectionText = when (networkConnectionState) {
        NetworkConnectionState.CONNECTED -> "Connected"
        NetworkConnectionState.DISCONNECTED -> "Disconnected"
        NetworkConnectionState.UNKNOWN -> "Unknown"
    }
    val icon = when (networkConnectionState) {
        NetworkConnectionState.CONNECTED -> Icons.Filled.CheckCircle
        NetworkConnectionState.DISCONNECTED -> Icons.Filled.Error
        NetworkConnectionState.UNKNOWN -> Icons.Filled.Error
    }
    val iconColor = when (networkConnectionState) {
        NetworkConnectionState.CONNECTED -> Color.Green
        NetworkConnectionState.DISCONNECTED -> Color.Red
        NetworkConnectionState.UNKNOWN -> Color.Gray
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = "Network Status",
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(text = connectionText, fontSize = 12.sp)
    }
}