package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import com.bortxapps.simplebleclient.data.BleNetworkMessage
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BleManagerGattReadOperationsTest {

    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bleNetworkMessageProcessorMock = mockk<BleNetworkMessageProcessor>(relaxed = true)

    private lateinit var bleManagerGattReadOperations: BleManagerGattReadOperations
    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var mutex: Mutex
    private val callbackSlot = slot<BluetoothGattCallback>()
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"

    @OptIn(ExperimentalUnsignedTypes::class)
    private val value = ByteArray(1).toUByteArray()

    @OptIn(ExperimentalUnsignedTypes::class)
    private val bleNetworkMessage = BleNetworkMessage(1, 1, value)

    @OptIn(ExperimentalUnsignedTypes::class)
    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        bleConfiguration = BleConfiguration().apply {
            operationTimeoutMillis = 20
        }
        mutex = Mutex()
        bleManagerGattCallBacks = spyk(BleManagerGattCallBacks(bleNetworkMessageProcessorMock))
        bleManagerGattReadOperations = spyk(BleManagerGattReadOperations(bleManagerGattCallBacks, mutex, bleConfiguration))

        every { bluetoothDeviceMock.name } returns goProName
        every { bluetoothDeviceMock.address } returns goProAddress

        every { bluetoothGattMock.getService(serviceUUID)?.getCharacteristic(characteristicUUID) } returns bluetoothCharacteristicMock
        every { bleNetworkMessageProcessorMock.processMessage(value) } just runs
        every { bleNetworkMessageProcessorMock.processSimpleMessage(value) } just runs
        every { bleNetworkMessageProcessorMock.isReceived() } returns true
        every { bleNetworkMessageProcessorMock.getPacket() } returns bleNetworkMessage

        every { bluetoothDeviceMock.connectGatt(any(), any(), capture(callbackSlot)) } answers {
            bluetoothGattMock
        }

        bluetoothDeviceMock.connectGatt(null, false, bleManagerGattCallBacks)
    }

    @After
    fun tearDown() {
        if (mutex.isLocked) {
            mutex.unlock()
        }
        callbackSlot.clear()
    }

    //region readData
    @Test
    fun testReadData_GattNotInitialized_expectException() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattReadOperations.readData(serviceUUID, characteristicUUID, bluetoothGattMock)
            }
        }
    }

    @Test
    fun testReadData_simpleData_sendSuccess_expectNetworkMessage() = runTest {
        val value = ByteArray(1)
        every { bluetoothGattMock.readCharacteristic(bluetoothCharacteristicMock) } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            true
        }

        assertEquals(bleNetworkMessage, bleManagerGattReadOperations.readData(serviceUUID, characteristicUUID, bluetoothGattMock))
    }

    @Test
    fun testReadData_simpleData_sendFail_expectException() = runTest {
        val value = ByteArray(1)
        every { bluetoothGattMock.readCharacteristic(bluetoothCharacteristicMock) } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            false
        }

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattReadOperations.readData(serviceUUID, characteristicUUID, bluetoothGattMock)
            }
        }
    }

    @Test
    fun testReadData_simpleData_noResponse_expectTimeOutException() = runTest {
        every { bluetoothGattMock.readCharacteristic(bluetoothCharacteristicMock) } answers {
            true
        }

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattReadOperations.readData(serviceUUID, characteristicUUID, bluetoothGattMock)
            }
        }
    }

    @Test
    fun testReadData_simpleData_already_unableToLockTheMutext_expectTimeOutException() = runTest {
        every { bluetoothGattMock.readCharacteristic(bluetoothCharacteristicMock) } answers {
            true
        }

        mutex.lock()
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattReadOperations.readData(serviceUUID, characteristicUUID, bluetoothGattMock)
            }
        }
    }

    @Test
    fun testReadData_simpleData_NullCharacteristic_expectException() = runTest {
        coEvery { bluetoothGattMock.getService(serviceUUID)?.getCharacteristic(characteristicUUID) } returns null

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattReadOperations.readData(serviceUUID, characteristicUUID, bluetoothGattMock)
            }
        }
    }
    //endregion
}
