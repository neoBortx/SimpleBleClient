package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.BleNetworkMessageProcessorDefaultImpl
import com.bortxapps.simplebleclient.providers.BleMessageProcessorProvider
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

internal class BleManagerGattReadOperationsTest {

    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)

    private lateinit var bleNetworkMessageProcessor: BleNetworkMessageProcessorDefaultImpl
    private lateinit var bleManagerGattReadOperations: BleManagerGattReadOperations
    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var bleMessageProcessorProvider: BleMessageProcessorProvider
    private lateinit var mutex: Mutex
    private val callbackSlot = slot<BluetoothGattCallback>()
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"

    private val value = ByteArray(1)

    private val bleNetworkMessage = BleNetworkMessage(characteristicUUID, value)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        bleNetworkMessageProcessor = spyk(BleNetworkMessageProcessorDefaultImpl())
        bleConfiguration = BleConfiguration().apply {
            operationTimeoutMillis = 20
            messageProcessor = bleNetworkMessageProcessor
        }
        bleMessageProcessorProvider = BleMessageProcessorProvider(bleConfiguration)


        mutex = Mutex()
        bleManagerGattCallBacks = spyk(BleManagerGattCallBacks(bleConfiguration, bleMessageProcessorProvider))
        bleManagerGattReadOperations = spyk(BleManagerGattReadOperations(bleManagerGattCallBacks, mutex, bleConfiguration))

        every { bluetoothDeviceMock.name } returns goProName
        every { bluetoothDeviceMock.address } returns goProAddress

        every { bluetoothGattMock.getService(serviceUUID)?.getCharacteristic(characteristicUUID) } returns bluetoothCharacteristicMock
        every { bleNetworkMessageProcessor.processMessage(characteristicUUID, value) } just runs
        every { bleNetworkMessageProcessor.isFullyReceived() } returns true
        every { bleNetworkMessageProcessor.getPacket() } returns bleNetworkMessage

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
