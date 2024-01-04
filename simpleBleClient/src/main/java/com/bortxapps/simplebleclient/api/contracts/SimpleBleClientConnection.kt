package com.bortxapps.simplebleclient.api.contracts

import android.content.Context
import com.bortxapps.simplebleclient.api.data.BleConnectionStatus
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import kotlinx.coroutines.flow.Flow

public interface SimpleBleClientConnection {

    /**
     * Attempts to establish a connection with a BLE device.
     *
     * @param context The context used for the connection operation.
     * @param address The MAC address of the BLE device to connect to.
     * @return A Boolean indicating the success or failure of the connection attempt.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     */
    public suspend fun connectToDevice(context: Context, address: String): Boolean

    /**
     * Disconnects the current BLE device connection.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     */

    public suspend fun disconnect()

    /**
     * Subscribes to the changes in the connection status of the BLE device.
     *
     * @return A [Flow] that emits connection status updates as [BleConnectionStatus].
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     */

    public fun subscribeToConnectionStatusChanges(): Flow<BleConnectionStatus>
}
