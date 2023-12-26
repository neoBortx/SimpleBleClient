package com.bortxapps.simplebleclient.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

class BleDeviceScannerCallbackBuilder {
    fun buildScanCallback(
        onResult: (BluetoothDevice) -> Unit,
        onFailure: () -> Unit
    ): ScanCallback {

        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                //super.onScanResult(callbackType, result)
                result?.device?.let {
                    onResult(it)
                } ?: onFailure()
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                // super.onBatchScanResults(results)
                results?.forEach { result ->
                    result.device?.let {
                        onResult(it)
                    } ?: onFailure()
                } ?: onFailure()
            }

            override fun onScanFailed(errorCode: Int) {
                //super.onScanFailed(errorCode)
                onFailure()
            }

        }
    }
}