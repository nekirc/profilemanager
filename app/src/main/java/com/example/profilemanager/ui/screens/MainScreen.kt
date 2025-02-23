package com.example.profilemanager.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Web
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
import com.example.profilemanager.data.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val systemUiController = rememberSystemUiController()
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
    var showProfileOptionsDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<Profile?>(null) }

    val isDarkTheme by dataStoreManager.isDarkMode.collectAsState(initial = false)
    LaunchedEffect(systemUiController, isDarkTheme) {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = !isDarkTheme
        )
    }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            showRefreshingDialog = true
            profiles.forEach { profile ->
                profileToRefresh = profile
                dataStoreManager.pingProfile(profile)
                delay(500)
            }
            refreshing = false
            showRefreshingDialog = false
            profileToRefresh = null
        }
    }
    LaunchedEffect(bulkRefreshing) {
        if (bulkRefreshing) {
            showRefreshingDialog = true
            profiles.forEach { profile ->
                profileToRefresh = profile
                dataStoreManager.pingProfile(profile)
                delay(500)
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
                        "Main"
                    )
                },
                actions = {
                    Text(
                        text = "Refresh All",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable {
                                bulkRefreshing = true
                            },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                            }, onProfileClicked = {
                                showProfileOptionsDialog = true
                                selectedProfile = it
                            })
                        }
                    }
                }
            }
        }
    }
    if (showRefreshingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Refreshing profiles...")
                    }
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
            }
        )
    }
    if (showPingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Pinging profile...")
                    }
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
            }
        )
    }
    if (showProfileOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showProfileOptionsDialog = false },
            icon = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Web,
                        contentDescription = "Webpage",
                        modifier = Modifier.size(48.dp)
                    )
                }
            },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Open profile in...", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Choose how you want to open the profile")
                }
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        showProfileOptionsDialog = false
                        if (selectedProfile != null) {
                            val httpsUrl = if (selectedProfile!!.port.isNotEmpty()) {
                                "https://${selectedProfile!!.ipAddress}:${selectedProfile!!.port}"
                            } else {
                                "https://${selectedProfile!!.ipAddress}"
                            }
                            val encodedHttpsUrl = Uri.encode(httpsUrl)
                            val encodedProfileName = Uri.encode(selectedProfile!!.name)
                            navController.navigate("profileDetail/$encodedHttpsUrl/$encodedProfileName")
                        }
                    }) {
                        Text("Internal WebView")
                    }
                }
            },
            dismissButton = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        showProfileOptionsDialog = false
                        if (selectedProfile != null) {
                            val httpsUrl = if (selectedProfile!!.port.isNotEmpty()) {
                                "https://${selectedProfile!!.ipAddress}:${selectedProfile!!.port}"
                            } else {
                                "https://${selectedProfile!!.ipAddress}"
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(httpsUrl))
                            context.startActivity(intent)
                        }
                    }) {
                        Text("External Browser")
                    }
                }
            }
        )
    }
}
@Composable
fun ProfileItem(
    profile: Profile,
    navController: NavController,
    onPingClicked: () -> Unit,
    onProfileClicked: (Profile) -> Unit
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.2f), // Start with a light gray
            Color.Transparent // End with transparent
        )
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush), // Apply the gradient here
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Set elevation to 0
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // Make the card background transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    onProfileClicked(profile)
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val connectionIcon = when (profile.connectionState) {
                ConnectionState.CONNECTED -> Icons.Filled.CheckCircle
                ConnectionState.NOT_CONNECTED -> Icons.Filled.Error
                else -> Icons.Filled.Error
            }
            val connectionColor = when (profile.connectionState) {
                ConnectionState.CONNECTED -> Color.Green
                ConnectionState.NOT_CONNECTED -> Color.Red
                else -> Color.Gray
            }
            Icon(
                imageVector = connectionIcon,
                contentDescription = if (profile.connectionState == ConnectionState.CONNECTED) "Connected" else "Not Connected",
                tint = connectionColor,
                modifier = Modifier.size(24.dp)
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