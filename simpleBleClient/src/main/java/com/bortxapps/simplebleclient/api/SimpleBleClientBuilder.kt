package com.bortxapps.simplebleclient.api

import android.content.Context
import com.bortxapps.simplebleclient.api.SimpleBleClientBuilder.operationTimeOutMillis
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClient
import com.bortxapps.simplebleclient.di.BleLibraryContainer
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleManager

/**
 * Builder for creating instances of [SimpleBleClient].
 *
 * This builder provides a fluent interface to configure and create an instance of [SimpleBleClient],
 * which is the main entry point for interacting with BLE devices using the SimpleBleClient library.
 *
 * Usage involves setting up any required configurations and then calling `build(context)` to create
 * an instance of [SimpleBleClient] that can be used for BLE operations.
 *
 * @property operationTimeOutMillis Optional configuration to set a custom timeout for BLE operations.
 *                                  If not set, a default timeout defined in the library will be used.
 *                                  A SimpleBleClientException will be thrown if the timeout is exceeded.
 *@property Context The context to use for BLE operations. To avoid memory leaks, this should be the
 *                                  application context.
 *
 * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
 * Usage example:
 * ```
 * val simpleBleClient = SimpleBleClientBuilder
 *     .setOperationTimeOutMillis(10000) // Optional: Set custom operation timeout (in milliseconds)
 *     .build(context)
 *
 */
public object SimpleBleClientBuilder {
    private var operationTimeOutMillis: Long? = null
    public fun setOperationTimeOutMillis(timeout: Long): SimpleBleClientBuilder = apply { operationTimeOutMillis = timeout }
    public fun build(context: Context): SimpleBleClient {
        return buildInstance(context)
    }

    private fun buildInstance(context: Context): SimpleBleClient {
        val container = BleLibraryContainer(context)
        operationTimeOutMillis?.let { container.bleConfiguration.operationTimeoutMillis = it }
        return BleManager(
            container.bleManagerDeviceSearchOperations,
            container.bleManagerGattConnectionOperations,
            container.bleManagerGattSubscriptions,
            container.bleManagerGattReadOperations,
            container.bleManagerGattWriteOperations,
            container.bleManagerGattCallBacks,
            context.applicationContext
        )
    }
}
