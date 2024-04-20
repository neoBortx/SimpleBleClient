package com.bortxapps.simplebleclient.di

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import androidx.core.content.ContextCompat
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException

internal fun getBlueToothScannerFactory(context: Context): BluetoothLeScanner =
    ContextCompat.getSystemService(context.applicationContext, BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
        ?: throw SimpleBleClientException(BleError.UNABLE_INITIALIZE_CONTROLLER, "Bluetooth scanner not found in the system.")
