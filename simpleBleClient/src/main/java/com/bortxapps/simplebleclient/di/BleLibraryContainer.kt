package com.bortxapps.simplebleclient.di

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.ContextCompat
import com.bortxapps.simplebleclient.data.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.BleManagerDeviceSearchOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattCallBacks
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattReadOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattSubscriptions
import com.bortxapps.simplebleclient.manager.BleManagerGattWriteOperations
import com.bortxapps.simplebleclient.providers.BuildVersionProvider
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerCallbackBuilder
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerFilterBuilder
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerManager
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerSettingsBuilder
import kotlinx.coroutines.sync.Mutex

internal class BleLibraryContainer(context: Context) {
    // ble
    private val blueToothScanner =
        ContextCompat.getSystemService(context.applicationContext, BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
            ?: throw SimpleBleClientException(BleError.UNABLE_INITIALIZE_CONTROLLER)

    val bleConfiguration = BleConfiguration()

    private val bleDeviceScannerFilterBuilder = BleDeviceScannerFilterBuilder()
    private val bleDeviceScannerSettingsBuilder = BleDeviceScannerSettingsBuilder()
    private val bleDeviceScannerCallbackBuilder = BleDeviceScannerCallbackBuilder()
    private val bleDeviceScannerManager = BleDeviceScannerManager(
        blueToothScanner,
        bleDeviceScannerSettingsBuilder,
        bleDeviceScannerFilterBuilder,
        bleDeviceScannerCallbackBuilder,
        bleConfiguration
    )

    private val bleNetworkMessageProcessor = BleNetworkMessageProcessor()
    private val gattMutex = Mutex()
    private val buildVersionProvider = BuildVersionProvider()

    val bleManagerGattCallBacks = BleManagerGattCallBacks(bleNetworkMessageProcessor)
    val bleManagerDeviceSearchOperations = BleManagerDeviceSearchOperations(bleDeviceScannerManager)
    val bleManagerGattConnectionOperations =
        BleManagerGattConnectionOperations(bleManagerDeviceSearchOperations, bleManagerGattCallBacks, gattMutex, bleConfiguration)
    val bleManagerGattSubscriptions = BleManagerGattSubscriptions(bleManagerGattCallBacks, buildVersionProvider, gattMutex, bleConfiguration)
    val bleManagerGattWriteOperations =
        BleManagerGattWriteOperations(bleManagerGattCallBacks, buildVersionProvider, gattMutex, bleConfiguration)
    val bleManagerGattReadOperations = BleManagerGattReadOperations(bleManagerGattCallBacks, gattMutex, bleConfiguration)
}
