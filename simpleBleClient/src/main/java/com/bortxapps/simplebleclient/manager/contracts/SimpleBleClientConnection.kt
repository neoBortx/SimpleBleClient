package com.bortxapps.simplebleclient.manager.contracts

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

interface SimpleBleClientConnection {
    suspend fun connectToDevice(context: Context, address: String): Boolean
    suspend fun disconnect()
    fun subscribeToConnectionStatusChanges(): MutableStateFlow<Int>
    suspend fun subscribeToCharacteristicChanges(characteristicsUUid: List<UUID>): Boolean
}
