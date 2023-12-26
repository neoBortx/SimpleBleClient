package com.bortxapps.simplebleclient.exceptions

class SimpleBleClientException(val bleError: BleError) : Exception() {
    override val message: String
        get() = bleError.toString()
}