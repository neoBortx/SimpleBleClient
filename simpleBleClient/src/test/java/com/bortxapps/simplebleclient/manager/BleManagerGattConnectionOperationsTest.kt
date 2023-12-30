package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.bortxapps.simplebleclient.data.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BleManagerGattConnectionOperationsTest {

    private val contextMock = mockk<Context>(relaxed = true)
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)
    private val bleNetworkMessageProcessorMock = mockk<BleNetworkMessageProcessor>(relaxed = true)

    private val bleManagerDeviceConnectionMock = mockk<BleManagerDeviceSearchOperations>(relaxed = true)

    private lateinit var bleManagerGattConnectionOperations: BleManagerGattConnectionOperations
    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var mutex: Mutex
    private val callbackSlot = slot<BluetoothGattCallback>()
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mutex = Mutex()
        bleConfiguration = BleConfiguration().apply {
            operationTimeoutMillis = 20
        }
        bleManagerGattCallBacks = spyk(BleManagerGattCallBacks(bleNetworkMessageProcessorMock))
        bleManagerGattConnectionOperations = spyk(BleManagerGattConnectionOperations(bleManagerDeviceConnectionMock, bleManagerGattCallBacks, mutex, bleConfiguration))

        every { bluetoothDeviceMock.name } returns goProName
        every { bluetoothDeviceMock.address } returns goProAddress
        every { bluetoothDeviceMock.toString() } returns ""

        every { bluetoothGattMock.getService(serviceUUID)?.getCharacteristic(characteristicUUID) } returns bluetoothCharacteristicMock
    }

    //region connectToDevice
    @Test
    fun testConnectToDeviceNotFoundDevice_expectFalse() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getDetectedDevices() } returns mutableListOf(bluetoothDeviceMock)

        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.connectToDevice(contextMock, "another_device_address", bleManagerGattCallBacks)
            }
        }
    }

    @Test
    fun testConnectToDeviceConnectFails_expectNull() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getDetectedDevices() } returns mutableListOf(bluetoothDeviceMock)
        coEvery { bleManagerGattConnectionOperations["connect"](contextMock, bluetoothDeviceMock, capture(callbackSlot)) } returns null

        bleManagerGattConnectionOperations.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacks)
    }

    @Test
    fun testConnectToDeviceSuccess_noGattServices_expectConnected() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getDetectedDevices() } returns mutableListOf(bluetoothDeviceMock)
        coEvery {
            bluetoothDeviceMock.connectGatt(
                contextMock,
                false,
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onConnectionStateChange(bluetoothGattMock, 0, BluetoothProfile.STATE_CONNECTED)
            bluetoothGattMock
        }

        coEvery {
            bluetoothGattMock.discoverServices()
        } answers {
            callbackSlot.captured.onServicesDiscovered(bluetoothGattMock, 0)
            true
        }

        bleManagerGattConnectionOperations.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacks)
    }

    @Test
    fun testConnectToDeviceSuccess_noResponse_expectTimeOutException() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getDetectedDevices() } returns mutableListOf(bluetoothDeviceMock)
        coEvery {
            bluetoothDeviceMock.connectGatt(
                contextMock,
                false,
                capture(callbackSlot)
            )
        } answers {
            bluetoothGattMock
        }

        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacks)
            }
        }
    }

    @Test
    fun testConnectToDeviceSuccess_unaBleToMock_expectTimeOutException() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getDetectedDevices() } returns mutableListOf(bluetoothDeviceMock)
        coEvery {
            bluetoothDeviceMock.connectGatt(
                contextMock,
                false,
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onConnectionStateChange(bluetoothGattMock, 0, BluetoothProfile.STATE_CONNECTED)
            bluetoothGattMock
        }

        mutex.lock()
        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.connectToDevice(contextMock, goProAddress, bleManagerGattCallBacks)
            }
        }
    }
    //endregion

    //region disconnect
    @Test
    fun testDisconnectGattDisconnectFails_NoResponse_expecTimeoutException() = runTest {
        every { bluetoothDeviceMock.connectGatt(any(), any(), capture(callbackSlot)) } answers {
            bluetoothGattMock
        }

        bluetoothDeviceMock.connectGatt(null, false, bleManagerGattCallBacks)

        coEvery { bluetoothGattMock.disconnect() } just runs

        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.disconnect(bluetoothGattMock)
            }
        }
    }

    @Test
    fun testDisconnectGattDisconnectFails_unableToLock_expectTimeoutException() = runTest {
        every { bluetoothDeviceMock.connectGatt(any(), any(), capture(callbackSlot)) } answers {
            bluetoothGattMock
        }

        bluetoothDeviceMock.connectGatt(null, false, bleManagerGattCallBacks)

        coEvery { bluetoothGattMock.disconnect() } just runs

        mutex.lock()
        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.disconnect(bluetoothGattMock)
            }
        }
    }

    @Test
    fun testDisconnectGattDisconnectSuccess_responses_expectTrue() = runTest {
        every { bluetoothDeviceMock.connectGatt(any(), any(), capture(callbackSlot)) } answers {
            bluetoothGattMock
        }

        bluetoothDeviceMock.connectGatt(null, false, bleManagerGattCallBacks)

        coEvery { bluetoothGattMock.disconnect() } answers {
            callbackSlot.captured.onConnectionStateChange(bluetoothGattMock, 0, BluetoothProfile.STATE_DISCONNECTED)
            bluetoothGattMock
        }

        assertTrue(bleManagerGattConnectionOperations.disconnect(bluetoothGattMock))
    }
    //endregion

    //region freeConnection
    @Test
    fun testFreeConnectionGattCloseFails() = runTest {
        coEvery { bluetoothGattMock.close() } just runs

        bleManagerGattConnectionOperations.freeConnection(bluetoothGattMock)
        verify(exactly = 1) { bluetoothGattMock.close() }
    }

    @Test
    fun testFreeConnectionGattCloseFails_throwsException_expectSimpleBleClientException() = runTest {
        coEvery { bluetoothGattMock.close() } throws Exception()

        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.disconnect(bluetoothGattMock)
            }
        }
    }

    @Test
    fun testFreeConnectionGattCloseFails_throwsSimpleBleClientException_expectSimpleBleClientException() = runTest {
        coEvery { bluetoothGattMock.close() } throws Exception()

        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.disconnect(bluetoothGattMock)
            }
        }
    }
    //endregion

    //region discoverServices
    @Test
    fun testDiscoverServicesFails_NoResponse_expectTimeoutException() = runTest {
        every { bluetoothDeviceMock.connectGatt(any(), any(), capture(callbackSlot)) } answers {
            bluetoothGattMock
        }

        bluetoothDeviceMock.connectGatt(null, false, bleManagerGattCallBacks)

        coEvery { bluetoothGattMock.discoverServices() } returns true

        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.discoverServices(bluetoothGattMock)
            }
        }
    }

    @Test
    fun testDiscoverServicesSuccess_unableToLock_expectTimeoutException() = runTest {
        every { bluetoothDeviceMock.connectGatt(any(), any(), capture(callbackSlot)) } answers {
            bluetoothGattMock
        }

        bluetoothDeviceMock.connectGatt(null, false, bleManagerGattCallBacks)

        coEvery { bluetoothGattMock.discoverServices() } answers {
            callbackSlot.captured.onServicesDiscovered(bluetoothGattMock, 0)
            true
        }

        mutex.lock()
        assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattConnectionOperations.discoverServices(bluetoothGattMock)
            }
        }
    }

    @Test
    fun testDiscoverServicesSuccess_responses_expectTrue() = runTest {
        every { bluetoothDeviceMock.connectGatt(any(), any(), capture(callbackSlot)) } answers {
            bluetoothGattMock
        }

        bluetoothDeviceMock.connectGatt(null, false, bleManagerGattCallBacks)

        coEvery { bluetoothGattMock.discoverServices() } answers {
            callbackSlot.captured.onServicesDiscovered(bluetoothGattMock, 0)
            true
        }

        assertTrue(bleManagerGattConnectionOperations.discoverServices(bluetoothGattMock))
    }

    //endregion
}
