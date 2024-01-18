package com.bortxapps.simplebleclient.exceptions

import androidx.annotation.Keep

@Keep
public class SimpleBleClientException(public val bleError: BleError) : Exception() {
    override val message: String
        get() = bleError.toString()
}
