package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.BleManagerGattWriteOperations
import com.bortxapps.simplebleclient.manager.utils.checkBleHardwareAvailable
import com.bortxapps.simplebleclient.manager.utils.checkBluetoothEnabled
import com.bortxapps.simplebleclient.manager.utils.checkPermissions
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsNotGrantedApiCodeS
import com.bortxapps.simplebleclient.manager.utils.checkPermissionsNotGrantedOldApi
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class SimpleBleClientWriterImplTest {
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)

    private val contextMock = mockk<Context>(relaxed = true)
    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)

    private val bleManagerGattWriteOperationsMock = mockk<BleManagerGattWriteOperations>(relaxed = true)
    private lateinit var bleConfiguration: BleConfiguration
    private lateinit var gatHolder: GattHolder

    private lateinit var simpleBleClientWriterImpl: SimpleBleClientWriterImpl

    private lateinit var mutex: Mutex
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"

    private val value = ByteArray(1)

    private val bleNetworkMessage = BleNetworkMessage(characteristicUUID, value)

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

        simpleBleClientWriterImpl = spyk(
            SimpleBleClientWriterImpl(
                contextMock,
                gatHolder,
                bleManagerGattWriteOperationsMock
            )
        )
    }

    @After
    fun tearDown() {
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    //region sendData
    @Test
    fun `sendDataWithResponse gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                simpleBleClientWriterImpl.sendDataWithResponse(serviceUUID, characteristicUUID, value)
            }
        }
    }

    @Test
    fun `sendDataWithResponse gatt initialized expect send data invoked`() = runTest {
        gatHolder.setGatt(bluetoothGattMock)
        coEvery {
            bleManagerGattWriteOperationsMock.sendDataWithResponse(
                serviceUUID,
                characteristicUUID,
                value,
                bluetoothGattMock
            )
        } returns bleNetworkMessage

        runBlocking {
            simpleBleClientWriterImpl.sendDataWithResponse(serviceUUID, characteristicUUID, value)
        }
    }

    @Test
    fun `sendData gatt not initialized expect exception`() = runTest {
        Assert.assertThrows(SimpleBleClientException::class.java) {
            runBlocking {
                simpleBleClientWriterImpl.sendData(serviceUUID, characteristicUUID, value)
            }
        }
    }

    @Test
    fun `sendData gatt initialized expect send data invoked`() = runTest {
        gatHolder.setGatt(bluetoothGattMock)
        coEvery {
            bleManagerGattWriteOperationsMock.sendDataWithResponse(
                serviceUUID,
                characteristicUUID,
                value,
                bluetoothGattMock
            )
        } returns bleNetworkMessage

        runBlocking {
            simpleBleClientWriterImpl.sendData(serviceUUID, characteristicUUID, value)
        }
    }
    //endregion

    private fun mockValidationsAllOk() {
        coEvery { checkBluetoothEnabled(any()) } returns Unit
        coEvery { checkPermissions(any()) } returns Unit
        coEvery { checkBleHardwareAvailable(any()) } returns Unit
    }
}
