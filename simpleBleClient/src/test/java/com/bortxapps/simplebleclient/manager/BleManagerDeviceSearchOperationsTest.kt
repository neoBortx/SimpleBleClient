package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import app.cash.turbine.test
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class BleManagerDeviceSearchOperationsTest {

    private val bleScannerMock = mockk<BleDeviceScannerManager>(relaxed = true)
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothDeviceMock2 = mockk<BluetoothDevice>(relaxed = true)
    private val contextMock = mockk<Context>(relaxed = true)
    private val bluetoothLeScannerMock = mockk<android.bluetooth.le.BluetoothLeScanner>(relaxed = true)
    private val bluetoothAdapterMock = mockk<android.bluetooth.BluetoothAdapter> {
        every { bluetoothLeScanner } returns bluetoothLeScannerMock
    }
    private val bluetoothManagerMock = mockk<BluetoothManager> {
        every { adapter } returns bluetoothAdapterMock
    }

    private lateinit var bleManagerDeviceConnection: BleManagerDeviceSearchOperations
    private val serviceUUID = UUID.randomUUID()
    private val goProName = "GoPro123456"
    private val goProAddress = "568676970987986"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        bleManagerDeviceConnection = spyk(BleManagerDeviceSearchOperations(bleScannerMock))

        every { bluetoothDeviceMock.name } returns goProName
        every { bluetoothDeviceMock.address } returns goProAddress
        every { bluetoothDeviceMock2.name } returns "Xiaomi123456"
    }

    //region getDevicesByService
    @Test
    fun testGetDevicesByServiceReturnListOfDevicesAndKeepRunning() = runTest {
        coEvery { bleScannerMock.scanBleDevicesNearby(serviceUUID, null) } returns flow {
            emit(bluetoothDeviceMock)
            emit(bluetoothDeviceMock2)
        }

        bleManagerDeviceConnection.getDevicesByService(serviceUUID, null).test {
            TestCase.assertEquals(bluetoothDeviceMock, awaitItem())
            TestCase.assertEquals(bluetoothDeviceMock2, awaitItem())
            awaitComplete()
        }

        assertEquals(bluetoothDeviceMock, bleManagerDeviceConnection.getDetectedDevices()[0])
        assertEquals(bluetoothDeviceMock2, bleManagerDeviceConnection.getDetectedDevices()[1])
    }

    @Test
    fun testGetDevicesByServiceLaunchTwice_expectException() = runTest {
        coEvery { bleScannerMock.scanBleDevicesNearby(serviceUUID, null) } returns flow {
            emit(bluetoothDeviceMock)
        }

        bleManagerDeviceConnection.getDevicesByService(serviceUUID, null)

        Assert.assertThrows(SimpleBleClientException::class.java) {
            bleManagerDeviceConnection.getDevicesByService(serviceUUID, null)
        }
    }

    @Test
    fun testGetDevicesByServiceConsumeTwice_expectNoException() = runTest {
        coEvery { bleScannerMock.scanBleDevicesNearby(serviceUUID, null) } returns flow {
            emit(bluetoothDeviceMock)
        }

        bleManagerDeviceConnection.getDevicesByService(serviceUUID, null).test {
            TestCase.assertEquals(bluetoothDeviceMock, awaitItem())
            awaitComplete()
        }

        bleManagerDeviceConnection.getDevicesByService(serviceUUID, null).test {
            TestCase.assertEquals(bluetoothDeviceMock, awaitItem())
            awaitComplete()
        }
    }
    //endregion

    //region getPairedDevicesByPrefix
    @Test
    fun testGetPairedDevicesByPrefix() = runTest {
        every { contextMock.getSystemService(BluetoothManager::class.java) } returns bluetoothManagerMock
        every { bluetoothAdapterMock.bondedDevices } returns setOf(bluetoothDeviceMock, bluetoothDeviceMock2)

        val result = bleManagerDeviceConnection.getPairedDevicesByPrefix(contextMock, "GoPro")

        TestCase.assertEquals(1, result.size)
        TestCase.assertEquals(bluetoothDeviceMock, result.first())
    }

    @Test
    fun testGetPairedDevicesByPrefixReturnsEmpty_expectEmpty() = runTest {
        every { contextMock.getSystemService(BluetoothManager::class.java) } returns bluetoothManagerMock
        every { bluetoothAdapterMock.bondedDevices } returns setOf()

        val result = bleManagerDeviceConnection.getPairedDevicesByPrefix(contextMock, "GoPro")

        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun testGetPairedDevicesByPrefixNullManager_expectEmpty() = runTest {
        every { contextMock.getSystemService(BluetoothManager::class.java) } returns null
        every { bluetoothAdapterMock.bondedDevices } returns setOf(bluetoothDeviceMock, bluetoothDeviceMock2)

        val result = bleManagerDeviceConnection.getPairedDevicesByPrefix(contextMock, "GoPro")

        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun testGetPairedDevicesByPrefixNullAdapter_expectEmpty() = runTest {
        every { contextMock.getSystemService(BluetoothManager::class.java) } returns bluetoothManagerMock
        every { bluetoothManagerMock.adapter } returns null
        every { bluetoothAdapterMock.bondedDevices } returns setOf(bluetoothDeviceMock, bluetoothDeviceMock2)

        val result = bleManagerDeviceConnection.getPairedDevicesByPrefix(contextMock, "GoPro")

        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun testGetPairedDevicesByPrefixNullBoundedDevices_expectEmpty() = runTest {
        every { contextMock.getSystemService(BluetoothManager::class.java) } returns bluetoothManagerMock
        every { bluetoothAdapterMock.bondedDevices } returns null

        val result = bleManagerDeviceConnection.getPairedDevicesByPrefix(contextMock, "GoPro")

        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun testGetPairedDevicesByPrefixNoMatchingFilter_expectEmpty() = runTest {
        every { contextMock.getSystemService(BluetoothManager::class.java) } returns bluetoothManagerMock
        every { bluetoothManagerMock.adapter } returns null
        every { bluetoothAdapterMock.bondedDevices } returns setOf(bluetoothDeviceMock2)

        val result = bleManagerDeviceConnection.getPairedDevicesByPrefix(contextMock, "GoPro")

        Assert.assertTrue(result.isEmpty())
    }
    //endregion

    //region stopSearchDevices
    @Test
    fun testStopSearchDevices() = runTest {
        every { bleScannerMock.stopSearch() } just runs

        bleManagerDeviceConnection.stopSearchDevices()

        verify { bleScannerMock.stopSearch() }
    }
    //endregion
}
