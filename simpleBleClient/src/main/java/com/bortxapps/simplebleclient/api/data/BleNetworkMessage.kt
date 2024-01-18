package com.bortxapps.simplebleclient.api.data

import androidx.annotation.Keep
import java.util.UUID

/**
 * Represents a message exchanged with a BLE (Bluetooth Low Energy) device.
 *
 * This data class encapsulates the details of a network message in BLE communications,
 * It is used to represent incoming messages.
 *
 * @property characteristicsId The UUID of the BLE characteristics that originated the message.
 * @property data A [ByteArray] containing the actual data of the message. In the context of BLE,
 *                this is typically the payload sent to or received from the BLE device.
 * @property missingData A boolean indicating whether there is missing data in the message. Defaults to `false`.
 *                       This can be used to signal incomplete or partial messages in scenarios when the data from  the BleNetworkMessageProcessor
 *                       and not all the data has been received.
 */

@Keep
public data class BleNetworkMessage(val characteristicsId: UUID?, val data: ByteArray, val missingData: Boolean = false) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleNetworkMessage

        if (characteristicsId != other.characteristicsId) return false
        if (!data.contentEquals(other.data)) return false
        return missingData == other.missingData
    }

    override fun hashCode(): Int {
        var result = characteristicsId.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + missingData.hashCode()
        return result
    }
}
