package com.bortxapps.simplebleclient.manager.contracts

import com.bortxapps.simplebleclient.data.BleNetworkMessage
import java.util.UUID

interface SimpleBleClientReader {
    suspend fun readData(serviceUUID: UUID, characteristicUUID: UUID, complexResponse: Boolean = false): BleNetworkMessage
}