package com.example.profilemanager.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.profilemanager.data.ConnectionState
import com.example.profilemanager.data.DataStoreManager
import com.example.profilemanager.data.Profile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(navController: NavController) {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(context)
    val coroutineScope = rememberCoroutineScope()
    val profiles: List<Profile> by dataStoreManager.getAllProfiles()
        .collectAsState(initial = emptyList())

    var isEditMode by remember { mutableStateOf(false) }
    var allExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
                actions = {
                    TextButton(onClick = { allExpanded = !allExpanded }) {
                        Text(
                            text = if (allExpanded) "Collapse All" else "Expand All",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (isEditMode) "Done" else "Edit"
                    )
                }
                FloatingActionButton(
                    onClick = { navController.navigate("addProfile") }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Profile")
                }
            }
        }
    ) { innerPadding ->
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
                        ProfileItem(
                            profile = profile,
                            isEditMode = isEditMode,
                            navController = navController,
                            allExpanded = allExpanded,
                            onDelete = {
                                coroutineScope.launch {
                                    dataStoreManager.deleteProfile(profile)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ProfileItem(
    profile: Profile,
    isEditMode: Boolean,
    navController: NavController,
    allExpanded: Boolean,
    onDelete: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var isExpanded by remember(profile.id, allExpanded) {
        mutableStateOf(false)
    }
    val isExpandedState by remember(allExpanded, isExpanded) {
        derivedStateOf {
            allExpanded || isExpanded
        }
    }
    val rotationState by animateFloatAsState(targetValue = if (isExpandedState) 180f else 0f, label = "rotationState")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (!allExpanded) {
                    isExpanded = !isExpanded
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Connection status indicator
                    val connectionColor = when (profile.connectionState) {
                        ConnectionState.CONNECTED -> Color.Green
                        ConnectionState.NOT_CONNECTED -> Color.Red
                        else -> Color.Gray
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(connectionColor)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditMode) {
                        IconButton(onClick = { navController.navigate("editProfile/${profile.id}") }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        IconButton(onClick = { showDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = {
                        isExpanded = !isExpanded
                    }) {
                        Icon(
                            imageVector = if (isExpandedState) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand",
                            modifier = Modifier
                                .rotate(rotationState)
                                .size(30.dp)
                        )
                    }
                }
            }
            if (isExpandedState) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(text = "IP: ${profile.ipAddress}")
                    if (profile.port.isNotEmpty()) {
                        Text(text = "Port: ${profile.port}")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete this profile?") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}