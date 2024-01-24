package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.BleManagerGattCallBacks
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattSubscriptions
import com.bortxapps.simplebleclient.manager.utils.checkBleHardwareAvailable
import com.bortxapps.simplebleclient.manager.utils.checkBluetoothEnabled
import com.bortxapps.simplebleclient.manager.utils.checkPermissions
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsNotGrantedApiCodeS
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsNotGrantedOldApi
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class SimpleBleClientSubscriptionImplTest {
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)

    private val contextMock = mockk<Context>(relaxed = true)
    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)

    private val bleManagerGattConnectionOperationsMock = mockk<BleManagerGattConnectionOperations>(relaxed = true)
    private val bleManagerGattSubscriptionsMock = mockk<BleManagerGattSubscriptions>(relaxed = true)
    private val bleManagerGattCallBacksMock = mockk<BleManagerGattCallBacks>(relaxed = true)
    private val bleNetworkMessageProcessorMock = mockk<BleNetworkMessageProcessor>(relaxed = true)
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var gatHolder: GattHolder

    private lateinit var simpleBleClientSubscriptionImpl: SimpleBleClientSubscriptionImpl
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

        gatHolder = GattHolder()

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

        simpleBleClientSubscriptionImpl = spyk(
            SimpleBleClientSubscriptionImpl(
                contextMock,
                gatHolder,
                bleManagerGattSubscriptionsMock,
                bleManagerGattConnectionOperationsMock
            )
        )
    }

    @After
    fun tearDown() {
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    //region subscribeToCharacteristicChanges
    @Test
    fun `subscribeToCharacteristicChanges gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                simpleBleClientSubscriptionImpl.subscribeToCharacteristicChanges(characteristics)
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
        gatHolder.setGatt(bluetoothGattMock)
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns true
        coEvery { bleManagerGattSubscriptionsMock.subscribeToNotifications(bluetoothGattMock, characteristics) } returns true

        assertEquals(true, simpleBleClientSubscriptionImpl.subscribeToCharacteristicChanges(characteristics))
    }

    @Test
    fun `subscribeToCharacteristicChanges gatt initialized discoverServices fails should throw exception`() = runTest {
        gatHolder.setGatt(bluetoothGattMock)
        coEvery { bleManagerGattConnectionOperationsMock.discoverServices(bluetoothGattMock) } returns false

        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                simpleBleClientSubscriptionImpl.subscribeToCharacteristicChanges(characteristics)
            }
        }
    }
    //endregion

    //region subscribeToIncomeMessages
    @Test
    fun `subscribeToIncomeMessages just calls helper class`() {
        every { bleManagerGattSubscriptionsMock.subscribeToIncomeMessages() } returns characteristicMessageFlow

        assertEquals(characteristicMessageFlow, simpleBleClientSubscriptionImpl.subscribeToIncomeMessages())
    }
    //region

    private fun mockValidationsAllOk() {
        coEvery { checkBluetoothEnabled(any()) } returns Unit
        coEvery { checkPermissions(any()) } returns Unit
        coEvery { checkBleHardwareAvailable(any()) } returns Unit
    }
}
