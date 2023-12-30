package com.bortxapps.simplebleclient.manager.contracts

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.UUID

public interface SimpleBleClientDeviceSeeker {
    public suspend fun getDevicesByService(serviceUUID: UUID): Flow<BluetoothDevice>
    public suspend fun getPairedDevicesByPrefix(context: Context, deviceNamePrefix: String): List<BluetoothDevice>
    public suspend fun stopSearchDevices()
}
