package com.bortxapps.simplebleclient.data

@OptIn(ExperimentalUnsignedTypes::class)
data class BleNetworkMessage(val id: Int, val status: Int, val data: UByteArray, val missingData: Boolean = false)
