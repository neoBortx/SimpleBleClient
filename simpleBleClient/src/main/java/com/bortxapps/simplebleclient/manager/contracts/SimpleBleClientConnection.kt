package com.bortxapps.simplebleclient.manager.contracts

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

public interface SimpleBleClientConnection {
    public suspend fun connectToDevice(context: Context, address: String): Boolean
    public suspend fun disconnect()
    public fun subscribeToConnectionStatusChanges(): MutableStateFlow<Int>
    public suspend fun subscribeToCharacteristicChanges(characteristicsUUid: List<UUID>): Boolean
}
