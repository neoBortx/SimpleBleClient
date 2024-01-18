@file:Suppress("KDocUnresolvedReference")

package com.bortxapps.simplebleclient.api.contracts

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.Keep
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Keep
public interface SimpleBleClientDeviceSeeker {
    /**
     * Retrieves a flow of BLE devices that offers a specified service. This operation has a timeout of 10 seconds by default.
     * The timeout can be changed calling [SimpleBleClientBuilder.setOperationTimeOutMillis] in builder.
     * Launch inside a coroutine to avoid blocking the main thread.
     *
     * You can filter by service UUID and/or device name. If you don't want to filter by service UUID or device name just leave them null
     *
     * You can launch this operation just once at a time. If you need to launch it more than once, you need to wait
     * for the previous one to finish or call [stopSearchDevices] to stop the previous one. If not a
     * [SimpleBleClientException] will be thrown.
     *
     * @param serviceUUID The UUID of the BLE service to filter devices.
     * @param deviceName The name of the BLE device to filter devices.
     * @return A flow of [BluetoothDevice] instances that offer the specified service.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     *
     */
    public fun getDevicesNearby(serviceUUID: UUID? = null, deviceName: String? = null): Flow<BluetoothDevice>

    /**
     *
     * Retrieves a list of BLE devices that are already paired to the device
     *
     * @return A flow of [BluetoothDevice] instances that offer the specified service.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     */

    public suspend fun getPairedDevices(context: Context): List<BluetoothDevice>

    /**
     * Stops the ongoing search for BLE devices.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     */
    public suspend fun stopSearchDevices()
}
