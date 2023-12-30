package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.bortxapps.simplebleclient.data.BleNetworkMessage
import com.bortxapps.simplebleclient.data.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.checkBleHardwareAvailable
import com.bortxapps.simplebleclient.manager.utils.checkBluetoothEnabled
import com.bortxapps.simplebleclient.manager.utils.checkPermissions
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsApiCodeS
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsOldApi
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class BleManagerTest {

    private val bleNetworkMessageProcessorMock = mockk<BleNetworkMessageProcessor>(relaxed = true)
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)

    private val contextMock = mockk<Context>(relaxed = true)
    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)

    private val bleManagerDeviceConnectionMock = mockk<BleManagerDeviceSearchOperations>(relaxed = true)
    private val bleManagerGattConnectionOperationsMock = mockk<BleManagerGattConnectionOperations>(relaxed = true)
    private val bleManagerGattSubscriptionsMock = mockk<BleManagerGattSubscriptions>(relaxed = true)
    private val bleManagerGattReadOperationsMock = mockk<BleManagerGattReadOperations>(relaxed = true)
    private val bleManagerGattWriteOperationsMock = mockk<BleManagerGattWriteOperations>(relaxed = true)
    private val bleManagerGattCallBacksMock = mockk<BleManagerGattCallBacks>(relaxed = true)
    private lateinit var bleConfiguration: BleConfiguration

    private lateinit var bleManager: BleManager
    private lateinit var mutex: Mutex
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"
    private val characteristics = listOf(characteristicUUID)

    @OptIn(ExperimentalUnsignedTypes::class)
    private val value = ByteArray(1).toUByteArray()

    @OptIn(ExperimentalUnsignedTypes::class)
    private val bleNetworkMessage = BleNetworkMessage(value)

    @OptIn(ExperimentalUnsignedTypes::class)
    @Before
    fun setUp() {
        mockkStatic(::checkBluetoothEnabled)
        mockkStatic(::checkPermissionsApiCodeS)
        mockkStatic(::checkPermissionsOldApi)
        mockkStatic(::checkPermissions)
        mockkStatic(::checkBleHardwareAvailable)

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
        every { bleNetworkMessageProcessorMock.processMessage(value) } just runs
        every { bleNetworkMessageProcessorMock.processSimpleMessage(value) } just runs
        every { bleNetworkMessageProcessorMock.isReceived() } returns true
        every { bleNetworkMessageProcessorMock.getPacket() } returns bleNetworkMessage
        every { bleManagerGattCallBacksMock.subscribeToConnectionStatusChanges() } returns MutableStateFlow(BluetoothProfile.STATE_CONNECTED)

        bleManager = spyk(
            BleManager(
                bleManagerDeviceConnectionMock,
                bleManagerGattConnectionOperationsMock,
                bleManagerGattSubscriptionsMock,
                bleManagerGattReadOperationsMock,
                bleManagerGattWriteOperationsMock,
                bleManagerGattCallBacksMock,
                contextMock
            )
        )
    }

    @After
    fun tearDown() {
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    //region getDevicesByService
    @Test
    fun `getDevicesByServiceShould call BleManagerDeviceSearchOperations getDevicesByService`() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getDevicesByService(serviceUUID, null) } returns flow { emit(bluetoothDeviceMock) }

        bleManager.getDevicesNearby(serviceUUID)

        verify { bleManagerDeviceConnectionMock.getDevicesByService(serviceUUID, null) }
    }

    @Test
    fun `getPairedDevicesByPrefix should call BleManagerDeviceSearchOperations getPairedDevicesByPrefix`() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getPairedDevicesByPrefix(contextMock, goProName) } returns listOf(bluetoothDeviceMock)

        bleManager.getPairedDevicesByPrefix(contextMock, goProName)

        verify { bleManagerDeviceConnectionMock.getPairedDevicesByPrefix(contextMock, goProName) }
    }
    //endregion

    //region stopSearchDevices
    @Test
    fun `stopSearchDevices should call BleManagerDeviceSearchOperations stopSearchDevices`() = runTest {
        bleManager.stopSearchDevices()

        verify { bleManagerDeviceConnectionMock.stopSearchDevices() }
    }
    //endregion

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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs

        runBlocking {
            assertTrue(bleManager.connectToDevice(contextMock, goProAddress))
            coVerify { bleManagerGattConnectionOperationsMock.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacksMock) }
        }
    }

    @Test
    fun `connectToDevice returns null expect SimpleBleClientException`() = runTest {
        coEvery { bleManagerGattConnectionOperationsMock.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacksMock) } returns null
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns true
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManager.connectToDevice(contextMock, goProAddress)
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
                bleManager.disconnect()
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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.disconnect()
            verify(exactly = 0) { bleManager["freeResources"]() }
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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns true
        coEvery { bleManagerGattConnectionOperationsMock.freeConnection(bluetoothGattMock) } just runs
        coEvery { bleManagerGattCallBacksMock.reset() } just runs

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.disconnect()
            coVerify { bleManagerGattConnectionOperationsMock.freeConnection(bluetoothGattMock) }
            coVerify { bleManagerGattCallBacksMock.reset() }
        }
    }
    //endregion

    //region subscribeToConnectionStatusChanges
    @Test
    fun `subscribeToConnectionStatusChanges just calls helper class`() = runTest {
        bleManager.subscribeToConnectionStatusChanges()

        coVerify { bleManager.subscribeToConnectionStatusChanges() }
    }
    //endregion

    //region sendData
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun `sendData gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManager.sendData(serviceUUID, characteristicUUID, value.toByteArray(), false)
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun `sendData gatt initialized expect read data invoked`() = runTest {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false
        coEvery {
            bleManagerGattWriteOperationsMock.sendData(
                serviceUUID,
                characteristicUUID,
                value.toByteArray(),
                bluetoothGattMock,
                false
            )
        } returns bleNetworkMessage

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.sendData(serviceUUID, characteristicUUID, value.toByteArray(), false)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun `sendData gatt initialized complex flag expect read data invoked`() = runTest {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false
        coEvery {
            bleManagerGattWriteOperationsMock.sendData(
                serviceUUID,
                characteristicUUID,
                value.toByteArray(),
                bluetoothGattMock,
                true
            )
        } returns bleNetworkMessage

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.sendData(serviceUUID, characteristicUUID, value.toByteArray(), true)
        }
    }
    //endregion

    //region readData
    @Test
    fun `readData gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManager.readData(serviceUUID, characteristicUUID, false)
            }
        }
    }

    @Test
    fun `readData gatt initialized expect read data invoked`() = runTest {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false
        coEvery {
            bleManagerGattReadOperationsMock.readData(
                serviceUUID,
                characteristicUUID,
                bluetoothGattMock,
                false
            )
        } returns bleNetworkMessage

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.readData(serviceUUID, characteristicUUID, false)
        }
    }

    @Test
    fun `readData gatt initialized complex flag expect read data invoked`() = runTest {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } just runs
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false
        coEvery {
            bleManagerGattReadOperationsMock.readData(
                serviceUUID,
                characteristicUUID,
                bluetoothGattMock,
                true
            )
        } returns bleNetworkMessage

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.readData(serviceUUID, characteristicUUID, true)
        }
    }
    //endregion

    private fun mockValidationsAllOk() {
        coEvery { checkBluetoothEnabled(any()) } returns Unit
        coEvery { checkPermissions(any()) } returns Unit
        coEvery { checkBleHardwareAvailable(any()) } returns Unit
    }
}
