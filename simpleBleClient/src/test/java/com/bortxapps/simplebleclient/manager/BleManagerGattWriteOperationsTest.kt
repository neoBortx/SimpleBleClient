package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import com.bortxapps.simplebleclient.data.BleNetworkMessage
import com.bortxapps.simplebleclient.data.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.providers.BuildVersionProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class BleManagerGattWriteOperationsTest {

    private val bleNetworkMessageProcessorMock = mockk<BleNetworkMessageProcessor>(relaxed = true)
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)

    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)
    private val buildVersionProviderMock = mockk<BuildVersionProvider>(relaxed = true)

    private lateinit var bleManagerGattWriteOperations: BleManagerGattWriteOperations
    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var mutex: Mutex
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"
    private val callbackSlot = slot<BluetoothGattCallback>()

    @OptIn(ExperimentalUnsignedTypes::class)
    private val value = ByteArray(1).toUByteArray()

    @OptIn(ExperimentalUnsignedTypes::class)
    private val bleNetworkMessage = BleNetworkMessage(value)

    @OptIn(ExperimentalUnsignedTypes::class)
    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        bleConfiguration = BleConfiguration().apply {
            operationTimeoutMillis = 20
        }
        mutex = Mutex()
        bleManagerGattCallBacks = spyk(BleManagerGattCallBacks(bleNetworkMessageProcessorMock))
        bleManagerGattWriteOperations = spyk(
            BleManagerGattWriteOperations(
                bleManagerGattCallBacks,
                buildVersionProviderMock,
                mutex,
                bleConfiguration
            )
        )
        every { bluetoothDeviceMock.name } returns goProName
        every { bluetoothDeviceMock.address } returns goProAddress
        every { bluetoothDeviceMock2.name } returns "Xiaomi123456"

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

    //region sendData
    @Test
    fun testSendData_GattNotInitialized_expectException() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, ByteArray(1), bluetoothGattMock, false)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testSendData_simpleData_sendSuccess_oldAPI_expectTrue() = runTest {
        val value = ByteArray(1)

        every { bluetoothGattMock.writeCharacteristic(bluetoothCharacteristicMock) } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            true
        }

        TestCase.assertEquals(
            bleNetworkMessage,
            bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun testSendData_complexData_sendSuccess_oldAPI_expectTrue() = runTest {
        val value = ByteArray(1)

        every { bluetoothGattMock.writeCharacteristic(bluetoothCharacteristicMock) } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            true
        }

        TestCase.assertEquals(
            bleNetworkMessage,
            bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, true)
        )

        verify { bleManagerGattCallBacks.initReadOperation(true) }
    }

    @Test
    fun testSendData_simpleData_sendSuccess_newAPI_expectTrue() = runTest {
        val value = ByteArray(1)

        every { buildVersionProviderMock.getSdkVersion() } returns Build.VERSION_CODES.TIRAMISU
        every {
            bluetoothGattMock.writeCharacteristic(
                bluetoothCharacteristicMock,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
        } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            BluetoothStatusCodes.SUCCESS
        }

        TestCase.assertEquals(
            bleNetworkMessage,
            bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun testSendData_simpleData_sendFail_oldAPI_expectTrue() = runTest {
        val value = ByteArray(1)

        every { bluetoothGattMock.writeCharacteristic(bluetoothCharacteristicMock) } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            false
        }

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
            }
        }
    }

    @Test
    fun testSendData_simpleData_sendFail_newAPI_expectTrue() = runTest {
        val value = ByteArray(1)

        every {
            bluetoothGattMock.writeCharacteristic(
                bluetoothCharacteristicMock,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
        } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            BluetoothStatusCodes.FEATURE_NOT_SUPPORTED
        }

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testSendData_simpleData_noResponse_oldAPI_expectTimeOutException() = runTest {
        val value = ByteArray(1)

        every { bluetoothGattMock.writeCharacteristic(bluetoothCharacteristicMock) } answers {
            true
        }

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testSendData_simpleData_sendSuccess_unableToUnlock_oldAPI_expectTimeOutException() = runTest {
        val value = ByteArray(1)

        every { bluetoothGattMock.writeCharacteristic(bluetoothCharacteristicMock) } answers {
            callbackSlot.captured.onCharacteristicRead(bluetoothGattMock, bluetoothCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
            true
        }

        mutex.lock()

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
            }
        }
    }

    @Test
    fun testSendData_simpleData_NullCharacteristic_oldAPI_expectException() = runTest {
        val value = ByteArray(1)

        coEvery { bluetoothGattMock.getService(serviceUUID)?.getCharacteristic(characteristicUUID) } returns null

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testSendData_simpleData_exception_oldAPI_expectException() = runTest {
        val value = ByteArray(1)

        every { bluetoothGattMock.writeCharacteristic(bluetoothCharacteristicMock) } throws Exception()

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock, false)
            }
        }
    }
    //endregion
}
