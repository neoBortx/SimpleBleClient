package com.bortxapps.simplebleclient.manager.utils

import android.bluetooth.BluetoothGattDescriptor

internal fun getEnableIndicationValue() = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
internal fun getEnableNotificationValue() = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE