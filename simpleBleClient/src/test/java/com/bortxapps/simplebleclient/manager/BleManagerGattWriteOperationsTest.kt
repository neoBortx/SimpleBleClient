package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.BleNetworkMessageProcessorDefaultImpl
import com.bortxapps.simplebleclient.providers.BleMessageProcessorProvider
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

    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)

    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)
    private val buildVersionProviderMock = mockk<BuildVersionProvider>(relaxed = true)

    private lateinit var bleNetworkMessageProcessor: BleNetworkMessageProcessorDefaultImpl
    private lateinit var bleManagerGattWriteOperations: BleManagerGattWriteOperations
    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var bleMessageProcessorProvider: BleMessageProcessorProvider
    private lateinit var mutex: Mutex
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"
    private val callbackSlot = slot<BluetoothGattCallback>()

    private val value = ByteArray(1)

    private val bleNetworkMessage = BleNetworkMessage(characteristicUUID, value)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mutex = Mutex()
        bleNetworkMessageProcessor = spyk(BleNetworkMessageProcessorDefaultImpl())
        bleConfiguration = BleConfiguration().apply {
            operationTimeoutMillis = 20
            messageProcessor = bleNetworkMessageProcessor
        }
        bleMessageProcessorProvider = BleMessageProcessorProvider(bleConfiguration)

        bleManagerGattCallBacks = spyk(BleManagerGattCallBacks(bleConfiguration, bleMessageProcessorProvider))
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

    //region sendData
    @Test
    fun testSendData_GattNotInitialized_expectException() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, ByteArray(1), bluetoothGattMock)
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
            bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
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
            bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
        )

        verify { bleManagerGattCallBacks.initDeferredReadOperation() }
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
            bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
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
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
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
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
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
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
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
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
            }
        }
    }

    @Test
    fun testSendData_simpleData_NullCharacteristic_oldAPI_expectException() = runTest {
        val value = ByteArray(1)

        coEvery { bluetoothGattMock.getService(serviceUUID)?.getCharacteristic(characteristicUUID) } returns null

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
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
                bleManagerGattWriteOperations.sendData(serviceUUID, characteristicUUID, value, bluetoothGattMock)
            }
        }
    }
    //endregion
}
