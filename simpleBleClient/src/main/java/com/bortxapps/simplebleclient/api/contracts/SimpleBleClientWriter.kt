package com.bortxapps.simplebleclient.api.contracts

import com.bortxapps.simplebleclient.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import java.util.UUID

public interface SimpleBleClientWriter {
    /**
     * Sends data to a specified BLE service and characteristic.
     *
     * This is a suspend function designed for use with Kotlin coroutines,
     * enabling asynchronous data transmission to a BLE device.
     *
     * @param serviceUUID The UUID of the BLE service for the data transmission.
     * @param characteristicUUID The UUID of the BLE characteristic where data is written.
     * @param data The byte array data to send to the BLE device.
     * @param complexResponse Indicates if the response from the BLE device
     *                        is expected to be complex. Default is `false`.
     *
     * @return [BleNetworkMessage] representing the result of the write operation.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     *
     *
     * Usage example:
     *  val simpleBleClient = SimpleBleClientBuilder..build(context)
     *
     *  viewModelScope.launch {
     *      withContext(IO) {
     *          val result = simpleBleClient.sendData(yourServiceUUID, yourCharacteristicUUID, yourData)
     *     }
     *  }
     */
    public suspend fun sendData(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        data: ByteArray,
        complexResponse: Boolean = false
    ): BleNetworkMessage
}
