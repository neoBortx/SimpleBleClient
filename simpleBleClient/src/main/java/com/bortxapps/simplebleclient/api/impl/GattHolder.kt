package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothGatt
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException

internal class GattHolder {

    private var bluetoothGatt: BluetoothGatt? = null

    fun setGatt(bluetoothGatt: BluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt
    }

    fun getGatt() = bluetoothGatt

    fun clear() {
        bluetoothGatt = null
    }

    fun checkGatt() {
        if (bluetoothGatt == null) {
            throw SimpleBleClientException(BleError.CAMERA_NOT_CONNECTED)
        }
    }
}
