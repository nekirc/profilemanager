package com.example.profilemanager.data

import androidx.compose.ui.graphics.Color

enum class ConnectionState(val color: Color) {
    CONNECTED(Color.Green),
    NOT_CONNECTED(Color.Red),
    UNKNOWN(Color.Gray)
}