package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.BleManagerDeviceSearchOperations
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
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class SimpleBleClientDeviceSeekerImplTest {
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)

    private val contextMock = mockk<Context>(relaxed = true)
    private val bluetoothGattMock: BluetoothGatt by lazy { mockk<BluetoothGatt>(relaxed = true) }
    private val bluetoothCharacteristicMock = mockk<BluetoothGattCharacteristic>(relaxed = true)

    private val bleManagerDeviceConnectionMock = mockk<BleManagerDeviceSearchOperations>(relaxed = true)
    private lateinit var bleConfiguration: BleConfiguration

    private lateinit var simpleBleClientDeviceSeekerImpl: SimpleBleClientDeviceSeekerImpl
    private lateinit var mutex: Mutex
    private val serviceUUID = UUID.randomUUID()
    private val characteristicUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"

    @Before
    fun setUp() {
        mockkStatic(::checkBluetoothEnabled)
        mockkStatic(::checkPermissionsNotGrantedApiCodeS)
        mockkStatic(::checkPermissionsNotGrantedOldApi)
        mockkStatic(::checkPermissions)
        mockkStatic(::checkBleHardwareAvailable)

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

        simpleBleClientDeviceSeekerImpl = spyk(
            SimpleBleClientDeviceSeekerImpl(
                contextMock,
                bleManagerDeviceConnectionMock
            )
        )
    }

    @After
    fun tearDown() {
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    //region getDevicesByService
    @Test
    fun `getDevicesByServiceShould call BleManagerDeviceSearchOperations getDevicesByService`() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getDevicesNearBy(serviceUUID, null) } returns flow { emit(bluetoothDeviceMock) }

        simpleBleClientDeviceSeekerImpl.getDevicesNearby(serviceUUID)

        verify { bleManagerDeviceConnectionMock.getDevicesNearBy(serviceUUID, null) }
    }

    @Test
    fun `getPairedDevices should call BleManagerDeviceSearchOperations getPairedDevicesByPrefix`() = runTest {
        coEvery { bleManagerDeviceConnectionMock.getPairedDevices(contextMock) } returns listOf(bluetoothDeviceMock)

        simpleBleClientDeviceSeekerImpl.getPairedDevices(contextMock)

        verify { bleManagerDeviceConnectionMock.getPairedDevices(contextMock) }
    }
    //endregion

    //region stopSearchDevices
    @Test
    fun `stopSearchDevices should call BleManagerDeviceSearchOperations stopSearchDevices`() = runTest {
        simpleBleClientDeviceSeekerImpl.stopSearchDevices()

        verify { bleManagerDeviceConnectionMock.stopSearchDevices() }
    }
    //endregion

    private fun mockValidationsAllOk() {
        coEvery { checkBluetoothEnabled(any()) } returns Unit
        coEvery { checkPermissions(any()) } returns Unit
        coEvery { checkBleHardwareAvailable(any()) } returns Unit
    }
}
