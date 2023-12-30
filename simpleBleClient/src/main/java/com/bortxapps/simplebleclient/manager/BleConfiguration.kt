package com.bortxapps.simplebleclient.manager

internal class BleConfiguration {
    companion object {
        private const val OPERATION_TIME_OUT: Long = 7000
        private const val SCAN_PERIOD: Long = 10000
    }

    var operationTimeoutMillis: Long = OPERATION_TIME_OUT
    var scanPeriodMillis: Long = SCAN_PERIOD
}
