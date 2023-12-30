package com.bortxapps.simplebleclient.scanner

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import java.util.UUID

internal class BleDeviceScannerFilterBuilder {

    fun buildFilters(serviceUuid: UUID?, deviceName: String?): List<ScanFilter> {
        val listFilters = mutableListOf<ScanFilter>()

        serviceUuid?.let { listFilters.add(buildFilterByService(it)) }
        deviceName?.let { listFilters.add(buildFilterByDeviceName(it)) }

        return listFilters
    }

    fun buildFilterByService(serviceUuid: UUID): ScanFilter {
        return ScanFilter.Builder().apply {
            setServiceUuid(ParcelUuid(serviceUuid))
        }.build()
    }

    fun buildFilterByDeviceName(deviceName: String): ScanFilter {
        return ScanFilter.Builder().apply {
            setDeviceName(deviceName)
        }.build()
    }
}
