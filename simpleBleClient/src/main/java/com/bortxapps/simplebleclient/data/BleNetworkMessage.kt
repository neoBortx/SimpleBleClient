package com.bortxapps.simplebleclient.data


/**
 * Represents a message exchanged with a BLE (Bluetooth Low Energy) device.
 *
 * This data class encapsulates the details of a network message in BLE communications,
 * It is used to represent incoming messages.
 *
 * @property data A [UByteArray] containing the actual data of the message. In the context of BLE,
 *                this is typically the payload sent to or received from the BLE device.
 * @property missingData A boolean indicating whether there is missing data in the message. Defaults to `false`.
 *                       This can be used to signal incomplete or partial messages in scenarios when the data from  the BleNetworkMessageProcessor
 *                       and not all the data has been received.
 */

@OptIn(ExperimentalUnsignedTypes::class)
public data class BleNetworkMessage(val data: UByteArray, val missingData: Boolean = false)
