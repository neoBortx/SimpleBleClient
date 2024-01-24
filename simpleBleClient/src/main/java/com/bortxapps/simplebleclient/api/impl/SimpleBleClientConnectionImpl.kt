package com.bortxapps.simplebleclient.api.impl

import android.bluetooth.BluetoothGatt
import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClientConnection
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleManagerGattCallBacks
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidations
import com.bortxapps.simplebleclient.manager.utils.mapBleConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class SimpleBleClientConnectionImpl(
    private val context: Context,
    private val gattHolder: GattHolder,
    private val bleManagerGattCallBacks: BleManagerGattCallBacks,
    private val bleManagerGattConnectionOperations: BleManagerGattConnectionOperations
) :
    SimpleBleClientConnection {

    init {
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch(Dispatchers.IO) {
            bleManagerGattCallBacks.subscribeToConnectionStatusChanges().collect {
                if (it == BluetoothGatt.STATE_DISCONNECTED) {
                    freeResources()
                }
            }
        }
    }

    override suspend fun connectToDevice(context: Context, address: String): Boolean =
        launchBleOperationWithValidations(context) {
            bleManagerGattConnectionOperations.connectToDevice(context, address, bleManagerGattCallBacks)?.let {
                gattHolder.setGatt(it)
            } ?: throw SimpleBleClientException(BleError.CAMERA_NOT_CONNECTED)
            true
        }

    override suspend fun disconnect() = launchBleOperationWithValidations(context) {
        gattHolder.checkGatt()
        if (bleManagerGattConnectionOperations.disconnect(gattHolder.getGatt()!!)) {
            freeResources()
        }
    }

    private suspend fun freeResources() {
        gattHolder.getGatt()?.let {
            bleManagerGattConnectionOperations.freeConnection(it)
            bleManagerGattCallBacks.reset()
            gattHolder.clear()
        }
    }

    override fun subscribeToConnectionStatusChanges() = bleManagerGattCallBacks.subscribeToConnectionStatusChanges().map {
        mapBleConnectionState(it)
    }
}
