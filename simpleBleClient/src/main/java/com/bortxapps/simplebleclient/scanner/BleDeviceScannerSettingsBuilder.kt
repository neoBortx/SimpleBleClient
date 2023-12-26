package com.bortxapps.simplebleclient.scanner

import android.bluetooth.le.ScanSettings

internal class BleDeviceScannerSettingsBuilder {

    fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()
    }
}