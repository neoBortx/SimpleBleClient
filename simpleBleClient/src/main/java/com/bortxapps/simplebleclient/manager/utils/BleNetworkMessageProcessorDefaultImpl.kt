@file:Suppress("KDocUnresolvedReference")

package com.bortxapps.simplebleclient.manager.utils

import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import java.util.UUID

/**
 * Default implementation of [BleNetworkMessageProcessor]
 *
 * This message process is used when a BleNetworkMessageProcessor is not provided in the [BleConfiguration].
 *
 * It goings to return the message as is.
 */

public class BleNetworkMessageProcessorDefaultImpl : BleNetworkMessageProcessor {

    private var packet = byteArrayOf()

    private var characteristic: UUID? = null

    override fun isFullyReceived(): Boolean = packet.any()

    override fun processMessage(characteristic: UUID, data: ByteArray) {
        this.characteristic = characteristic
        packet = data
    }

    override fun getPacket(): BleNetworkMessage = BleNetworkMessage(characteristic, packet, !isFullyReceived())

    override fun clearData() {
        packet = byteArrayOf()
        characteristic = null
    }
}
