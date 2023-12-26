package com.bortxapps.simplebleclient.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.util.UUID

internal class BleManagerDeviceSearchOperations(
    private val bleScanner: BleDeviceScannerManager,
)  {

    private var searchingDevices = false
    private val detectedDevices = mutableListOf<BluetoothDevice>()
    fun getDevicesByService(serviceUUID: UUID): Flow<BluetoothDevice> {
        if (!searchingDevices) {
            searchingDevices = true
            detectedDevices.clear()
            return bleScanner.scanBleDevicesNearby(serviceUUID).onEach {
                detectedDevices += it
            }.onCompletion {
                searchingDevices = false
            }
        } else {
            Log.e("BleManager", "getDevicesByService already searching devices")
            throw SimpleBleClientException(BleError.ALREADY_SEARCHING_BLE_DEVICES)
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevicesByPrefix(context: Context, deviceNamePrefix: String): List<BluetoothDevice> =
        context.getSystemService(BluetoothManager::class.java)
            ?.adapter
            ?.bondedDevices
            ?.filter { it.name.startsWith(deviceNamePrefix) }
            .orEmpty()


    fun getDetectedDevices() = detectedDevices.toList()

    fun stopSearchDevices() = bleScanner.stopSearch()
}