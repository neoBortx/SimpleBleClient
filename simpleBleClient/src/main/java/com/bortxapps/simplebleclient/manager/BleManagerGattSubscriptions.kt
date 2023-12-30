package com.bortxapps.simplebleclient.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.utils.BleManagerGattOperationBase
import com.bortxapps.simplebleclient.providers.BuildVersionProvider
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.UUID

internal class BleManagerGattSubscriptions(
    private val bleManagerGattCallBacks: BleManagerGattCallBacks,
    private val buildVersionProvider: BuildVersionProvider,
    gattMutex: Mutex,
    bleConfiguration: BleConfiguration
) : BleManagerGattOperationBase(gattMutex, bleConfiguration) {

    companion object {
        private const val BLE_DESCRIPTION_BASE_UUID = "00002902-0000-1000-8000-00805F9B34FB"
    }

    suspend fun subscribeToNotifications(bluetoothGatt: BluetoothGatt, characteristicsUUid: List<UUID>) {
        getNotifiableCharacteristics(bluetoothGatt, characteristicsUUid).forEach { characteristic ->
            withContext(IO) {
                launchGattOperation {
                    bleManagerGattCallBacks.initWriteDescriptorOperation()
                    if (writeDescriptor(characteristic, getDescriptionValueToSubscribe(characteristic), bluetoothGatt)) {
                        launchDeferredOperation {
                            bleManagerGattCallBacks.waitForWrittenDescriptor()
                        }
                    } else {
                        throw SimpleBleClientException(BleError.UNABLE_TO_SUBSCRIBE_TO_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    private fun getNotifiableCharacteristics(bluetoothGatt: BluetoothGatt, characteristicsUUid: List<UUID>): List<BluetoothGattCharacteristic> =
        bluetoothGatt.services
            ?.map { it.characteristics }
            ?.flatten()
            ?.filter { filterCharacteristicForSubscriptions(it.properties) }
            ?.filter { filterResponseCharacteristic(it, characteristicsUUid) }
            .orEmpty()

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission", "NewApi")
    private fun writeDescriptor(characteristic: BluetoothGattCharacteristic, descriptorValue: ByteArray, bluetoothGatt: BluetoothGatt): Boolean =
        try {
            bluetoothGatt.setCharacteristicNotification(characteristic, true)
            characteristic.getDescriptor(UUID.fromString(BLE_DESCRIPTION_BASE_UUID))?.let { descriptor ->
                return if (buildVersionProvider.getSdkVersion() >= Build.VERSION_CODES.TIRAMISU) {
                    bluetoothGatt.writeDescriptor(descriptor, descriptorValue) == BluetoothStatusCodes.SUCCESS
                } else {
                    bluetoothGatt.writeDescriptor(descriptor.apply { value = descriptorValue })
                }
            } ?: false
        } catch (ex: Exception) {
            Log.e("BleManager", "writeDescriptor ${ex.message} ${ex.stackTraceToString()}")
            throw SimpleBleClientException(BleError.UNABLE_TO_SUBSCRIBE_TO_NOTIFICATIONS)
        }

    private fun filterCharacteristicForSubscriptions(properties: Int) =
        filterNotifiableCharacteristic(properties) || filterIndicatableCharacteristic(properties)

    private fun filterNotifiableCharacteristic(properties: Int) =
        properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

    private fun filterIndicatableCharacteristic(properties: Int) =
        properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

    private fun filterResponseCharacteristic(characteristic: BluetoothGattCharacteristic, characteristicsUUid: List<UUID>): Boolean {
        return characteristicsUUid.any { it == characteristic.uuid }
    }

    private fun getDescriptionValueToSubscribe(characteristic: BluetoothGattCharacteristic): ByteArray {
        return if (filterIndicatableCharacteristic(characteristic.properties)) {
            getEnableIndicationValue()
        } else {
            getEnableNotificationValue()
        }
    }

    private fun getEnableIndicationValue() = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
    private fun getEnableNotificationValue() = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
}
