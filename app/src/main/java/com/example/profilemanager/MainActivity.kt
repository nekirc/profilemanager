package com.example.profilemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.profilemanager.data.DataStoreManager
import com.example.profilemanager.data.NetworkConnectivityManager
import com.example.profilemanager.ui.Navigation
import com.example.profilemanager.ui.theme.ProfileManagerTheme

class MainActivity : ComponentActivity() {

    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataStoreManager = DataStoreManager(this)

        setContent {
            MainContent(dataStoreManager = dataStoreManager, context = this)
        }
    }
}

@Composable
fun MainContent(dataStoreManager: DataStoreManager, context: MainActivity) {
    val networkConnectivityManager = remember { NetworkConnectivityManager(context) }
    val isDarkMode by dataStoreManager.isDarkMode.collectAsState(initial = isSystemInDarkTheme())
    ProfileManagerTheme(darkTheme = isDarkMode) {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Navigation(dataStoreManager, networkConnectivityManager)
        }
    }
}