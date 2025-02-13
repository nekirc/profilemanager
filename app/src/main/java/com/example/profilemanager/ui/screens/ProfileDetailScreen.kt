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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.http.SslError
import android.webkit.SslErrorHandler
import com.example.profilemanager.data.DataStoreManager

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(navController: NavController, url: String, profileName: String) {
    val dataStoreManager = DataStoreManager(LocalContext.current)
    val isDarkMode by dataStoreManager.isDarkMode.collectAsState(initial = false)
    val decodedUrl = Uri.decode(url)
    val httpsUrl = decodedUrl
    val httpUrl = decodedUrl.replace("https", "http")
    val isPageLoaded = remember { mutableStateOf(false) }
    val isError = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(true) }
    val webView = remember { mutableStateOf<WebView?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show loading snackbar when isLoading changes to true
    LaunchedEffect(isLoading.value) {
        if (isLoading.value) {
            scope.launch {
                snackbarHostState.showSnackbar("Loading...")
            }
        }
    }

    // Check if page is loaded correctly after 3 seconds
    LaunchedEffect(key1 = true) {
        delay(3000)
        if (!isPageLoaded.value) {
            isError.value = true
            val encodedHttpsUrl = Uri.encode(httpsUrl)
            val encodedHttpUrl = Uri.encode(httpUrl)
            val encodedProfileName = Uri.encode(profileName)
            navController.navigate("connectionProblem/$encodedHttpsUrl/$encodedHttpUrl/$encodedProfileName/$isDarkMode") {
                popUpTo("profileDetail/$encodedHttpsUrl/$encodedProfileName") {
                    inclusive = true
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(profileName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isPageLoaded.value = false
                        isError.value = false
                        isLoading.value = true
                        webView.value?.loadUrl(decodedUrl)
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                Log.e("ProfileDetailScreen", "SSL error: ${error?.toString()}")
                                handler?.proceed() // Ignore SSL certificate errors
                            }
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                Log.e("ProfileDetailScreen", "WebView error: ${error?.description}")
                                if (error?.errorCode == WebViewClient.ERROR_CONNECT ||
                                    error?.errorCode == WebViewClient.ERROR_TIMEOUT ||
                                    error?.errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                                    error?.description?.contains("net::ERR_NAME_NOT_RESOLVED") == true
                                ) {
                                    if (!isError.value) {
                                        isError.value = true
                                        val encodedHttpsUrl = Uri.encode(httpsUrl)
                                        val encodedHttpUrl = Uri.encode(httpUrl)
                                        val encodedProfileName = Uri.encode(profileName)
                                        navController.navigate("connectionProblem/$encodedHttpsUrl/$encodedHttpUrl/$encodedProfileName/$isDarkMode") {
                                            popUpTo("profileDetail/$encodedHttpsUrl/$encodedProfileName") {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                                isLoading.value = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPageLoaded.value = true
                                isLoading.value = false
                            }
                        }
                        settings.javaScriptEnabled = true
                        setBackgroundColor(Color.Transparent.hashCode())
                        loadUrl(decodedUrl)
                        webView.value = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}