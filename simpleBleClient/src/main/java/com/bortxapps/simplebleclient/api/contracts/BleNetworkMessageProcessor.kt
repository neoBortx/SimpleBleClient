package com.bortxapps.simplebleclient.api.contracts

import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import java.util.UUID


/**
 * Depending on the protocol used by the device, the message received from the device can be fragmented or may require a special treatment.
 *
 * BLE operations are asynchronous but you only can perform one in a time. When you're expecting a fragmented messages, you need to wait
 * for the last message to be received before sending the next operation. Because this is not straightforward, this interface is used to
 * encapsulate the logic to handle this kind of messages in lower layers.
 *
 * If this Interface doesn't match with your needs, you can implement your own and pass it to the SimpleBleClientBuilder.
 */
public interface BleNetworkMessageProcessor {

    /**
     * Indicates if all fragments of the message have been received.
     */
    public fun isFullyReceived(): Boolean


    /**
     * Usually there are two different cases:
     *  - The message is not fragmented: In this case, the message is returned as is.
     *  - The message is fragmented: In this case, this method should return the data of the message without the fragmentation control data.
     *  The way to determine if it's a fragmented message or not is protocol dependent.
     *
     * The library is in charge of compose the fragmented message with the data provided by this method. When the last fragment is recieved the method
     * isFullyReceived should return true.
     */
    public fun processMessage(characteristic: UUID, data: ByteArray)

    /**
     * Returns the message received and processed from the device.
     *
     * @return [BleNetworkMessage] representing the result of the write operation.
     * This result contains the characteristic where the message was received, the data and a flag indicating if the message is fully received.
     *  (because in fragmented messages, you can call getPacket() before the message is fully received)
     */
    public fun getPacket(): BleNetworkMessage

    /**
     * Clears the data stored in the processor. called in error cases.
     */
    public fun clearData()

}