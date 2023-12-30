package com.bortxapps.simplebleclient.data

@OptIn(ExperimentalUnsignedTypes::class)
public data class BleNetworkMessage(val id: Int, val status: Int, val data: UByteArray, val missingData: Boolean = false)
