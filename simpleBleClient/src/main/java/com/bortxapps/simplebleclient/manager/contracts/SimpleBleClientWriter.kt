package com.bortxapps.simplebleclient.manager.contracts

import com.bortxapps.simplebleclient.data.BleNetworkMessage
import java.util.UUID

interface SimpleBleClientWriter {
    suspend fun sendData(serviceUUID: UUID, characteristicUUID: UUID, data: ByteArray, complexResponse: Boolean = false): BleNetworkMessage
}