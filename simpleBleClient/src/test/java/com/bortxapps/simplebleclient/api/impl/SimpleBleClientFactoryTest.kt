package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothProfile
import android.content.Context
import com.bortxapps.simplebleclient.manager.BleManagerDeviceSearchOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattCallBacks
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattReadOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattSubscriptions
import com.bortxapps.simplebleclient.manager.BleManagerGattWriteOperations
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

internal class SimpleBleClientFactoryTest {

    private lateinit var simpleBleClientFactory: SimpleBleClientFactory

    private val contextMock = mockk<Context>(relaxed = true)
    private val bleManagerDeviceConnectionMock = mockk<BleManagerDeviceSearchOperations>(relaxed = true)
    private val bleManagerGattConnectionOperationsMock = mockk<BleManagerGattConnectionOperations>(relaxed = true)
    private val bleManagerGattSubscriptionsMock = mockk<BleManagerGattSubscriptions>(relaxed = true)
    private val bleManagerGattReadOperationsMock = mockk<BleManagerGattReadOperations>(relaxed = true)
    private val bleManagerGattWriteOperationsMock = mockk<BleManagerGattWriteOperations>(relaxed = true)
    private val bleManagerGattCallBacksMock = mockk<BleManagerGattCallBacks>(relaxed = true)

    @Test
    fun `create should returns a simpleBleClientImplInstance`() {
        every { bleManagerGattCallBacksMock.subscribeToConnectionStatusChanges() } returns MutableStateFlow(BluetoothProfile.STATE_CONNECTED)

        simpleBleClientFactory = SimpleBleClientFactory(
            contextMock,
            bleManagerGattCallBacksMock,
            bleManagerGattConnectionOperationsMock,
            bleManagerGattReadOperationsMock,
            bleManagerGattWriteOperationsMock,
            bleManagerGattSubscriptionsMock,
            bleManagerDeviceConnectionMock
        )

        val simpleBleClient = simpleBleClientFactory.create()

        assertNotNull(simpleBleClient)
    }
}
