package com.bortxapps.simplebleclient.exceptions

public class SimpleBleClientException(public val bleError: BleError, public val detail: String? = null) : Exception() {
    override val message: String
        get() = if (detail.isNullOrEmpty()) {
            bleError.toString()
        } else {
            detail
        }
}
