package com.bortxapps.simplebleclient.manager.contracts

import com.bortxapps.simplebleclient.data.BleNetworkMessage
import java.util.UUID

public interface SimpleBleClientWriter {
    public suspend fun sendData(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        data: ByteArray,
        complexResponse: Boolean = false
    ): BleNetworkMessage
}
