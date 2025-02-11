package com.example.profilemanager.ui.screens

data class NetworkDevice(
    val adapterName: String,
    val ipAddress: String,
    val macAddress: String,
    val ssid: String,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0
)