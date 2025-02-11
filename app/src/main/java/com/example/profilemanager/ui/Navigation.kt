package com.example.profilemanager.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.profilemanager.data.DataStoreManager
import com.example.profilemanager.ui.screens.AddProfileScreen
import com.example.profilemanager.ui.screens.ConnectionProblemScreen
import com.example.profilemanager.ui.screens.EditProfileScreen
import com.example.profilemanager.ui.screens.MainScreen
import com.example.profilemanager.ui.screens.NetworkScreen
import com.example.profilemanager.ui.screens.ProfileDetailScreen
import com.example.profilemanager.ui.screens.ProfileListScreen
import com.example.profilemanager.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation(dataStoreManager: DataStoreManager) {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Manager") },
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
                "connectionProblem/{httpsUrl}/{httpUrl}/{profileName}",
                arguments = listOf(
                    navArgument("httpsUrl") { type = NavType.StringType },
                    navArgument("httpUrl") { type = NavType.StringType },
                    navArgument("profileName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val httpsUrl = backStackEntry.arguments?.getString("httpsUrl") ?: ""
                val httpUrl = backStackEntry.arguments?.getString("httpUrl") ?: ""
                val profileName = backStackEntry.arguments?.getString("profileName") ?: ""
                ConnectionProblemScreen(navController, profileName, httpsUrl, httpUrl)
            }
        }
    }
}