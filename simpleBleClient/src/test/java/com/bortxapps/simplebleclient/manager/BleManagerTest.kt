package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class BleManagerTest {

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
    private val bleNetworkMessageProcessorMock = mockk<BleNetworkMessageProcessor>(relaxed = true)
    private lateinit var bleConfiguration: BleConfiguration

    private lateinit var bleManager: BleManager
    private lateinit var mutex: Mutex
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"
    private val characteristics = listOf(characteristicUUID)

    private val value = ByteArray(1)

    private val bleNetworkMessage = BleNetworkMessage(characteristicUUID, value)

    private val characteristicMessageFlow = MutableSharedFlow<BleNetworkMessage>()

    @Before
    fun setUp() {
        mockkStatic(::checkBluetoothEnabled)
        mockkStatic(::checkPermissionsNotGrantedApiCodeS)
        mockkStatic(::checkPermissionsNotGrantedOldApi)
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
        every { bleNetworkMessageProcessorMock.processMessage(characteristicUUID, value) } just runs
        every { bleNetworkMessageProcessorMock.isFullyReceived() } returns true
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
        coEvery { bleManagerDeviceConnectionMock.getDevicesNearBy(serviceUUID, null) } returns flow { emit(bluetoothDeviceMock) }

        bleManager.getDevicesNearby(serviceUUID)

        verify { bleManagerDeviceConnectionMock.getDevicesNearBy(serviceUUID, null) }
    }

    @Test
    fun `getPairedDevicesByPrefix should call BleManagerDeviceSearchOperations getPairedDevicesByPrefix`() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getPairedDevicesByPrefix(contextMock) } returns listOf(bluetoothDeviceMock)

        bleManager.getPairedDevicesByPrefix(contextMock)

        verify { bleManagerDeviceConnectionMock.getPairedDevicesByPrefix(contextMock) }
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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true

        runBlocking {
            assertTrue(bleManager.connectToDevice(contextMock, goProAddress))
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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true
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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true
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
    @Test
    fun `sendData gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManager.sendData(serviceUUID, characteristicUUID, value)
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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false
        coEvery {
            bleManagerGattWriteOperationsMock.sendData(
                serviceUUID,
                characteristicUUID,
                value,
                bluetoothGattMock
            )
        } returns bleNetworkMessage

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.sendData(serviceUUID, characteristicUUID, value)
        }
    }
    //endregion

    //region readData
    @Test
    fun `readData gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManager.readData(serviceUUID, characteristicUUID)
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
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true
        coEvery { bleManagerGattConnectionOperationsMock.disconnect(bluetoothGattMock) } returns false
        coEvery {
            bleManagerGattReadOperationsMock.readData(
                serviceUUID,
                characteristicUUID,
                bluetoothGattMock
            )
        } returns bleNetworkMessage

        runBlocking {
            bleManager.connectToDevice(contextMock, goProAddress)
            bleManager.readData(serviceUUID, characteristicUUID)
        }
    }
    //endregion

    //region subscribeToCharacteristicChanges
    @Test
    fun `subscribeToCharacteristicChanges gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManager.subscribeToCharacteristicChanges(characteristics)
            }
        }
    }

    @Test
    fun `subscribeToCharacteristicChanges gatt initialized expect subscribeToNotifications invoked`() = runTest {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns true
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true

        bleManager.connectToDevice(contextMock, goProAddress)
        assertEquals(true, bleManager.subscribeToCharacteristicChanges(characteristics))
    }

    @Test
    fun `subscribeToCharacteristicChanges gatt initialized discoverServices fails should throw exception`() = runTest {
        coEvery {
            bleManagerGattConnectionOperationsMock.connectToDevice(
                contextMock,
                goProAddress,
                bleManagerGattCallBacksMock
            )
        } returns bluetoothGattMock
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManager.connectToDevice(contextMock, goProAddress)
                bleManager.subscribeToCharacteristicChanges(characteristics)
            }
        }
    }
    //endregion

    //region subscribeToIncomeMessages
    @Test
    fun `subscribeToIncomeMessages just calls helper class`() {
        every { bleManagerGattSubscriptionsMock.subscribeToIncomeMessages() } returns characteristicMessageFlow

        assertEquals(characteristicMessageFlow, bleManager.subscribeToIncomeMessages())
    }

    private fun mockValidationsAllOk() {
        coEvery { checkBluetoothEnabled(any()) } returns Unit
        coEvery { checkPermissions(any()) } returns Unit
        coEvery { checkBleHardwareAvailable(any()) } returns Unit
    }
}
