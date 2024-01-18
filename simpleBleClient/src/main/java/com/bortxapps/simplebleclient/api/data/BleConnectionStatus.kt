package com.bortxapps.simplebleclient.api.data

import androidx.annotation.Keep

@Keep
public enum class BleConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    DISCONNECTING,
    UNKNOWN
}
