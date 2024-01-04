package com.bortxapps.simplebleclient.manager.utils

import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_CONNECTING
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTING
import com.bortxapps.simplebleclient.api.data.BleConnectionStatus

internal fun mapBleConnectionState(state: Int): BleConnectionStatus {
    return when (state) {
        STATE_DISCONNECTED -> BleConnectionStatus.DISCONNECTED
        STATE_CONNECTING -> BleConnectionStatus.CONNECTING
        STATE_CONNECTED -> BleConnectionStatus.CONNECTED
        STATE_DISCONNECTING -> BleConnectionStatus.DISCONNECTING
        else -> BleConnectionStatus.UNKNOWN
    }
}
