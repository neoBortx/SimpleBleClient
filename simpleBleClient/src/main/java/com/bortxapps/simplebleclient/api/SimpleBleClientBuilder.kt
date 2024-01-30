package com.bortxapps.simplebleclient.api

import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClient
import com.bortxapps.simplebleclient.api.impl.SimpleBleClientFactory
import com.bortxapps.simplebleclient.di.BleLibraryContainer
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import org.jetbrains.annotations.VisibleForTesting

/**
 * Builder for creating instances of [SimpleBleClient].
 *
 * This builder provides a fluent interface to configure and create an instance of [SimpleBleClient],
 * which is the main entry point for interacting with BLE devices using the SimpleBleClient library.
 *
 * Usage involves setting up any required configurations and then calling `build(context)` to create
 * an instance of [SimpleBleClient] that can be used for BLE operations.
 *
 * You can configure the following:
 *  - Operation timeout (in milliseconds) - The maximum amount of time to wait for a BLE operation to complete.
 *      If the operation does not complete within this time, an exception will be thrown.
 *  - Scan period (in milliseconds) - The maximum amount of time to scan for BLE devices.
 *      If no devices are found within this time, the library will return an empty list
 *   - Message processor - The message processor to use for processing messages received from BLE devices.
 *      If not set, a default message processor will be used witch will returns messages as they are received, so for complex messages you
 *      will have to subscribe to SimpleBleClientSubscription.subscribeToIncomeMessages and develop your own message processor and operation
 *      scheduler because ble operations are not thread safe and can't be concurrent.
 *      If you set a custom message processor, the library will manage the concurrency to retrieve the hole message before processing it and
 *      launching the next operation. The custom message processor will be used for all the operations and you must set the logic to handle,
 *      buffer the messages received from the device and determine which messages are fragmented and which are not.
 *
 * @throws SimpleBleClientException Thrown when an error occurs
 * Usage example:
 * ```
 * val simpleBleClient = SimpleBleClientBuilder()
 *     .setOperationTimeOutMillis(10000) // Optional: Set custom operation timeout (in milliseconds)
 *     .setScanPeriodMillis(20000) // Optional: Set custom scan period (in milliseconds)
 *     .setMessageProcessor(customMessageProcessor) // Optional: Set custom message processor
 *     .build(context)
 *
 */
public class SimpleBleClientBuilder {
    private var operationTimeOutMillisConf: Long? = null
    private var scanPeriodMillisConf: Long? = null
    private var messageProcessorConf: BleNetworkMessageProcessor? = null
    private var messageBufferSizeConf: Int? = null
    private var messageBufferRetriesConf: Int? = null

    @VisibleForTesting
    internal var bleLibraryContainer: BleLibraryContainer = BleLibraryContainer()

    public fun setOperationTimeOutMillis(timeout: Long): SimpleBleClientBuilder =
        apply { operationTimeOutMillisConf = timeout }

    public fun setScanPeriodMillis(timeout: Long): SimpleBleClientBuilder =
        apply { scanPeriodMillisConf = timeout }

    public fun setMessageProcessor(messageProcessor: BleNetworkMessageProcessor): SimpleBleClientBuilder =
        apply { this.messageProcessorConf = messageProcessor }

    public fun setMessageBufferSize(messageBufferSize: Int): SimpleBleClientBuilder =
        apply { this.messageBufferSizeConf = messageBufferSize }

    public fun setMessageBufferRetries(messageBufferRetries: Int): SimpleBleClientBuilder =
        apply { this.messageBufferRetriesConf = messageBufferRetries }

    public fun build(context: Context): SimpleBleClient {
        return buildInstance(context)
    }

    private fun buildInstance(context: Context): SimpleBleClient {
        bleLibraryContainer.init(context)

        with(bleLibraryContainer.getBleConfiguration()) {
            operationTimeOutMillisConf?.let { operationTimeoutMillis = it }
            scanPeriodMillisConf?.let { scanPeriodMillis = it }
            messageProcessorConf?.let { messageProcessor = it }
            messageBufferSizeConf?.let { messageBufferSize = it }
            messageBufferRetriesConf?.let { messageBufferRetries = it }
        }

        with(bleLibraryContainer) {
            return SimpleBleClientFactory(
                context.applicationContext,
                getBleManagerGattCallBacks(),
                getBleManagerGattConnectionOperations(),
                getBleManagerGattReadOperations(),
                getBleManagerGattWriteOperations(),
                getBleManagerGattSubscriptions(),
                getBleManagerDeviceSearchOperations()
            ).create()
        }
    }
}
