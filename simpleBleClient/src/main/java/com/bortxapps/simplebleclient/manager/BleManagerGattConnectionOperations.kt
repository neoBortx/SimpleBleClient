package com.bortxapps.simplebleclient.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.util.Log
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.BleManagerGattOperationBase
import kotlinx.coroutines.sync.Mutex

internal class BleManagerGattConnectionOperations(
    private val bleManagerDeviceSearchOperations: BleManagerDeviceSearchOperations,
    private val bleManagerGattCallBacks: BleManagerGattCallBacks,
    gattMutex: Mutex,
    bleConfiguration: BleConfiguration
) : BleManagerGattOperationBase(gattMutex, bleConfiguration) {

    suspend fun connectToDevice(context: Context, address: String, gattCallBacks: BluetoothGattCallback): BluetoothGatt? {
        bleManagerDeviceSearchOperations.getDetectedDevices().firstOrNull { it.address == address }?.let {
            return connect(context, it, gattCallBacks)
        } ?: run {
            Log.e("BleManager", "connectToDevice ${BleError.BLE_DEVICE_NOT_FOUND}")
            throw SimpleBleClientException(BleError.BLE_DEVICE_NOT_FOUND)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connect(
        context: Context,
        device: BluetoothDevice,
        gattCallBacks: BluetoothGattCallback
    ): BluetoothGatt? = launchGattOperation {
        bleManagerGattCallBacks.initConnectOperation()
        device.connectGatt(context, false, gattCallBacks)?.let {
            launchDeferredOperation {
                bleManagerGattCallBacks.waitForConnectionEstablished()
            }
            it
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun disconnect(
        bluetoothGatt: BluetoothGatt
    ): Boolean = launchGattOperation {
        bleManagerGattCallBacks.initDisconnectOperation()
        bluetoothGatt.disconnect()
        launchDeferredOperation {
            bleManagerGattCallBacks.waitForDisconnected()
        }
        true
    }

    @SuppressLint("MissingPermission")
    suspend fun freeConnection(
        bluetoothGatt: BluetoothGatt
    ) = launchGattOperation {
        bluetoothGatt.close()
    }

    @SuppressLint("MissingPermission")
    suspend fun discoverServices(bluetoothGatt: BluetoothGatt): Boolean = launchGattOperation {
        bleManagerGattCallBacks.initDiscoverServicesOperation()
        bluetoothGatt.discoverServices()
        launchDeferredOperation {
            bleManagerGattCallBacks.waitForServicesDiscovered()
        }
    }
}
