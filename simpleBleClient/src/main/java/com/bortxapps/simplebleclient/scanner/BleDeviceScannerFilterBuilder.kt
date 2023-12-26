package com.bortxapps.simplebleclient.scanner

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import java.util.UUID

internal class BleDeviceScannerFilterBuilder {

    fun buildFilters(serviceUuid: UUID): List<ScanFilter> {
        val scanFilterBuilder = ScanFilter.Builder()
        scanFilterBuilder.setServiceUuid(ParcelUuid(serviceUuid))
        return listOf(scanFilterBuilder.build())
    }
}