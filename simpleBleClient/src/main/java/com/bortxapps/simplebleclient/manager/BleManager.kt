package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothGatt
import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClient
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidations
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidationsSync
import com.bortxapps.simplebleclient.manager.utils.mapBleConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

internal class BleManager(
    private val bleManagerDeviceConnection: BleManagerDeviceSearchOperations,
    private val bleManagerGattConnectionOperations: BleManagerGattConnectionOperations,
    private val bleManagerGattSubscriptions: BleManagerGattSubscriptions,
    private val bleManagerGattReadOperations: BleManagerGattReadOperations,
    private val bleManagerGattWriteOperations: BleManagerGattWriteOperations,
    private val bleManagerGattCallBacks: BleManagerGattCallBacks,
    private val context: Context
) : SimpleBleClient {

    private var bluetoothGatt: BluetoothGatt? = null

    init {
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch(IO) {
            bleManagerGattCallBacks.subscribeToConnectionStatusChanges().collect {
                if (it == BluetoothGatt.STATE_DISCONNECTED) {
                    freeResources()
                }
            }
        }
    }

    //region search devices
    override fun getDevicesNearby(serviceUUID: UUID?, deviceName: String?) = launchBleOperationWithValidationsSync(context) {
        bleManagerDeviceConnection.getDevicesNearBy(serviceUUID, deviceName)
    }

    override suspend fun getPairedDevicesByPrefix(context: Context) =
        launchBleOperationWithValidations(context) {
            bleManagerDeviceConnection.getPairedDevicesByPrefix(context)
        }

    override suspend fun stopSearchDevices() =
        launchBleOperationWithValidations(context) {
            bleManagerDeviceConnection.stopSearchDevices()
        }
    //endregion

    //region connection
    override suspend fun connectToDevice(context: Context, address: String): Boolean =
        launchBleOperationWithValidations(context) {
            bleManagerGattConnectionOperations.connectToDevice(context, address, bleManagerGattCallBacks)?.let {
                bluetoothGatt = it
            } ?: throw SimpleBleClientException(BleError.CAMERA_NOT_CONNECTED)
            true
        }

    override suspend fun subscribeToCharacteristicChanges(characteristicsUUid: List<UUID>): Boolean =
        launchBleOperationWithValidations(context) {
            checkGatt()
            if (bleManagerGattConnectionOperations.discoverServices(bluetoothGatt!!)) {
                bleManagerGattSubscriptions.subscribeToNotifications(bluetoothGatt!!, characteristicsUUid)
            } else {
                throw SimpleBleClientException(BleError.UNABLE_TO_SUBSCRIBE_TO_NOTIFICATIONS)
            }
        }

    override fun subscribeToIncomeMessages() = bleManagerGattSubscriptions.subscribeToIncomeMessages()

    override suspend fun disconnect() = launchBleOperationWithValidations(context) {
        checkGatt()
        if (bleManagerGattConnectionOperations.disconnect(bluetoothGatt!!)) {
            freeResources()
        }
    }

    private suspend fun freeResources() {
        bluetoothGatt?.let {
            bleManagerGattConnectionOperations.freeConnection(it)
            bleManagerGattCallBacks.reset()
            bluetoothGatt = null
        }
    }

    override fun subscribeToConnectionStatusChanges() = bleManagerGattCallBacks.subscribeToConnectionStatusChanges().map {
        mapBleConnectionState(it)
    }
    //endregion

    //region IO
    override suspend fun sendData(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        data: ByteArray
    ): BleNetworkMessage = launchBleOperationWithValidations(context) {
        checkGatt()
        bleManagerGattWriteOperations.sendData(
            serviceUUID,
            characteristicUUID,
            data,
            bluetoothGatt!!
        )
    }

    override suspend fun readData(
        serviceUUID: UUID,
        characteristicUUID: UUID
    ): BleNetworkMessage = launchBleOperationWithValidations(context) {
        checkGatt()
        bleManagerGattReadOperations.readData(
            serviceUUID,
            characteristicUUID,
            bluetoothGatt!!
        )
    }
    //endregion

    //region private methods
    private fun checkGatt() {
        if (bluetoothGatt == null) {
            throw SimpleBleClientException(BleError.CAMERA_NOT_CONNECTED)
        }
    }

//endregion
}
