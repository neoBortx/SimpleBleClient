package com.bortxapps.simplebleclient.api.contracts

import androidx.annotation.Keep
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import java.util.UUID

@Keep
public interface SimpleBleClientReader {

    /**
     * Reads data from a specified BLE service and characteristic.
     *
     * This function is a suspend function, designed for use with Kotlin coroutines,
     * allowing for asynchronous reading of data from a BLE device. It's used to retrieve
     * data from a specified characteristic of a BLE service.
     *
     * @param serviceUUID The UUID of the BLE service from which the data will be read.
     *                    This UUID should uniquely identify the service on the BLE device.
     * @param characteristicUUID The UUID of the BLE characteristic from which the data will be read.
     *                           Characteristics are specific data points on a BLE device that hold values.
     *
     * @return [BleNetworkMessage] representing the data read from the device. This object may contain
     *         the data read from the characteristic, along with any relevant status information.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     *
     * Usage example:
     *  val simpleBleClient = SimpleBleClientBuilder..build(context)
     *
     *  viewModelScope.launch {
     *      withContext(IO) {
     *          val result = simpleBleClient.readData(yourServiceUUID, yourCharacteristicUUID)
     *     }
     *  }
     */

    public suspend fun readData(serviceUUID: UUID, characteristicUUID: UUID): BleNetworkMessage
}
