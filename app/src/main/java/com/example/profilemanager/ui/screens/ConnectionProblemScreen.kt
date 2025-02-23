package com.example.profilemanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.profilemanager.R
import android.net.Uri

@Composable
fun ConnectionProblemScreen(
    navController: NavController,
    profileName: String,
    httpsUrl: String,
    httpUrl: String,
    isDarkMode: Boolean // Add this parameter
) {
    // Use the passed isDarkMode value
    val imageResource = if (isDarkMode) {
        R.drawable.funny_404_dark
    } else {
        R.drawable.funny_404_light
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageResource),
            contentDescription = "Funny 404",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 16.dp)
        )
        Text(text = "Connection Problem")
        Text(text = "Could not load the web page.")
        Button(onClick = {
            val encodedHttpsUrl = Uri.encode(httpsUrl)
            val encodedProfileName = Uri.encode(profileName)
            navController.navigate("profileDetail/$encodedHttpsUrl/$encodedProfileName")
        }) {
            Text(text = "Retry")
        }
        Button(onClick = { navController.navigate("main") }) {
            Text(text = "Back to Main")
        }
    }
}