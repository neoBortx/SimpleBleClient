package com.bortxapps.simplebleclient.manager.utils

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.util.Log
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException

internal fun checkBluetoothEnabled(context: Context) {
    if (context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == false) {
        throw SimpleBleClientException(BleError.BLE_NOT_ENABLED)
    }
}

internal fun checkPermissionsApiCodeS(context: Context) =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PERMISSION_GRANTED
                    || context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PERMISSION_GRANTED)

internal fun checkPermissionsOldApi(context: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            && (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PERMISSION_GRANTED)

internal fun checkPermissions(context: Context) {
    if (checkPermissionsApiCodeS(context) || checkPermissionsOldApi(context)) {
        throw SimpleBleClientException(BleError.MISSING_BLE_PERMISSIONS)
    }
}

internal fun checkBleHardwareAvailable(context: Context) {
    if (!context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE)) {
        throw SimpleBleClientException(BleError.BLE_NOT_SUPPORTED)
    }
}

internal suspend fun <T> launchBleOperationWithValidations(context: Context, action: suspend () -> T): T {
    return try {
        checkBleHardwareAvailable(context)
        checkBluetoothEnabled(context)
        checkPermissions(context)
        action()
    } catch (ex: SimpleBleClientException) {
        Log.e("RepositoryBaseBle", "launchBleOperationWithValidations error -> $ex - ${ex.stackTraceToString()}")
        throw ex
    } catch (ex: Exception) {
        Log.e("RepositoryBaseBle", "launchBleOperationWithValidations error -> $ex - ${ex.stackTraceToString()}")
        throw SimpleBleClientException(BleError.OTHER)
    }
}