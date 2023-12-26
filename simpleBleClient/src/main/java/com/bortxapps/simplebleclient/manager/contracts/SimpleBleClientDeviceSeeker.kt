package com.bortxapps.simplebleclient.manager.contracts

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SimpleBleClientDeviceSeeker {
    suspend fun getDevicesByService(serviceUUID: UUID): Flow<BluetoothDevice>
    suspend fun getPairedDevicesByPrefix(context: Context, deviceNamePrefix: String): List<BluetoothDevice>
    suspend fun stopSearchDevices()
}