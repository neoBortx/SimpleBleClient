package com.bortxapps.simplebleclient.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

internal class BleDeviceScannerCallbackBuilder {
    fun buildScanCallback(
        onResult: (BluetoothDevice) -> Unit,
        onFailure: () -> Unit
    ): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let {
                    onResult(it)
                } ?: onFailure()
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { result ->
                    result.device?.let {
                        onResult(it)
                    } ?: onFailure()
                } ?: onFailure()
            }

            override fun onScanFailed(errorCode: Int) {
                onFailure()
            }
        }
    }
}
