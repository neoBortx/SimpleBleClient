package com.bortxapps.simplebleclient.manager.utils

import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_CONNECTING
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTING
import com.bortxapps.simplebleclient.api.data.BleConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

internal class MapBleConnectionStateTest {

    @Test
    fun `mapBleConnectionState with STATE_DISCONNECTED should return BleConnectionStatusDISCONNECTED`() {
        // Arrange
        val state = STATE_DISCONNECTED

        // Act
        val result = mapBleConnectionState(state)

        // Assert
        assertEquals(BleConnectionStatus.DISCONNECTED, result)
    }

    @Test
    fun `mapBleConnectionState with STATE_CONNECTING should return BleConnectionStatusCONNECTING`() {
        // Arrange
        val state = STATE_CONNECTING

        // Act
        val result = mapBleConnectionState(state)

        // Assert
        assertEquals(BleConnectionStatus.CONNECTING, result)
    }

    @Test
    fun `mapBleConnectionState with STATE_CONNECTED should return BleConnectionStatusCONNECTED`() {
        // Arrange
        val state = STATE_CONNECTED

        // Act
        val result = mapBleConnectionState(state)

        // Assert
        assertEquals(BleConnectionStatus.CONNECTED, result)
    }

    @Test
    fun `mapBleConnectionState with STATE_DISCONNECTING should return BleConnectionStatusDISCONNECTING`() {
        // Arrange
        val state = STATE_DISCONNECTING

        // Act
        val result = mapBleConnectionState(state)

        // Assert
        assertEquals(BleConnectionStatus.DISCONNECTING, result)
    }

    @Test
    fun `mapBleConnectionState with unknown state should return BleConnectionStatusUNKNOWN`() {
        // Arrange
        val state = 9999

        // Act
        val result = mapBleConnectionState(state)

        // Assert
        assertEquals(BleConnectionStatus.UNKNOWN, result)
    }
}
