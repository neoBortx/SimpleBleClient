package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothGatt
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test

internal class GattHolderTest {

    @Test
    fun `checkGatt should throw exception when gatt is null`() {
        // Given
        val gattHolder = GattHolder()

        // When
        val exception = try {
            gattHolder.checkGatt()
            null
        } catch (e: Exception) {
            e
        }

        // Then
        assert(exception is SimpleBleClientException)
        assert((exception as SimpleBleClientException).bleError == BleError.CAMERA_NOT_CONNECTED)
    }

    @Test
    fun `checkGatt should not throw exception when gatt is not null`() {
        // Given
        val gattHolder = GattHolder()
        gattHolder.setGatt(mockk(relaxed = true))

        // When
        val exception = try {
            gattHolder.checkGatt()
            null
        } catch (e: Exception) {
            e
        }

        // Then
        assert(exception == null)
    }

    @Test
    fun `getGatt should return null when gatt is null`() {
        // Given
        val gattHolder = GattHolder()

        assertNull(gattHolder.getGatt())
    }

    @Test
    fun `getGatt should not return null when gatt is not null`() {
        // Given
        val gattHolder = GattHolder()
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        gattHolder.setGatt(mockGatt)

        // When
        val res = gattHolder.getGatt()
        // Then
        assertNotNull(res)
        assertEquals(mockGatt, res)
    }

    @Test
    fun `clear should set gatt to null`() {
        // Given
        val gattHolder = GattHolder()
        gattHolder.setGatt(mockk(relaxed = true))

        // When
        gattHolder.clear()

        // Then
        assertNull(gattHolder.getGatt())
    }
}
