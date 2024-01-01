package com.bortxapps.simplebleclient.di

import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.BleManagerDeviceSearchOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattCallBacks
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattReadOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattSubscriptions
import com.bortxapps.simplebleclient.manager.BleManagerGattWriteOperations
import com.bortxapps.simplebleclient.providers.BleMessageProcessorProvider
import com.bortxapps.simplebleclient.providers.BuildVersionProvider
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerCallbackBuilder
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerFilterBuilder
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerManager
import com.bortxapps.simplebleclient.scanner.BleDeviceScannerSettingsBuilder
import kotlinx.coroutines.sync.Mutex

internal class BleLibraryContainer {
    private lateinit var blueToothScanner: BluetoothLeScanner
    private lateinit var bleConfiguration: BleConfiguration

    private lateinit var bleDeviceScannerManager: BleDeviceScannerManager
    private lateinit var bleManagerGattCallBacks: BleManagerGattCallBacks
    private lateinit var bleManagerDeviceSearchOperations: BleManagerDeviceSearchOperations
    private lateinit var bleManagerGattConnectionOperations: BleManagerGattConnectionOperations
    private lateinit var bleManagerGattSubscriptions: BleManagerGattSubscriptions
    private lateinit var bleManagerGattWriteOperations: BleManagerGattWriteOperations
    private lateinit var bleManagerGattReadOperations: BleManagerGattReadOperations

    private lateinit var gattMutex: Mutex
    private lateinit var buildVersionProvider: BuildVersionProvider
    private lateinit var bleMessageProcessorProvider: BleMessageProcessorProvider

    fun init(context: Context) {

        gattMutex = Mutex()

        bleConfiguration = BleConfiguration()

        bleMessageProcessorProvider = BleMessageProcessorProvider(bleConfiguration)

        buildVersionProvider = BuildVersionProvider()

        blueToothScanner = getBlueToothScannerFactory(context)

        bleDeviceScannerManager = BleDeviceScannerManager(
            blueToothScanner, BleDeviceScannerSettingsBuilder(), BleDeviceScannerFilterBuilder(), BleDeviceScannerCallbackBuilder(), bleConfiguration
        )

        bleManagerGattCallBacks = BleManagerGattCallBacks(bleMessageProcessorProvider)

        bleManagerDeviceSearchOperations = BleManagerDeviceSearchOperations(bleDeviceScannerManager)

        bleManagerGattConnectionOperations = BleManagerGattConnectionOperations(
            bleManagerDeviceSearchOperations, bleManagerGattCallBacks, gattMutex, bleConfiguration
        )

        bleManagerGattSubscriptions = BleManagerGattSubscriptions(
            bleManagerGattCallBacks, buildVersionProvider, gattMutex, bleConfiguration
        )

        bleManagerGattWriteOperations = BleManagerGattWriteOperations(
            bleManagerGattCallBacks, buildVersionProvider, gattMutex, bleConfiguration
        )

        bleManagerGattReadOperations = BleManagerGattReadOperations(
            bleManagerGattCallBacks, gattMutex, bleConfiguration
        )
    }

    fun getBleConfiguration(): BleConfiguration {

        if (::bleConfiguration.isInitialized.not()) {
            throw SimpleBleClientException(BleError.LIBRARY_NOT_INITIALIZED)
        }
        return bleConfiguration
    }

    fun getBleManagerDeviceSearchOperations(): BleManagerDeviceSearchOperations {

        if (::bleManagerDeviceSearchOperations.isInitialized.not()) {
            throw SimpleBleClientException(BleError.LIBRARY_NOT_INITIALIZED)
        }
        return bleManagerDeviceSearchOperations
    }

    fun getBleManagerGattConnectionOperations(): BleManagerGattConnectionOperations {

        if (::bleManagerGattConnectionOperations.isInitialized.not()) {
            throw SimpleBleClientException(BleError.LIBRARY_NOT_INITIALIZED)
        }
        return bleManagerGattConnectionOperations
    }

    fun getBleManagerGattSubscriptions(): BleManagerGattSubscriptions {

        if (::bleManagerGattSubscriptions.isInitialized.not()) {
            throw SimpleBleClientException(BleError.LIBRARY_NOT_INITIALIZED)
        }
        return bleManagerGattSubscriptions
    }

    fun getBleManagerGattWriteOperations(): BleManagerGattWriteOperations {

        if (::bleManagerGattWriteOperations.isInitialized.not()) {
            throw SimpleBleClientException(BleError.LIBRARY_NOT_INITIALIZED)
        }
        return bleManagerGattWriteOperations
    }

    fun getBleManagerGattReadOperations(): BleManagerGattReadOperations {

        if (::bleManagerGattReadOperations.isInitialized.not()) {
            throw SimpleBleClientException(BleError.LIBRARY_NOT_INITIALIZED)
        }
        return bleManagerGattReadOperations
    }

    fun getBleManagerGattCallBacks(): BleManagerGattCallBacks {

        if (::bleManagerGattCallBacks.isInitialized.not()) {
            throw SimpleBleClientException(BleError.LIBRARY_NOT_INITIALIZED)
        }
        return bleManagerGattCallBacks
    }


}
