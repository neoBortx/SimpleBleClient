package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import app.cash.turbine.test
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.manager.utils.BleNetworkMessageProcessorDefaultImpl
import com.bortxapps.simplebleclient.providers.BleMessageProcessorProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
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

internal class BleManagerGattCallBacksTest {

    private val bluetoothGattMock = mockk<BluetoothGatt>()
    private val bluetoothGattCharacteristicMock = mockk<BluetoothGattCharacteristic>()

    private lateinit var bleNetworkMessageProcessor: BleNetworkMessageProcessorDefaultImpl
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private lateinit var bleMessageProcessorProvider: BleMessageProcessorProvider
    private val value = byteArrayOf(0x01, 0x02, 0x03)
    private val characteristicUUID = UUID.randomUUID()
    private val receivedMessage = BleNetworkMessage(characteristicUUID, value)

    @Before
    fun setUp() {
        bleNetworkMessageProcessor = spyk(BleNetworkMessageProcessorDefaultImpl())
        bleConfiguration = BleConfiguration().apply {
            operationTimeoutMillis = 20
            messageProcessor = bleNetworkMessageProcessor
        }
        bleMessageProcessorProvider = BleMessageProcessorProvider(bleConfiguration)

        bleManagerGattCallBacks = BleManagerGattCallBacks(bleConfiguration, bleMessageProcessorProvider)
        coEvery { bluetoothGattCharacteristicMock.uuid } returns characteristicUUID
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
        bleManagerGattCallBacks.initDeferredConnectOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onConnectionStateChange(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        }

        assertTrue(bleManagerGattCallBacks.waitForConnectionEstablished())
    }

    @Test
    fun waitForDataRead_initReadOperation_simpleResponse_onCharacteristicRead_called_expectSuccess() = runTest {
        coEvery { bleNetworkMessageProcessor.processMessage(characteristicUUID, value) } just runs
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage
        coEvery { bleNetworkMessageProcessor.isFullyReceived() } returns true

        bleManagerGattCallBacks.initDeferredReadOperation()

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
        coVerify { bleNetworkMessageProcessor.processMessage(characteristicUUID, value) }
    }

    @Test
    fun waitForDataRead_initReadOperation_simpleResponse_onCharacteristicChanged_called_expectSuccess() = runTest {
        coEvery { bleNetworkMessageProcessor.processMessage(characteristicUUID, value) } just runs
        coEvery { bleNetworkMessageProcessor.isFullyReceived() } returns true
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage

        bleManagerGattCallBacks.initDeferredReadOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
        }

        assertEquals(receivedMessage, bleManagerGattCallBacks.waitForDataRead())
        coVerify { bleNetworkMessageProcessor.processMessage(characteristicUUID, value) }
    }

    @Test
    fun waitForWrittenDescriptor_initWriteDescriptorOperation_called_expectSuccess() = runTest {
        bleManagerGattCallBacks.initDeferredWriteDescriptorOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onDescriptorWrite(bluetoothGattMock, null, BluetoothGatt.GATT_SUCCESS)
        }

        assertTrue(bleManagerGattCallBacks.waitForWrittenDescriptor())
    }

    @Test
    fun waitForDisconnected_initDisconnectOperation_called_expectSuccess() = runTest {
        bleManagerGattCallBacks.initDeferredDisconnectOperation()

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
        bleManagerGattCallBacks.initDeferredDiscoverServicesOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onServicesDiscovered(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS)
        }

        assertTrue(bleManagerGattCallBacks.waitForServicesDiscovered())
    }

    @Test
    fun waitForConnectionEstablished_errorOnConnectOperation_expectException() = runTest {
        bleManagerGattCallBacks.initDeferredConnectOperation()

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

        bleManagerGattCallBacks.initDeferredReadOperation()

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
        bleManagerGattCallBacks.initDeferredWriteDescriptorOperation()

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
        bleManagerGattCallBacks.initDeferredDisconnectOperation()

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
        bleManagerGattCallBacks.initDeferredDiscoverServicesOperation()

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
        bleManagerGattCallBacks.initDeferredConnectOperation()
        bleManagerGattCallBacks.initDeferredReadOperation()
        bleManagerGattCallBacks.initDeferredWriteDescriptorOperation()
        bleManagerGattCallBacks.initDeferredDisconnectOperation()
        bleManagerGattCallBacks.initDeferredDiscoverServicesOperation()

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

    @Test
    fun subscribeToIncomeMessages_onCharacteristicReads_is_updated_with_income_messages() = runTest {
        launch {
            val uuid = UUID.randomUUID()
            coEvery { bluetoothGattCharacteristicMock.uuid } returns uuid
            bleManagerGattCallBacks.subscribeToIncomeMessages().test {
                bleManagerGattCallBacks.onCharacteristicRead(bluetoothGattMock, bluetoothGattCharacteristicMock, value, BluetoothGatt.GATT_SUCCESS)
                val res = awaitItem()
                assertEquals(uuid, res.characteristicsId)
                assertEquals(value, res.data)
            }
        }
    }

    @Test
    fun subscribeToIncomeMessages_onCharacteristicChanged_is_updated_with_income_messages() = runTest {
        launch {
            val uuid = UUID.randomUUID()
            coEvery { bluetoothGattCharacteristicMock.uuid } returns uuid
            bleManagerGattCallBacks.subscribeToIncomeMessages().test {
                bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
                val res = awaitItem()
                assertEquals(uuid, res.characteristicsId)
                assertEquals(value, res.data)
            }
        }
    }

    @Test
    fun subscribeToIncomeMessages_onCharacteristicChanged_bufferOverflow_messages_old_messages_dropped() = runTest {
        launch {
            val uuid = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()
            val uuid3 = UUID.randomUUID()
            val uuid4 = UUID.randomUUID()
            val uuid5 = UUID.randomUUID()
            coEvery { bluetoothGattCharacteristicMock.uuid } returns uuid

            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
            coEvery { bluetoothGattCharacteristicMock.uuid } returns uuid2
            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
            coEvery { bluetoothGattCharacteristicMock.uuid } returns uuid3
            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
            coEvery { bluetoothGattCharacteristicMock.uuid } returns uuid4
            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)
            coEvery { bluetoothGattCharacteristicMock.uuid } returns uuid5
            bleManagerGattCallBacks.onCharacteristicChanged(bluetoothGattMock, bluetoothGattCharacteristicMock, value)

            bleManagerGattCallBacks.subscribeToIncomeMessages().test {
                val res = awaitItem()
                assertEquals(uuid5, res.characteristicsId)
                assertEquals(value, res.data)
            }
        }
    }
}
