package com.example.profilemanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.profilemanager.data.ConnectionState
import com.example.profilemanager.data.DataStoreManager
import com.example.profilemanager.data.Profile
import com.example.profilemanager.data.isValidIpAddressOrDns
import com.example.profilemanager.data.isValidPort
import com.example.profilemanager.data.isValidProfileName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(context)
    val coroutineScope = rememberCoroutineScope()

    var profileName by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showSavingDialog by remember { mutableStateOf(false) }

    var isProfileNameValid by remember { mutableStateOf(false) }
    var isIpAddressValid by remember { mutableStateOf(false) } // Changed to false initially
    var isPortValid by remember { mutableStateOf(true) }
    var profileNameError by remember { mutableStateOf<String?>(null) }
    var ipAddressError by remember { mutableStateOf<String?>(null) }
    var portError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = true) {
        if (profileName.isNotBlank() || ipAddress.isNotBlank() || port.isNotBlank()) {
            showDialog = true
        } else {
            navController.navigate("profiles")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (profileName.isNotBlank() || ipAddress.isNotBlank() || port.isNotBlank()) {
                            showDialog = true
                        } else {
                            navController.navigate("profiles")
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = profileName,
                onValueChange = {
                    profileName = it
                    isProfileNameValid = isValidProfileName(it)
                    profileNameError =
                        if (!isProfileNameValid) "Profile name must be less than 30 characters and not empty" else null
                },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isProfileNameValid,
                supportingText = {
                    if (profileNameError != null) {
                        Text(text = profileNameError!!)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
            OutlinedTextField(
                value = ipAddress,
                onValueChange = {
                    ipAddress = it
                    isIpAddressValid = isValidIpAddressOrDns(it) // Use the correct function
                    ipAddressError =
                        if (!isIpAddressValid) "DNS Name or IP Address is not valid" else null // Updated error message
                },
                label = { Text("DNS Name or IP Address") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isIpAddressValid,
                supportingText = {
                    if (ipAddressError != null) {
                        Text(text = ipAddressError!!)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
            OutlinedTextField(
                value = port,
                onValueChange = {
                    port = it
                    isPortValid = port.isEmpty() || isValidPort(it)
                    portError = if (!isPortValid) "Port must be a number between 1 and 65535" else null
                },
                label = { Text("Port (optional)") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isPortValid,
                supportingText = {
                    if (portError != null) {
                        Text(text = portError!!)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            Button(
                onClick = {
                    if (isProfileNameValid && isIpAddressValid) {
                        showSavingDialog = true
                        coroutineScope.launch {
                            val newProfile = Profile(
                                0,
                                profileName,
                                ipAddress,
                                port,
                                ConnectionState.UNKNOWN,
                                "",
                                0
                            )
                            dataStoreManager.addProfile(newProfile)
                            showSavingDialog = false
                            navController.navigate("profiles") {
                                popUpTo("profiles") { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 64.dp),
                enabled = isProfileNameValid && isIpAddressValid
            ) {
                Text("Save")
            }
        }
        if (showSavingDialog) {
            AlertDialog(
                onDismissRequest = { /* Prevent dismissing by tapping outside */ },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Saving profile...")
                    }
                },
                text = {
                    Text("Please wait while we save the profile.")
                },
                confirmButton = {
                    // No button needed here
                }
            )
        }
if (showDialog) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("Discard Changes?") },
        text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
        confirmButton = {
            Button(onClick = {
                showDialog = false
                navController.navigate("profiles")
            }) {
                Text("Discard")
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
}