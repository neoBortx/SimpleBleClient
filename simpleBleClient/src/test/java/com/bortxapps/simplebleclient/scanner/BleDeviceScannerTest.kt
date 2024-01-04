package com.bortxapps.simplebleclient.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES
import android.content.Context
import android.os.Looper
import app.cash.turbine.test
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleConfiguration
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class BleDeviceScannerTest {

    private val contextMock = mockk<Context>(relaxed = true)
    private val bluetoothDeviceMock = mockk<BluetoothDevice>(relaxed = true)
    private val bluetoothLeScannerMock = mockk<android.bluetooth.le.BluetoothLeScanner>(relaxed = true)
    private val bluetoothAdapterMock = mockk<android.bluetooth.BluetoothAdapter> {
        every { bluetoothLeScanner } returns bluetoothLeScannerMock
    }
    private val bleDeviceScannerFilterBuilderMock = spyk(BleDeviceScannerFilterBuilder(), recordPrivateCalls = true)
    private val bleDeviceScannerSettingsBuilderMock = mockk<BleDeviceScannerSettingsBuilder>(relaxed = true)
    private val bleConfiguration = BleConfiguration()

    private val serviceUuid = UUID.randomUUID()
    private val deviceName = "Name"
    private var bleDeviceScanner = BleDeviceScannerManager(
        bluetoothLeScannerMock,
        bleDeviceScannerSettingsBuilderMock,
        bleDeviceScannerFilterBuilderMock,
        BleDeviceScannerCallbackBuilder(),
        bleConfiguration
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(Looper::class)
        every { contextMock.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothAdapterMock
        val looper = mockk<Looper> {
            every { thread } returns Thread.currentThread()
        }
        every { Looper.getMainLooper() } returns looper

        coEvery { bleDeviceScannerFilterBuilderMock.buildFilterByService(serviceUuid) } returns mockk(relaxed = true)
        coEvery { bleDeviceScannerFilterBuilderMock.buildFilterByDeviceName(deviceName) } returns mockk(relaxed = true)
    }

    @Test
    fun testSuccessfulBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        val scanResultMock = mockk<ScanResult>(relaxed = true)
        every { scanResultMock.device } returns bluetoothDeviceMock

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onScanResult(
                CALLBACK_TYPE_ALL_MATCHES,
                scanResultMock
            )
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(bluetoothDeviceMock, awaitItem())
        }

        coVerify(exactly = 1) { bleDeviceScannerFilterBuilderMock.buildFilterByService(serviceUuid) }
        coVerify(exactly = 1) { bleDeviceScannerFilterBuilderMock.buildFilterByDeviceName(deviceName) }
    }

    @Test
    fun testSuccessfulReturnsNullResultBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onScanResult(
                CALLBACK_TYPE_ALL_MATCHES,
                null
            )
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(BleError.CANNOT_START_SEARCHING_DEVICES, (this.awaitError() as SimpleBleClientException).bleError)
        }
    }

    @Test
    fun testSuccessfulReturnsNullDeviceBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        val scanResultMock = mockk<ScanResult>(relaxed = true)
        every { scanResultMock.device } returns null

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onScanResult(
                CALLBACK_TYPE_ALL_MATCHES,
                scanResultMock
            )
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(BleError.CANNOT_START_SEARCHING_DEVICES, (this.awaitError() as SimpleBleClientException).bleError)
        }
    }

    @Test
    fun testSuccessfulBatchBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        val scanResultMock = mockk<ScanResult>(relaxed = true)
        every { scanResultMock.device } returns bluetoothDeviceMock

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onBatchScanResults(
                listOf(scanResultMock, scanResultMock, scanResultMock)
            )
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(bluetoothDeviceMock, awaitItem())
            assertEquals(bluetoothDeviceMock, awaitItem())
            assertEquals(bluetoothDeviceMock, awaitItem())
        }
    }

    @Test
    fun testSuccessfulBatchReturnsNullResultBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onBatchScanResults(
                null
            )
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(BleError.CANNOT_START_SEARCHING_DEVICES, (this.awaitError() as SimpleBleClientException).bleError)
        }
    }

    @Test
    fun testSuccessfulBatchReturnsNullDeviceBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        val scanResultMock1 = mockk<ScanResult>(relaxed = true)
        every { scanResultMock1.device } returns bluetoothDeviceMock
        val scanResultMock2 = mockk<ScanResult>(relaxed = true)
        every { scanResultMock2.device } returns null

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onBatchScanResults(
                listOf(scanResultMock1, scanResultMock2)
            )
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(bluetoothDeviceMock, awaitItem())
            assertEquals(BleError.CANNOT_START_SEARCHING_DEVICES, (this.awaitError() as SimpleBleClientException).bleError)
        }
    }

    @Test
    fun testSuccessfulBatchReturnsEmptyDeviceBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onBatchScanResults(
                listOf()
            )
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            expectNoEvents()
        }
    }

    @Test
    fun testExceptionBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        val scanResultMock = mockk<ScanResult>(relaxed = true)
        every { scanResultMock.device } returns bluetoothDeviceMock

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } throws Exception("Mock exception")

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(BleError.CANNOT_START_SEARCHING_DEVICES, (this.awaitError() as SimpleBleClientException).bleError)
        }
    }

    @Test
    fun testFailBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onScanFailed(0)
        }

        bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName).test {
            assertEquals(BleError.CANNOT_START_SEARCHING_DEVICES, (this.awaitError() as SimpleBleClientException).bleError)
        }
    }

    @Test
    fun testStopSearchSuccessfulBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        val scanResultMock = mockk<ScanResult>(relaxed = true)
        every { scanResultMock.device } returns bluetoothDeviceMock

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onScanResult(
                CALLBACK_TYPE_ALL_MATCHES,
                scanResultMock
            )
        }

        val flow = bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName)

        flow.test {
            awaitItem()
            bleDeviceScanner.stopSearch()
            expectNoEvents()
            awaitComplete()
        }
    }

    @Test
    fun testSPosyDelayedBleScanFindOneDevice() = runTest {
        val callbackSlot = slot<ScanCallback>()
        val scanResultMock = mockk<ScanResult>(relaxed = true)
        every { scanResultMock.device } returns bluetoothDeviceMock
        bleConfiguration.scanPeriodMillis = 60

        bleDeviceScanner = BleDeviceScannerManager(
            bluetoothLeScannerMock,
            bleDeviceScannerSettingsBuilderMock,
            bleDeviceScannerFilterBuilderMock,
            BleDeviceScannerCallbackBuilder(),
            bleConfiguration
        )

        // Mock BLE scan results
        coEvery {
            bluetoothLeScannerMock.startScan(
                any(),
                any(),
                capture(callbackSlot)
            )
        } answers {
        }

        runBlocking {
            val flow = bleDeviceScanner.scanBleDevicesNearby(serviceUuid, deviceName)

            flow.test {
                expectNoEvents()
                awaitComplete()
            }
        }
    }
}
