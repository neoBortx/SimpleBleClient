package com.bortxapps.simplebleclient.manager.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

@OptIn(ExperimentalUnsignedTypes::class)
private fun getBluetoothGattCallback(
    onConnected: () -> Unit,
    onDisconnected: () -> Unit,
    onCharacteristicRead: (UByteArray) -> Unit,
    onCharacteristicChanged: (BluetoothGattCharacteristic?, UByteArray) -> Unit,
    onServicesDiscovered: () -> Unit,
    onDescriptorWrite: () -> Unit
) =
    object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BleConnectionManager", "onConnectionStateChange: STATE_CONNECTED")
                    onConnected()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.e("BleConnectionManager", "onConnectionStateChange: STATE_DISCONNECTED")
                    onDisconnected()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == GATT_SUCCESS) {
                onCharacteristicRead(value.toUByteArray())
            } else {
                Log.e("BleConnectionManager", "onCharacteristicRead: ${characteristic.uuid} FAIL")
                Throwable(status.toString())
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            onCharacteristicChanged(characteristic, value.toUByteArray())
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == GATT_SUCCESS) {
                onServicesDiscovered()
            } else {
                Log.e("BleConnectionManager", "onServicesDiscovered: FAIL")
                Throwable(status.toString())
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == GATT_SUCCESS) {
                onDescriptorWrite()
            } else {
                Log.e("BleConnectionManager", "onDescriptorWrite:  FAIL")
                Throwable(status.toString())
            }
        }
    }



@SuppressLint("MissingPermission")
@OptIn(ExperimentalUnsignedTypes::class)
internal fun connectToGoProBleDevice(
    context: Context,
    device: BluetoothDevice,
    onConnected: () -> Unit,
    onDisconnected: () -> Unit,
    onCharacteristicRead: (UByteArray) -> Unit,
    onCharacteristicChanged: (BluetoothGattCharacteristic?, UByteArray) -> Unit,
    onServicesDiscovered: () -> Unit,
    onDescriptorWrite: () -> Unit
): BluetoothGatt =
    device.connectGatt(
        context,
        false,
        getBluetoothGattCallback(
            onConnected,
            onDisconnected,
            onCharacteristicRead,
            onCharacteristicChanged,
            onServicesDiscovered,
            onDescriptorWrite
        )
    )