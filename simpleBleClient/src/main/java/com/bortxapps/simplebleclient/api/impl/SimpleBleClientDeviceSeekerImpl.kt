package com.bortxapps.simplebleclient.api.impl

import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClientDeviceSeeker
import com.bortxapps.simplebleclient.manager.BleManagerDeviceSearchOperations
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidations
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidationsSync
import java.util.UUID

internal class SimpleBleClientDeviceSeekerImpl(
    private val context: Context,
    private val bleManagerDeviceSearchOperations: BleManagerDeviceSearchOperations
) : SimpleBleClientDeviceSeeker {
    override fun getDevicesNearby(serviceUUID: UUID?, deviceName: String?) = launchBleOperationWithValidationsSync(context) {
        bleManagerDeviceSearchOperations.getDevicesNearBy(serviceUUID, deviceName)
    }

    override suspend fun getPairedDevices(context: Context) =
        launchBleOperationWithValidations(context) {
            bleManagerDeviceSearchOperations.getPairedDevices(context)
        }

    override suspend fun stopSearchDevices() =
        launchBleOperationWithValidations(context) {
            bleManagerDeviceSearchOperations.stopSearchDevices()
        }
}
