package com.bortxapps.simplebleclient.exceptions

import org.junit.Assert.assertEquals
import org.junit.Test

internal class SimpleBleClientExceptionTest {

    @Test
    internal fun testNoDetailShouldShowBleErrorAsMessage() {
        val error =  BleError.BLE_DEVICE_NOT_FOUND
        val exception = SimpleBleClientException(error)
        assertEquals( error.toString(), exception.message)
        assertEquals( error, exception.bleError)
    }

    @Test
    internal fun testDetailShouldShowDetailAsMessage() {
        val detail = "Detail"
        val exception = SimpleBleClientException(BleError.BLE_DEVICE_NOT_FOUND, detail)
        assertEquals(detail, exception.message)
        assertEquals(detail, exception.detail)
    }
}
