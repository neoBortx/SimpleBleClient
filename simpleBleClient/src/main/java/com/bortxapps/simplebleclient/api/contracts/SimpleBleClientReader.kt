package com.bortxapps.simplebleclient.api.contracts

import com.bortxapps.simplebleclient.api.data.BleCharacteristic
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import java.util.UUID

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

    /**
     * Reads all characteristics from the BLE device.
     *
     * This function is a suspend function, designed for use with Kotlin coroutines,
     * allowing for asynchronous reading of all characteristics from a BLE device.
     *
     * @return A list of [BleCharacteristic] objects representing all the characteristics of the BLE device.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     *
     * Usage example:
     *  val simpleBleClient = SimpleBleClientBuilder..build(context)
     *
     *  viewModelScope.launch {
     *      withContext(IO) {
     *          val result = simpleBleClient.getAllCharacteristics()
     *     }
     *  }
     */
    public suspend fun getAllCharacteristics(): List<BleCharacteristic>
}
