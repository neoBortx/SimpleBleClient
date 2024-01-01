package com.bortxapps.simplebleclient.manager.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.providers.BuildVersionProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class BleValidationsTest {

    private val versionProvider = mockk<BuildVersionProvider>(relaxed = true)

    private val context = mockk<Context>(relaxed = true)
    private val bluetoothManager = mockk<BluetoothManager>()
    private val bluetoothAdapter = mockk<BluetoothAdapter>()
    private val packageManager = mockk<PackageManager>()

    @Before
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.getSystemService(BluetoothManager::class.java) } returns bluetoothManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
    }

    @Test
    fun `checkBluetoothEnabled is enabled should execute without error`() {
        every { bluetoothAdapter.isEnabled } returns true

        checkBluetoothEnabled(context)
    }

    @Test
    fun `checkBluetoothEnabled is not enabled should execute without error`() {
        every { bluetoothAdapter.isEnabled } returns false

        try {
            checkBluetoothEnabled(context)
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.BLE_NOT_ENABLED, e.bleError)
        }
    }

    @Test
    fun `checkPermissionsApiCode version api lower returns false`() {
        every { versionProvider.getSdkVersion() } returns 22

        assertFalse(checkPermissionsNotGrantedApiCodeS(context, versionProvider))
    }

    @Test
    fun `checkPermissionsNotGrantedApiCodeS version api upper and permissions not granted should returns true`() {
        every { versionProvider.getSdkVersion() } returns 32
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns PERMISSION_DENIED
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) } returns PERMISSION_DENIED

        assertTrue(checkPermissionsNotGrantedApiCodeS(context, versionProvider))
    }

    @Test
    fun `checkPermissionsNotGrantedApiCodeS version api upper and permissions granted should returns false`() {
        every { versionProvider.getSdkVersion() } returns 32
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns PERMISSION_GRANTED
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) } returns PERMISSION_GRANTED

        assertFalse(checkPermissionsNotGrantedApiCodeS(context, versionProvider))
    }

    @Test
    fun `checkPermissionsNotGrantedOldApi version api lower and permissions not granted should returns true`() {
        every { versionProvider.getSdkVersion() } returns 22

        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) } returns PERMISSION_DENIED

        assertTrue(checkPermissionsNotGrantedOldApi(context, versionProvider))
    }

    @Test
    fun `checkPermissionsNotGrantedOldApi version api lower and permissions granted should returns false`() {
        every { versionProvider.getSdkVersion() } returns 22

        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) } returns PERMISSION_GRANTED

        assertFalse(checkPermissionsNotGrantedOldApi(context, versionProvider))
    }


    @Test
    fun `checkPermissions version api S and not granted should throw exception`() {
        every { versionProvider.getSdkVersion() } returns 32
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns PERMISSION_DENIED
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) } returns PERMISSION_DENIED

        try {
            checkPermissions(context, versionProvider)
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.MISSING_BLE_PERMISSIONS, e.bleError)
        }
    }

    @Test
    fun `checkPermissions version api S and granted should execute without error`() {
        every { versionProvider.getSdkVersion() } returns 32
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns PERMISSION_GRANTED
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) } returns PERMISSION_GRANTED

        checkPermissions(context, versionProvider)
    }

    @Test
    fun `checkPermissions version api lower and not granted should throw exception`() {
        every { versionProvider.getSdkVersion() } returns 22
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) } returns PERMISSION_DENIED

        try {
            checkPermissions(context, versionProvider)
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.MISSING_BLE_PERMISSIONS, e.bleError)
        }
    }

    @Test
    fun `checkPermissions version api lower and granted should runs without exception`() {
        every { versionProvider.getSdkVersion() } returns 22
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) } returns PERMISSION_GRANTED

        checkPermissions(context, versionProvider)
    }

    @Test
    fun `checkBleHardwareAvailable hardware is supported should execute without error`() {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) } returns true

        // Call your function - expecting no exception
        checkBleHardwareAvailable(context)
    }

    @Test
    fun `checkBleHardwareAvailable hardware is not supported should throw exception`() {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) } returns false

        try {
            checkBleHardwareAvailable(context)
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.BLE_NOT_SUPPORTED, e.bleError)
        }
    }

    @Test
    fun `launchBleOperationWithValidations action throws exception should throw exception`() = runTest {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) } returns true
        every { versionProvider.getSdkVersion() } returns 22
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) } returns PERMISSION_GRANTED
        every { bluetoothAdapter.isEnabled } returns true

        try {
            launchBleOperationWithValidations(context) {
                throw NullPointerException()
            }
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.OTHER, e.bleError)
        }
    }

    @Test
    fun `launchBleOperationWithValidations action throws SimpleBleClientException should throw exception`() = runTest {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) } returns true
        every { versionProvider.getSdkVersion() } returns 22
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) } returns PERMISSION_GRANTED
        every { bluetoothAdapter.isEnabled } returns true

        try {
            launchBleOperationWithValidations(context) {
                throw SimpleBleClientException(BleError.INTERNAL_ERROR)
            }
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.INTERNAL_ERROR, e.bleError)
        }
    }

}