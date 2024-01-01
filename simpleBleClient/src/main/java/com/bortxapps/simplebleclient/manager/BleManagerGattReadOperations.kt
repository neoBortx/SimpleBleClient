package com.bortxapps.simplebleclient.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.BleManagerGattOperationBase
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

internal class BleManagerGattReadOperations(
    private val bleManagerGattCallBacks: BleManagerGattCallBacks,
    gattMutex: Mutex,
    bleConfiguration: BleConfiguration
) :
    BleManagerGattOperationBase(gattMutex, bleConfiguration) {

    //region read data
    @SuppressLint("MissingPermission")
    suspend fun readData(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        bluetoothGatt: BluetoothGatt
    ): BleNetworkMessage {
        val resultRead: BleNetworkMessage?

        val characteristic = launchGattOperation {
            bluetoothGatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
        }
        resultRead = launchGattOperation {
            bleManagerGattCallBacks.initDeferredReadOperation()
            if (bluetoothGatt.readCharacteristic(characteristic)) {
                launchDeferredOperation {
                    bleManagerGattCallBacks.waitForDataRead()
                }
            } else {
                null
            }
        }

        return resultRead ?: throw SimpleBleClientException(BleError.SEND_COMMAND_FAILED)
    }
    //endregion
}
