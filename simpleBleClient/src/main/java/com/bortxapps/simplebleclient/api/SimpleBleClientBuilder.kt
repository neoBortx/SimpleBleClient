package com.bortxapps.simplebleclient.api

import android.content.Context
import com.bortxapps.simplebleclient.di.BleLibraryContainer
import com.bortxapps.simplebleclient.manager.BleManager
import com.bortxapps.simplebleclient.manager.contracts.SimpleBleClient

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
