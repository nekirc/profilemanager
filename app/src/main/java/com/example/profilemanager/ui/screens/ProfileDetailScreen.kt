package com.example.profilemanager.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(navController: NavController, url: String, profileName: String) {
    val decodedUrl = Uri.decode(url)
    val httpsUrl = decodedUrl
    val httpUrl = decodedUrl.replace("https", "http")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profileName) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("main") }) {
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
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                Log.e("ProfileDetailScreen", "WebView error: ${error?.description}")
                                // Navigate to ConnectionProblemScreen
                                val encodedHttpsUrl = Uri.encode(httpsUrl)
                                val encodedHttpUrl = Uri.encode(httpUrl)
                                val encodedProfileName = Uri.encode(profileName)
                                navController.navigate("connectionProblem/$encodedHttpsUrl/$encodedHttpUrl/$encodedProfileName")
                            }
                        }
                        settings.javaScriptEnabled = true
                        loadUrl(decodedUrl)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { webView ->
                    webView.loadUrl(decodedUrl)
                }
            )
        }
    }
}