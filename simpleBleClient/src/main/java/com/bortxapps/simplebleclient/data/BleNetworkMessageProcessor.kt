package com.bortxapps.simplebleclient.data

import android.util.Log

@OptIn(ExperimentalUnsignedTypes::class)
class BleNetworkMessageProcessor {

    private var packet = ubyteArrayOf()
    private var bytesRemaining = 0

    companion object {
        private const val EXT_16_HEADER_SIZE = 3
        private const val EXT_13_HEADER_SIZE = 2
        private const val GENERAL_HEADER_SIZE = 1
        private const val BITS_8 = 8
        private const val BITS_5 = 5

    }

    private enum class Mask(val value: UByte) {
        Header(0b01100000U),
        Continuation(0b10000000U),
        GenLength(0b00011111U),
        Ext13Byte0(0b00011111U)
    }

    private enum class Header(val value: Byte) {
        GENERAL(0b00),
        EXT_13(0b01),
        EXT_16(0b10),
        RESERVED(0b11);

        companion object {
            private val valueMap: Map<Byte, Header> by lazy {
                entries.associateBy { it.value }
            }

            fun fromValue(value: Int) = valueMap.getValue(value.toByte())
        }
    }

    fun processSimpleMessage(data: UByteArray) {
        packet = data
        bytesRemaining = 0
    }

    fun processMessage(data: UByteArray) {
        if (isContinuationMessage(data)) {
            packet += data.drop(1).toUByteArray()
            bytesRemaining -= data.size - 1
        } else {
            processNewMessage(data)
        }
        Log.e("BleNetworkMessageProcessor", "Precessed data, remaining bytes -> $bytesRemaining")
    }

    private fun processNewMessage(data: UByteArray) {
        packet = ubyteArrayOf()
        var buff = data
        when (Header.fromValue((data.first() and Mask.Header.value).toInt() shr BITS_5)) {
            Header.GENERAL -> {
                bytesRemaining = data[0].and(Mask.GenLength.value).toInt()
                buff = buff.drop(GENERAL_HEADER_SIZE).toUByteArray()
            }

            Header.EXT_13 -> {
                bytesRemaining = ((data[0].and(Mask.Ext13Byte0.value).toLong() shl BITS_8) or data[1].toLong()).toInt()
                buff = buff.drop(EXT_13_HEADER_SIZE).toUByteArray()
            }

            Header.EXT_16 -> {
                bytesRemaining = ((data[1].toLong() shl BITS_8) or data[2].toLong()).toInt()
                buff = buff.drop(EXT_16_HEADER_SIZE).toUByteArray()
            }

            Header.RESERVED -> {
                throw NoSuchElementException("Reserved header")
            }
        }
        packet += if (buff.size >= bytesRemaining) buff.take(bytesRemaining) else buff
        if (buff.size >= bytesRemaining) bytesRemaining = 0 else bytesRemaining -= buff.size
    }

    fun clearData() {
        packet = ubyteArrayOf()
        bytesRemaining = 0
    }

    private fun isContinuationMessage(data: UByteArray) = data.firstOrNull()?.and(Mask.Continuation.value) == Mask.Continuation.value

    fun isReceived() = bytesRemaining == 0

    fun getPacket() = BleNetworkMessage(id(), status(), packet, !isReceived())

    private fun id() = packet[0].toInt()
    private fun status() = packet[1].toInt()
}