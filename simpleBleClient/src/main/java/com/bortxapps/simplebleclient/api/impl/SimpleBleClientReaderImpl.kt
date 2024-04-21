package com.bortxapps.simplebleclient.api.impl

import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClientReader
import com.bortxapps.simplebleclient.api.data.BleCharacteristic
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.manager.BleManagerGattReadOperations
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidations
import java.util.UUID

internal class SimpleBleClientReaderImpl(
    private val context: Context,
    private val gattHolder: GattHolder,
    private val bleManagerGattReadOperations: BleManagerGattReadOperations
) : SimpleBleClientReader {

    override suspend fun readData(
        serviceUUID: UUID,
        characteristicUUID: UUID
    ): BleNetworkMessage = launchBleOperationWithValidations(context) {
        gattHolder.checkGatt()
        bleManagerGattReadOperations.readData(
            serviceUUID,
            characteristicUUID,
            gattHolder.getGatt()!!
        )
    }

    override suspend fun getAllCharacteristics():
            List<BleCharacteristic> = launchBleOperationWithValidations(context) {
        gattHolder.checkGatt()
        bleManagerGattReadOperations.getAllCharacteristics(gattHolder.getGatt()!!)
    }
}
