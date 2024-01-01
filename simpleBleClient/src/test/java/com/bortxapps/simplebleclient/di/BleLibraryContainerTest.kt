package com.bortxapps.simplebleclient.di

import android.content.Context
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

internal class BleLibraryContainerTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(::getBlueToothScannerFactory)
        every { context.applicationContext } returns context
        every { getBlueToothScannerFactory(context) } returns mockk(relaxed = true)
    }

    @Test
    fun `call getBleConfiguration before init should throw an exception`() {
        val bleLibraryContainer = BleLibraryContainer()

        try {
            bleLibraryContainer.getBleConfiguration()
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.LIBRARY_NOT_INITIALIZED, e.bleError)
        }
    }

    @Test
    fun `call getBleManagerDeviceSearchOperations before init should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        try {
            bleLibraryContainer.getBleManagerDeviceSearchOperations()
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.LIBRARY_NOT_INITIALIZED, e.bleError)
        }
    }

    @Test
    fun `call getBleManagerGattConnectionOperations before init should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        try {
            bleLibraryContainer.getBleManagerGattConnectionOperations()
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.LIBRARY_NOT_INITIALIZED, e.bleError)
        }
    }

    @Test
    fun `call getBleManagerGattSubscriptions after before should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        try {
            bleLibraryContainer.getBleManagerGattSubscriptions()
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.LIBRARY_NOT_INITIALIZED, e.bleError)
        }
    }

    @Test
    fun `call getBleManagerGattWriteOperations after before should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        try {
            bleLibraryContainer.getBleManagerGattWriteOperations()
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.LIBRARY_NOT_INITIALIZED, e.bleError)
        }
    }

    @Test
    fun `call getBleManagerGattReadOperations after before should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        try {
            bleLibraryContainer.getBleManagerGattReadOperations()
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.LIBRARY_NOT_INITIALIZED, e.bleError)
        }
    }

    @Test
    fun `call getBleManagerGattCallBacks before init should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        try {
            bleLibraryContainer.getBleManagerGattCallBacks()
        } catch (e: SimpleBleClientException) {
            assertEquals(BleError.LIBRARY_NOT_INITIALIZED, e.bleError)
        }
    }

    @Test
    fun `call getBleConfiguration after after should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        bleLibraryContainer.init(context)

        assertNotNull(bleLibraryContainer.getBleConfiguration())
    }

    @Test
    fun `call getBleManagerDeviceSearchOperations after after should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        bleLibraryContainer.init(context)

        assertNotNull(bleLibraryContainer.getBleManagerDeviceSearchOperations())
    }

    @Test
    fun `call getBleManagerGattConnectionOperations after after should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        bleLibraryContainer.init(context)

        assertNotNull(bleLibraryContainer.getBleManagerGattConnectionOperations())
    }

    @Test
    fun `call getBleManagerGattSubscriptions after after should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        bleLibraryContainer.init(context)

        assertNotNull(bleLibraryContainer.getBleManagerGattSubscriptions())
    }

    @Test
    fun `call getBleManagerGattWriteOperations after after should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        bleLibraryContainer.init(context)

        assertNotNull(bleLibraryContainer.getBleManagerGattWriteOperations())
    }

    @Test
    fun `call getBleManagerGattReadOperations after after should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        bleLibraryContainer.init(context)

        assertNotNull(bleLibraryContainer.getBleManagerGattReadOperations())
    }

    @Test
    fun `call getBleManagerGattCallBacks after after should return the configuration`() {
        val bleLibraryContainer = BleLibraryContainer()

        bleLibraryContainer.init(context)

        assertNotNull(bleLibraryContainer.getBleManagerGattCallBacks())
    }
}