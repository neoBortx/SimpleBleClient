package com.bortxapps.simplebleclient.manager.contracts

import com.bortxapps.simplebleclient.data.BleNetworkMessage
import java.util.UUID

public interface SimpleBleClientReader {
    public suspend fun readData(serviceUUID: UUID, characteristicUUID: UUID, complexResponse: Boolean = false): BleNetworkMessage
}
