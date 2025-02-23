package com.example.profilemanager.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "profiles")

class DataStoreManager(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("app_prefs")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val PROFILE_KEY = stringPreferencesKey("profile_key")
        private val PROFILE_ID_COUNTER = intPreferencesKey("profile_id_counter")
        fun profileIdKey(id: Int) = intPreferencesKey("profile_id_$id")
        fun profileNameKey(id: Int) = stringPreferencesKey("profile_name_$id")
        fun profileIpAddressKey(id: Int) = stringPreferencesKey("profile_ip_address_$id")
        fun profilePortKey(id: Int) = stringPreferencesKey("profile_port_$id")
        fun profileConnectionStateKey(id: Int) = stringPreferencesKey("profile_connection_state_$id")
        fun profileLastCheckedKey(id: Int) = stringPreferencesKey("profile_last_checked_$id")
        fun profileOrderKey(id: Int) = intPreferencesKey("profile_order_$id")
        fun profileIconNameKey(id: Int) = stringPreferencesKey("profile_icon_name_$id")
        // New keys for daily data
        fun dailyBytesSentKey(date: LocalDate) = longPreferencesKey("daily_bytes_sent_${date.format(DateTimeFormatter.ISO_DATE)}")
        fun dailyBytesReceivedKey(date: LocalDate) = longPreferencesKey("daily_bytes_received_${date.format(DateTimeFormatter.ISO_DATE)}")
    }
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }

    private val _connectionStateFlow = MutableStateFlow<List<Profile>>(emptyList())
    val connectionStateFlow = _connectionStateFlow.asStateFlow()
    suspend fun refreshProfiles() {
        val profiles = getAllProfilesFromDataStore().toMutableList()
        val updatedProfiles = mutableListOf<Profile>()
        for (profile in profiles) {
            val isConnected = isConnected(profile.ipAddress)
            val updatedConnectionState = if (isConnected) ConnectionState.CONNECTED else ConnectionState.NOT_CONNECTED
            val lastChecked = getCurrentTimestamp()
            updatedProfiles.add(
                profile.copy(
                    connectionState = updatedConnectionState,
                    lastChecked = lastChecked
                )
            )
            updateProfileConnectionState(profile.copy(connectionState = updatedConnectionState, lastChecked = lastChecked))
        }
        _connectionStateFlow.update { updatedProfiles }
    }

    suspend fun getAllProfilesFromDataStore(): List<Profile> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e("DataStoreManager", "Error reading preferences: ${exception.message}")
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val profiles = mutableListOf<Profile>()
                val profileIdCounter = preferences[PROFILE_ID_COUNTER] ?: 0
                for (id in 1..profileIdCounter) {
                    val name = preferences[profileNameKey(id)] ?: continue
                    val ipAddress = preferences[profileIpAddressKey(id)] ?: ""
                    val port = preferences[profilePortKey(id)] ?: ""
                    val connectionStateString = preferences[profileConnectionStateKey(id)] ?: ConnectionState.NOT_CONNECTED.name
                    val connectionState = ConnectionState.valueOf(connectionStateString)
                    val lastChecked = preferences[profileLastCheckedKey(id)] ?: ""
                    val order = preferences[profileOrderKey(id)] ?: 0
                    val iconName = preferences[profileIconNameKey(id)] ?: "Default"
                    profiles.add(Profile(id, name, ipAddress, port, connectionState, lastChecked, order))
                }
                profiles.sortedBy { it.order }
            }.firstOrNull() ?: emptyList()
    }
    fun getAllProfiles(): Flow<List<Profile>> = flow {
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e("DataStoreManager", "Error reading preferences: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .collect { preferences ->
                val profiles = mutableListOf<Profile>()
                val profileIdCounter = preferences[PROFILE_ID_COUNTER] ?: 0
                for (id in 1..profileIdCounter) {
                    val name = preferences[profileNameKey(id)] ?: continue
                    val ipAddress = preferences[profileIpAddressKey(id)] ?: ""
                    val port = preferences[profilePortKey(id)] ?: ""
                    val connectionStateString = preferences[profileConnectionStateKey(id)] ?: ConnectionState.NOT_CONNECTED.name
                    val connectionState = ConnectionState.valueOf(connectionStateString)
                    val lastChecked = preferences[profileLastCheckedKey(id)] ?: ""
                    val order = preferences[profileOrderKey(id)] ?: 0
                    val iconName = preferences[profileIconNameKey(id)] ?: "Default"
                    profiles.add(Profile(id, name, ipAddress, port, connectionState, lastChecked, order))
                }
                emit(profiles.sortedBy { it.order })
            }
    }
    suspend fun addProfile(profile: Profile) {
        context.dataStore.edit { preferences ->
            val currentId = preferences[PROFILE_ID_COUNTER] ?: 0
            val nextId = currentId + 1
            val currentMaxOrder = getAllProfilesFromDataStore().maxOfOrNull { it.order } ?: 0
            val nextOrder = currentMaxOrder + 1

            preferences[PROFILE_ID_COUNTER] = nextId
            preferences[profileNameKey(nextId)] = profile.name
            preferences[profileIpAddressKey(nextId)] = profile.ipAddress
            preferences[profilePortKey(nextId)] = profile.port
            preferences[profileConnectionStateKey(nextId)] = profile.connectionState.name
            preferences[profileLastCheckedKey(nextId)] = profile.lastChecked
            preferences[profileOrderKey(nextId)] = nextOrder
        }
    }
    suspend fun pingProfile(profile: Profile) {
        val isConnected = isConnected(profile.ipAddress)
        val updatedConnectionState = if (isConnected) ConnectionState.CONNECTED else ConnectionState.NOT_CONNECTED
        val lastChecked = getCurrentTimestamp()
        val updatedProfile = profile.copy(
            connectionState = updatedConnectionState,
            lastChecked = lastChecked
        )
        updateProfileConnectionState(updatedProfile)
    }

    suspend fun editProfile(profile: Profile) {
        context.dataStore.edit { preferences ->
            preferences[profileNameKey(profile.id)] = profile.name
            preferences[profileIpAddressKey(profile.id)] = profile.ipAddress
            preferences[profilePortKey(profile.id)] = profile.port
        }
    }

    suspend fun deleteProfile(profile: Profile) {
        context.dataStore.edit { preferences ->
            preferences.remove(profileIdKey(profile.id))
            preferences.remove(profileNameKey(profile.id))
            preferences.remove(profileIpAddressKey(profile.id))
            preferences.remove(profilePortKey(profile.id))
            preferences.remove(profileConnectionStateKey(profile.id))
            preferences.remove(profileLastCheckedKey(profile.id))
            preferences.remove(profileOrderKey(profile.id))
            preferences.remove(profileIconNameKey(profile.id))
        }
        reorderProfilesAfterDeletion()
    }
    private suspend fun reorderProfilesAfterDeletion() {
        val profiles = getAllProfilesFromDataStore()
        profiles.forEachIndexed { index, profile ->
            updateProfileOrder(profile.copy(order = index))
        }
    }

    private suspend fun updateProfileConnectionState(profile: Profile) {
        context.dataStore.edit { preferences ->
            preferences[profileConnectionStateKey(profile.id)] = profile.connectionState.name
            preferences[profileLastCheckedKey(profile.id)] = profile.lastChecked
        }
    }

    suspend fun updateProfileOrder(profile: Profile) {
        context.dataStore.edit { preferences ->
            preferences[profileOrderKey(profile.id)] = profile.order
        }
    }
    // New functions for daily data
    suspend fun saveDailyData(date: LocalDate, bytesSent: Long, bytesReceived: Long) {
        context.dataStore.edit { preferences ->
            preferences[dailyBytesSentKey(date)] = bytesSent
            preferences[dailyBytesReceivedKey(date)] = bytesReceived
        }
    }

    suspend fun getDailyData(date: LocalDate): Pair<Long, Long> {
        return context.dataStore.data
            .map { preferences ->
                val sent = preferences[dailyBytesSentKey(date)] ?: 0L
                val received = preferences[dailyBytesReceivedKey(date)] ?: 0L
                Pair(sent, received)
            }
            .firstOrNull() ?: Pair(0L, 0L)
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = Date()
        return dateFormat.format(currentTime)
    }

    private suspend fun isConnected(ipAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inetAddress = InetAddress.getByName(ipAddress)
                inetAddress.isReachable(5000)
            } catch (e: IOException) {
                Log.e("MainScreen", "Error checking connection: ${e.message}")
                false
            }
        }
    }
}