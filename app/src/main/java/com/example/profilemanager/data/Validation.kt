package com.example.profilemanager.data

import java.util.regex.Pattern

fun isValidProfileName(name: String): Boolean {
    return name.isNotBlank() && name.length <= 30
}

fun isValidPort(port: String): Boolean {
    return try {
        val portNumber = port.toInt()
        portNumber in 1..65535
    } catch (e: NumberFormatException) {
        false
    }
}
fun isValidIpAddressOrDns(input: String): Boolean {
    if (input.isBlank()) {
        return false
    }

    // Check if it's a valid IP address
    val ipPattern = Pattern.compile(
        "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\$"
    )
    if (ipPattern.matcher(input).matches()) {
        return true
    }

    // Check if it's a valid DNS name (hostname)
    val dnsPattern = Pattern.compile(
        "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])\$"
    )
    return dnsPattern.matcher(input).matches()
}