package com.example.profilemanager.data


data class Profile(
    val id: Int,
    val name: String,
    val ipAddress: String,
    val port: String,
    val connectionState: ConnectionState,
    val lastChecked: String,
    val order: Int,
)