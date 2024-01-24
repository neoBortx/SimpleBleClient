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
    fun `subscribeToConnectionStatusChanges onConnectionStateChangeInvoked expectStateUpdated`() = runTest {
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
    fun `waitForConnectionEstablished initConnectOperation not called expect exception`() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForConnectionEstablished()
            }
        }
    }

    @Test
    fun `waitForDataRead initReadOperation not called expect exception`() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDataRead()
            }
        }
    }

    @Test
    fun `waitForDataWrite initRWriteOperation not called expect exception`() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDataWrite()
            }
        }
    }

    @Test
    fun `waitForWrittenDescriptor initWriteDescriptorOperation not called expect exception`() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForWrittenDescriptor()
            }
        }
    }

    @Test
    fun `waitForDisconnected initDisconnectOperation not called expect exception`() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDisconnected()
            }
        }
    }

    @Test
    fun `waitForServicesDiscovered initDiscoverServicesOperation not called expect exception`() = runTest {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForServicesDiscovered()
            }
        }
    }

    @Test
    fun `waitForConnectionEstablished initConnectOperation called expect success`() = runTest {
        bleManagerGattCallBacks.initDeferredConnectOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onConnectionStateChange(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        }

        assertTrue(bleManagerGattCallBacks.waitForConnectionEstablished())
    }

    @Test
    fun `waitForDataRead initReadOperation simpleResponse onCharacteristicRead called expect success`() = runTest {
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
    fun `waitForDataRead initReadOperation simpleResponse onCharacteristicChanged called expect success`() = runTest {
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
    fun `waitForDataWrite initWriteOperation onCharacteristicWrite called expect success`() = runTest {
        coEvery { bleNetworkMessageProcessor.processMessage(characteristicUUID, value) } just runs
        coEvery { bleNetworkMessageProcessor.isFullyReceived() } returns true
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage

        bleManagerGattCallBacks.initDeferredWriteOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicWrite(bluetoothGattMock, bluetoothGattCharacteristicMock, BluetoothGatt.GATT_SUCCESS)
        }

        assertTrue(bleManagerGattCallBacks.waitForDataWrite())
    }

    @Test
    fun `waitForDataWrite initWriteOperation onCharacteristicWrite throws error called expect exception`() = runTest {
        coEvery { bleNetworkMessageProcessor.processMessage(characteristicUUID, value) } just runs
        coEvery { bleNetworkMessageProcessor.isFullyReceived() } returns true
        coEvery { bleNetworkMessageProcessor.getPacket() } returns receivedMessage

        bleManagerGattCallBacks.initDeferredWriteOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onCharacteristicWrite(bluetoothGattMock, bluetoothGattCharacteristicMock, BluetoothGatt.GATT_FAILURE)
        }

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDataWrite()
            }
        }
    }

    @Test
    fun `waitForWrittenDescriptor initWriteDescriptorOperation called expect success`() = runTest {
        bleManagerGattCallBacks.initDeferredWriteDescriptorOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onDescriptorWrite(bluetoothGattMock, null, BluetoothGatt.GATT_SUCCESS)
        }

        assertTrue(bleManagerGattCallBacks.waitForWrittenDescriptor())
    }

    @Test
    fun `waitForDisconnected initDisconnectOperation called expect success`() = runTest {
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
    fun `waitForServicesDiscovered initDiscoverServicesOperation called expect success`() = runTest {
        bleManagerGattCallBacks.initDeferredDiscoverServicesOperation()

        thread {
            Thread.sleep(100)
            bleManagerGattCallBacks.onServicesDiscovered(bluetoothGattMock, BluetoothGatt.GATT_SUCCESS)
        }

        assertTrue(bleManagerGattCallBacks.waitForServicesDiscovered())
    }

    @Test
    fun `waitForConnectionEstablished errorOnConnectOperation expect exception`() = runTest {
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
    fun `waitForDataRead errorOnReadOperation expect exception`() = runTest {
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
    fun `waitForWrittenDescriptor errorOnWriteDescriptorOperation expect exception`() = runTest {
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
    fun `waitForDisconnected errorOnDisconnectOperation expect exception`() = runTest {
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
    fun `waitForServicesDiscovered errorOnDiscoverServicesOperation expect exception`() = runTest {
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
    fun `testResetFunction expectCancelAllDeferredActions`() = runTest {
        // Initialize operations
        bleManagerGattCallBacks.initDeferredConnectOperation()
        bleManagerGattCallBacks.initDeferredReadOperation()
        bleManagerGattCallBacks.initDeferredWriteDescriptorOperation()
        bleManagerGattCallBacks.initDeferredDisconnectOperation()
        bleManagerGattCallBacks.initDeferredDiscoverServicesOperation()
        bleManagerGattCallBacks.initDeferredWriteOperation()

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

        assertThrows(UninitializedPropertyAccessException::class.java) {
            runBlocking {
                bleManagerGattCallBacks.waitForDataWrite()
            }
        }
    }

    @Test
    fun `reset whenNonInitialized expectJustRuns`() = runTest {
        bleManagerGattCallBacks.reset()
    }

    @Test
    fun `subscribeToIncomeMessages onCharacteristicReads is updated with income messages`() = runTest {
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
    fun `subscribeToIncomeMessages onCharacteristicChanged is updated with income messages`() = runTest {
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
    fun `subscribeToIncomeMessages onCharacteristicChanged bufferOverflow messages old messages dropped`() = runTest {
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
