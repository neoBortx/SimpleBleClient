package com.bortxapps.simplebleclient.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.BleManagerGattOperationBase
import com.bortxapps.simplebleclient.providers.BuildVersionProvider
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

internal class BleManagerGattWriteOperations(
    private val bleManagerGattCallBacks: BleManagerGattCallBacks,
    private val buildVersionProvider: BuildVersionProvider,
    gattMutex: Mutex,
    bleConfiguration: BleConfiguration
) : BleManagerGattOperationBase(gattMutex, bleConfiguration) {

    //region send data
    suspend fun sendDataWithResponse(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        data: ByteArray,
        bluetoothGatt: BluetoothGatt
    ): BleNetworkMessage {
        bluetoothGatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)?.let {
            return writeCharacteristicWithResponse(
                bluetoothGatt,
                data,
                it
            )
        } ?: run {
            Log.e("BleManager", "writeCharacteristic characteristic is null")
            throw SimpleBleClientException(BleError.SEND_COMMAND_FAILED_NO_CHARACTERISTIC_FOUND_TO_SEND, "Characteristic is null")
        }
    }

    suspend fun sendData(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        data: ByteArray,
        bluetoothGatt: BluetoothGatt
    ) {
        bluetoothGatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)?.let {
            writeCharacteristic(
                bluetoothGatt,
                data,
                it
            )
        } ?: run {
            Log.e("BleManager", "writeCharacteristic characteristic is null")
            throw SimpleBleClientException(BleError.SEND_COMMAND_FAILED_NO_CHARACTERISTIC_FOUND_TO_SEND, "Characteristic is null")
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission", "NewApi")
    private suspend fun writeCharacteristicWithResponse(
        bluetoothGatt: BluetoothGatt,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic
    ): BleNetworkMessage {
        return launchGattOperation {
            bleManagerGattCallBacks.initDeferredReadOperation()
            val res = if (buildVersionProvider.getSdkVersion() >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt.writeCharacteristic(
                    characteristic,
                    value,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                bluetoothGatt.writeCharacteristic(characteristic.apply { this.value = value })
            }

            if (res) {
                launchDeferredOperation {
                    bleManagerGattCallBacks.waitForDataRead()
                }
            } else {
                null
            }
        } ?: run {
            throw SimpleBleClientException(BleError.SEND_COMMAND_FAILED_NO_DATA_RECEIVED_IN_RESPONSE, "No data received from device")
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission", "NewApi")
    private suspend fun writeCharacteristic(
        bluetoothGatt: BluetoothGatt,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic
    ) {
        launchGattOperation {
            bleManagerGattCallBacks.initDeferredWriteOperation()
            val res = if (buildVersionProvider.getSdkVersion() >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt.writeCharacteristic(
                    characteristic,
                    value,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                bluetoothGatt.writeCharacteristic(characteristic.apply { this.value = value })
            }

            if (res) {
                launchDeferredOperation {
                    bleManagerGattCallBacks.waitForDataWrite()
                }
            } else {
                null
            }
        } ?: run {
            throw SimpleBleClientException(BleError.SEND_COMMAND_FAILED_NO_DATA_RECEIVED_IN_RESPONSE, "No data received from device")
        }
    }
    //endregion
}
