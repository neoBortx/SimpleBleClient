package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import app.cash.turbine.test
import com.bortxapps.simplebleclient.data.BleNetworkMessage
import com.bortxapps.simplebleclient.data.BleNetworkMessageProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlin.concurrent.thread

@OptIn(ExperimentalUnsignedTypes::class)
internal class BleManagerGattCallBacksTest {

    private val bleNetworkMessageProcessor = mockk<BleNetworkMessageProcessor>()
    private val bluetoothGattMock = mockk<BluetoothGatt>()
    private val bluetoothGattCharacteristicMock = mockk<BluetoothGattCharacteristic>()

    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private val value = byteArrayOf(0x01, 0x02, 0x03)
    private val receivedMessage = BleNetworkMessage(value.toUByteArray())

    @Before
    fun setUp() {
        bleManagerGattCallBacks = BleManagerGattCallBacks(bleNetworkMessageProcessor)
        coEvery { bluetoothGattCharacteristicMock.uuid } returns UUID.randomUUID()
    }

    @Test
    fun subscribeToConnectionStatusChanges_onConnectionStateChangeInvoked_expectStateUpdated() = runTest {
        launch {
            bleManagerGattCallBacks.subscribeToConnectionStatusChanges().test {
                assertEquals(BluetoothProfile.STATE_DISCONNECTED, awaitItem())
                bleManagerGattCallBacks.onConnectionStateChange(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
                assertEquals(BluetoothProfile.STATE_CONNECTED, awaitItem())

                bleManagerGattCallBacks.onConnectionStateChange(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
                assertEquals(BluetoothProfile.STATE_DISCONNECTED, awaitItem())

                bleManagerGattCallBacks.onConnectionStateChange(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTING)
                assertEquals(BluetoothProfile.STATE_CONNECTING, awaitItem())

                bleManagerGattCallBacks.onConnectionStateChange(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTING)
                assertEquals(BluetoothProfile.STATE_DISCONNECTING, awaitItem())
            }
        }
    }

    @Test
    fun waitForConnectionEstablished_initConnectOperation_not_called_expectException() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForConnectionEstablished()
            }
        }
    }

    @Test
    fun waitForDataRead_initReadOperation_not_called_expectException() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDataRead()
            }
        }
    }

    @Test
    fun waitForWrittenDescriptor_initWriteDescriptorOperation_not_called_expectException() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForWrittenDescriptor()
            }
        }
    }

    @Test
    fun waitForDisconnected_initDisconnectOperation_not_called_expectException() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDisconnected()
            }
        }
    }

    @Test
    fun waitForServicesDiscovered_initDiscoverServicesOperation_not_called_expectException() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForServicesDiscovered()
            }
        }
    }

    @Test
    fun waitForConnectionEstablished_initConnectOperation_called_expectSuccess() = runTest {
        bleManagerGattCallBacks.initConnectOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onConnectionStateChange(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        }

        assertTrue(bleManagerGattCallBacks.waitForConnectionEstablished())
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun waitForDataRead_initReadOperation_simpleResponse_onCharacteristicRead_called_expectSuccess() = runTest {
        coEvery { bleNetworkMessageProcessor.processSimpleMessage(value.toUByteArray()) } just runs
        coEvery { bleNetworkMessageProcessor.isReceived() } returns true
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage

        bleManagerGattCallBacks.initReadOperation(false)

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicRead(
                bluetoothGattMock,
                bluetoothGattCharacteristicMock,
                value,
                BluetoothGatt.GATT_SUCCESS
            )
        }

        assertEquals(receivedMessage, bleManagerGattCallBacks.waitForDataRead())
        coVerify { bleNetworkMessageProcessor.processSimpleMessage(value.toUByteArray()) }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun waitForDataRead_initReadOperation_complexResponse_onCharacteristicRead_called_expectSuccess() = runTest {
        coEvery { bleNetworkMessageProcessor.processMessage(value.toUByteArray()) } just runs
        coEvery { bleNetworkMessageProcessor.isReceived() } returns true
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage

        bleManagerGattCallBacks.initReadOperation(true)

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicRead(
                bluetoothGattMock,
                bluetoothGattCharacteristicMock,
                value,
                BluetoothGatt.GATT_SUCCESS
            )
        }

        assertEquals(receivedMessage, bleManagerGattCallBacks.waitForDataRead())
        coVerify { bleNetworkMessageProcessor.processMessage(value.toUByteArray()) }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun waitForDataRead_initReadOperation_simpleResponse_onCharacteristicChanged_called_expectSuccess() = runTest {
        coEvery { bleNetworkMessageProcessor.processSimpleMessage(value.toUByteArray()) } just runs
        coEvery { bleNetworkMessageProcessor.isReceived() } returns true
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage

        bleManagerGattCallBacks.initReadOperation(false)

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
        }

        assertEquals(receivedMessage, bleManagerGattCallBacks.waitForDataRead())
        coVerify { bleNetworkMessageProcessor.processSimpleMessage(value.toUByteArray()) }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun waitForDataRead_initReadOperation_complexResponse_onCharacteristicChanged_called_expectSuccess() = runTest {
        coEvery { bleNetworkMessageProcessor.processMessage(value.toUByteArray()) } just runs
        coEvery { bleNetworkMessageProcessor.isReceived() } returns true
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage

        bleManagerGattCallBacks.initReadOperation(true)

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
        }

        assertEquals(receivedMessage, bleManagerGattCallBacks.waitForDataRead())
        coVerify { bleNetworkMessageProcessor.processMessage(value.toUByteArray()) }
    }

    @Test
    fun waitForWrittenDescriptor_initWriteDescriptorOperation_called_expectSuccess() = runTest {
        bleManagerGattCallBacks.initWriteDescriptorOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onDescriptorWrite(bluetoothGattMock, null, BluetoothGatt.GATT_SUCCESS)
        }

        assertTrue(bleManagerGattCallBacks.waitForWrittenDescriptor())
    }

    @Test
    fun waitForDisconnected_initDisconnectOperation_called_expectSuccess() = runTest {
        bleManagerGattCallBacks.initDisconnectOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onConnectionStateChange(
                bluetoothGattMock,
                BluetoothGatt.GATT_SUCCESS,
                BluetoothProfile.STATE_DISCONNECTED
            )
        }

        assertTrue(bleManagerGattCallBacks.waitForDisconnected())
    }

    @Test
    fun waitForServicesDiscovered_initDiscoverServicesOperation_called_expectSuccess() = runTest {
        bleManagerGattCallBacks.initDiscoverServicesOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onServicesDiscovered(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS)
        }

        assertTrue(bleManagerGattCallBacks.waitForServicesDiscovered())
    }

    @Test
    fun waitForConnectionEstablished_errorOnConnectOperation_expectException() = runTest {
        bleManagerGattCallBacks.initConnectOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onConnectionStateChange(
                bluetoothGattMock,
                BluetoothGatt.GATT_FAILURE,
                BluetoothProfile.STATE_DISCONNECTED
            )
        }

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForConnectionEstablished()
            }
        }
    }

    @Test
    fun waitForDataRead_errorOnReadOperation_expectException() = runTest {
        coEvery { bleNetworkMessageProcessor.clearData() } just runs

        bleManagerGattCallBacks.initReadOperation(false)

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicRead(
                bluetoothGattMock,
                bluetoothGattCharacteristicMock,
                value,
                BluetoothGatt.GATT_FAILURE
            )
        }

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDataRead()
            }
        }
        coVerify { bleNetworkMessageProcessor.clearData() }
    }

    @Test
    fun waitForWrittenDescriptor_errorOnWriteDescriptorOperation_expectException() = runTest {
        bleManagerGattCallBacks.initWriteDescriptorOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onDescriptorWrite(bluetoothGattMock, null, BluetoothGatt.GATT_FAILURE)
        }

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForWrittenDescriptor()
            }
        }
    }

    @Test
    fun waitForDisconnected_errorOnDisconnectOperation_expectException() = runTest {
        bleManagerGattCallBacks.initDisconnectOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onConnectionStateChange(
                bluetoothGattMock,
                BluetoothGatt.GATT_FAILURE,
                BluetoothProfile.STATE_DISCONNECTED
            )
        }

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDisconnected()
            }
        }
    }

    @Test
    fun waitForServicesDiscovered_errorOnDiscoverServicesOperation_expectException() = runTest {
        bleManagerGattCallBacks.initDiscoverServicesOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onServicesDiscovered(bluetoothGattMock, BluetoothGatt.GATT_FAILURE)
        }

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForServicesDiscovered()
            }
        }
    }

    @Test
    fun testResetFunction_expectCancelAllDeferredActions() = runTest {
        // Initialize operations
        bleManagerGattCallBacks.initConnectOperation()
        bleManagerGattCallBacks.initReadOperation(true)
        bleManagerGattCallBacks.initWriteDescriptorOperation()
        bleManagerGattCallBacks.initDisconnectOperation()
        bleManagerGattCallBacks.initDiscoverServicesOperation()

        // Perform reset
        bleManagerGattCallBacks.reset()

        // Try to complete the operations and assert they don't complete
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForConnectionEstablished()
            }
        }

        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDataRead()
            }
        }

        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForWrittenDescriptor()
            }
        }

        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDisconnected()
            }
        }

        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForServicesDiscovered()
            }
        }
    }

    @Test
    fun reset_whenNonInitialized_expectJustRuns() = runTest {
        bleManagerGattCallBacks.reset()
    }
}
