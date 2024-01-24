package com.bortxapps.simplebleclient.api.impl

import android.content.Context
import com.bortxapps.simplebleclient.manager.BleManagerDeviceSearchOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattCallBacks
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattReadOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattSubscriptions
import com.bortxapps.simplebleclient.manager.BleManagerGattWriteOperations

internal class SimpleBleClientFactory(
    private val context: Context,
    private val bleManagerGattCallBacks: BleManagerGattCallBacks,
    private val bleManagerGattConnectionOperations: BleManagerGattConnectionOperations,
    private val bleManagerGattReadOperations: BleManagerGattReadOperations,
    private val bleManagerGattWriteOperations: BleManagerGattWriteOperations,
    private val bleManagerGattSubscriptions: BleManagerGattSubscriptions,
    private val bleManagerGattSeekOperations: BleManagerDeviceSearchOperations
) {

    private fun createSimpleBleClientDeviceSeekerImpl(): SimpleBleClientDeviceSeekerImpl {
        return SimpleBleClientDeviceSeekerImpl(
            context,
            bleManagerGattSeekOperations
        )
    }

    private fun createSimpleBleClientConnectionImpl(
        gattHolder: GattHolder
    ): SimpleBleClientConnectionImpl {
        return SimpleBleClientConnectionImpl(
            context,
            gattHolder,
            bleManagerGattCallBacks,
            bleManagerGattConnectionOperations
        )
    }

    private fun createSimpleBleClientReaderImpl(
        gattHolder: GattHolder
    ): SimpleBleClientReaderImpl {
        return SimpleBleClientReaderImpl(
            context,
            gattHolder,
            bleManagerGattReadOperations
        )
    }

    private fun createSimpleBleClientWriterImpl(
        gattHolder: GattHolder
    ): SimpleBleClientWriterImpl {
        return SimpleBleClientWriterImpl(
            context,
            gattHolder,
            bleManagerGattWriteOperations
        )
    }

    private fun createSimpleBleClientSubscriptionImpl(
        gattHolder: GattHolder
    ): SimpleBleClientSubscriptionImpl {
        return SimpleBleClientSubscriptionImpl(
            context,
            gattHolder,
            bleManagerGattSubscriptions,
            bleManagerGattConnectionOperations
        )
    }

    private fun createGattHolder(): GattHolder {
        return GattHolder()
    }

    fun create(): SimpleBleClientImpl {
        val gattHolder = createGattHolder()
        return SimpleBleClientImpl(
            createSimpleBleClientDeviceSeekerImpl(),
            createSimpleBleClientConnectionImpl(gattHolder),
            createSimpleBleClientReaderImpl(gattHolder),
            createSimpleBleClientWriterImpl(gattHolder),
            createSimpleBleClientSubscriptionImpl(gattHolder)
        )
    }
}
