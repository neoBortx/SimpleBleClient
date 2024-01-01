package com.bortxapps.simplebleclient.api.contracts

import android.content.Context
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import kotlinx.coroutines.flow.MutableStateFlow

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
     * TODO change to return an enum instead of an INT
     * Subscribes to the changes in the connection status of the BLE device.
     *
     * @return A [MutableStateFlow] that emits connection status updates as integers.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     */

    public fun subscribeToConnectionStatusChanges(): MutableStateFlow<Int>
}
