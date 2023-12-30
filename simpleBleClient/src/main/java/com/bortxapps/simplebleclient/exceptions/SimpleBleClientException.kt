package com.bortxapps.simplebleclient.exceptions

public class SimpleBleClientException(public val bleError: BleError) : Exception() {
    override val message: String
        get() = bleError.toString()
}
