package com.bortxapps.simplebleclient.manager

import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor

internal class BleConfiguration {
    companion object {
        private const val OPERATION_TIME_OUT: Long = 7000
        private const val SCAN_PERIOD: Long = 10000
        private const val MESSAGE_BUFFER_SIZE: Int = 1
        private const val MESSAGE_BUFFER_RETRIES: Int = 1
    }

    var operationTimeoutMillis: Long = OPERATION_TIME_OUT
    var scanPeriodMillis: Long = SCAN_PERIOD
    var messageProcessor: BleNetworkMessageProcessor? = null
    var messageBufferSize: Int = MESSAGE_BUFFER_SIZE
    var messageBufferRetries: Int = MESSAGE_BUFFER_RETRIES
}
