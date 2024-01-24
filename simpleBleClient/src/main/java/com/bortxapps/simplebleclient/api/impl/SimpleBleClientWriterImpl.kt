package com.bortxapps.simplebleclient.api.impl

import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClientWriter
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.manager.BleManagerGattWriteOperations
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidations
import java.util.UUID

internal class SimpleBleClientWriterImpl(
    private val context: Context,
    private val gattHolder: GattHolder,
    private val bleManagerGattWriteOperations: BleManagerGattWriteOperations
) : SimpleBleClientWriter {
    override suspend fun sendDataWithResponse(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        data: ByteArray
    ): BleNetworkMessage = launchBleOperationWithValidations(context) {
        gattHolder.checkGatt()
        bleManagerGattWriteOperations.sendDataWithResponse(
            serviceUUID,
            characteristicUUID,
            data,
            gattHolder.getGatt()!!
        )
    }

    override suspend fun sendData(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        data: ByteArray
    ) = launchBleOperationWithValidations(context) {
        gattHolder.checkGatt()
        bleManagerGattWriteOperations.sendData(
            serviceUUID,
            characteristicUUID,
            data,
            gattHolder.getGatt()!!
        )
    }
}
