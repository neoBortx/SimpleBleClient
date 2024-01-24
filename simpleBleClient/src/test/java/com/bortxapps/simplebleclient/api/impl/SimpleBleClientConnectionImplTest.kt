package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.BleManagerGattCallBacks
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattSubscriptions
import com.bortxapps.simplebleclient.manager.utils.checkBleHardwareAvailable
import com.bortxapps.simplebleclient.manager.utils.checkBluetoothEnabled
import com.bortxapps.simplebleclient.manager.utils.checkPermissions
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsNotGrantedApiCodeS
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsNotGrantedOldApi
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class SimpleBleClientConnectionImplTest {

    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)

    private val contextMock = mockk<Context>(relaxed = true)

    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)

    private val bleManagerGattConnectionOperationsMock = mockk<BleManagerGattConnectionOperations>(relaxed = true)
    private val bleManagerGattSubscriptionsMock = mockk<BleManagerGattSubscriptions>(relaxed = true)
    private val bleManagerGattCallBacksMock = mockk<BleManagerGattCallBacks>(relaxed = true)
    private val bleNetworkMessageProcessorMock = mockk<BleNetworkMessageProcessor>(relaxed = true)
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var gatHolder: GattHolder

    private lateinit var simpleBleClientConnectionImpl: SimpleBleClientConnectionImpl
    private lateinit var mutex: Mutex
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"
    private val characteristics = listOf(characteristicUUID)

    private val value = ByteArray(1)

    private val bleNetworkMessage = BleNetworkMessage(characteristicUUID, value)

    @Before
    fun setUp() {
        mockkStatic(::checkBluetoothEnabled)
        mockkStatic(::checkPermissionsNotGrantedApiCodeS)
        mockkStatic(::checkPermissionsNotGrantedOldApi)
        mockkStatic(::checkPermissions)
        mockkStatic(::checkBleHardwareAvailable)

        gatHolder = GattHolder()

        MockKAnnotations.init(this)
        mutex = Mutex()
        bleConfiguration = BleConfiguration().apply {
            operationTimeoutMillis = 20
        }

        mockValidationsAllOk()

        every { bluetoothDeviceMock.name } returns goProName
        every { bluetoothDeviceMock.address } returns goProAddress
        every { bluetoothDeviceMock2.name } returns "Xiaomi123456"

        every { bluetoothGattMock.getService(serviceUUID)?.getCharacteristic(characteristicUUID) } returns bluetoothCharacteristicMock
        every { bleNetworkMessageProcessorMock.processMessage(characteristicUUID, value) } just runs
        every { bleNetworkMessageProcessorMock.isFullyReceived() } returns true
        every { bleNetworkMessageProcessorMock.getPacket() } returns bleNetworkMessage
        every { bleManagerGattCallBacksMock.subscribeToConnectionStatusChanges() } returns MutableStateFlow(BluetoothProfile.STATE_CONNECTED)

        simpleBleClientConnectionImpl = spyk(
            SimpleBleClientConnectionImpl(
                contextMock,
                gatHolder,
                bleManagerGattCallBacksMock,
                bleManagerGattConnectionOperationsMock

            )
        )
    }

    @After
    fun tearDown() {
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    //region connectToDevice
    @Test
    fun `connectToDevice should call BleManagerGattConnectionOperations connectToDevice`() = runTest {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns true
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true

        runBlocking {
            Assert.assertTrue(simpleBleClientConnectionImpl.connectToDevice(contextMock, goProAddress))
            coVerify { bleManagerGattConnectionOperationsMock.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacksMock) }
        }
    }

    @Test
    fun `connectToDevice returns null expect SimpleBleClientException`() = runTest {
        coEvery { bleManagerGattConnectionOperationsMock.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacksMock) } returns null
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns true
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                simpleBleClientConnectionImpl.connectToDevice(contextMock, goProAddress)
            }
        }

        coVerify { bleManagerGattConnectionOperationsMock.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacksMock) }
    }
    //endregion

    //region disconnect
    @Test
    fun `disconnect gatt not initialized expect exception`() {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                simpleBleClientConnectionImpl.disconnect()
            }
        }
    }

    @Test
    fun `disconnect gatt initialized result error expect free Reources not call`() {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false

        runBlocking {
            simpleBleClientConnectionImpl.connectToDevice(contextMock, goProAddress)
            simpleBleClientConnectionImpl.disconnect()
            verify(exactly = 0) { simpleBleClientConnectionImpl["freeResources"]() }
        }
    }

    @Test
    fun `disconnect gatt initialized result success expect free resources`() {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns true
        coEvery { bleManagerGattConnectionOperationsMock.freeConnection(bluetoothGattMock) } just runs
        coEvery { bleManagerGattCallBacksMock.reset() } just runs

        runBlocking {
            simpleBleClientConnectionImpl.connectToDevice(contextMock, goProAddress)
            simpleBleClientConnectionImpl.disconnect()
            coVerify { bleManagerGattConnectionOperationsMock.freeConnection(bluetoothGattMock) }
            coVerify { bleManagerGattCallBacksMock.reset() }
        }
    }
    //endregion

    //region subscribeToConnectionStatusChanges
    @Test
    fun `subscribeToConnectionStatusChanges just calls helper class`() = runTest {
        simpleBleClientConnectionImpl.subscribeToConnectionStatusChanges()

        coVerify { simpleBleClientConnectionImpl.subscribeToConnectionStatusChanges() }
    }
    //endregion

    private fun mockValidationsAllOk() {
        coEvery { checkBluetoothEnabled(any()) } returns Unit
        coEvery { checkPermissions(any()) } returns Unit
        coEvery { checkBleHardwareAvailable(any()) } returns Unit
    }
}
