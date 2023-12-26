package com.bortxapps.simplebleclient.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BleNetworkMessageProcessorTest {

    private lateinit var messageProcessor: BleNetworkMessageProcessor

    @Before
    fun setUp() {
        messageProcessor = BleNetworkMessageProcessor()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testProcessGeneralMessage_messageParsed_expectValues() {
        val header: UByte = 0x04U
        val id: UByte = 0x01U
        val status: UByte = 0x05U
        val data = ubyteArrayOf (0x10U, 0x20U)

        val testData = ubyteArrayOf(header,id, status, data[0], data[1]) // Header with payload length 0
        messageProcessor.processMessage(testData)

        val res = messageProcessor.getPacket()
        assertEquals(id.toInt(), res.id)
        assertEquals(status.toInt(), res.status)
        assertEquals(id, res.data[0])
        assertEquals(status, res.data[1])
        assertEquals(data[0], res.data[2])
        assertEquals(data[1], res.data[3])

        //test that that the last byte is not added to the packet
        assertEquals(4, res.data.size)
        assertTrue(messageProcessor.isReceived())
        assertFalse(res.missingData)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testProcessGeneralMessage_extendedMessage_TwoMessageParsed_expectValues() {
        val headerContinuation: UByte = 0x80U
        val header: UByte = 0x07U
        val id: UByte = 0x01U
        val status: UByte = 0x05U
        val data = ubyteArrayOf (0x10U, 0x20U)
        val dataContinuation = ubyteArrayOf (0x30U, 0x40U, 0x50U)

        val testDataFirst = ubyteArrayOf(header,id, status, data[0], data[1])
        val testDataSecond = ubyteArrayOf(headerContinuation,dataContinuation[0],dataContinuation[1], dataContinuation[2])
        messageProcessor.processMessage(testDataFirst)

        val res = messageProcessor.getPacket()
        assertFalse(messageProcessor.isReceived())
        assertTrue(res.missingData)

        messageProcessor.processMessage(testDataSecond)
        val resContinuation = messageProcessor.getPacket()
        assertTrue(messageProcessor.isReceived())
        assertFalse(resContinuation.missingData)

        assertEquals(id.toInt(), resContinuation.id)
        assertEquals(status.toInt(), resContinuation.status)
        assertEquals(id, resContinuation.data[0])
        assertEquals(status, resContinuation.data[1])
        assertEquals(data[0], resContinuation.data[2])
        assertEquals(data[1], resContinuation.data[3])
        assertEquals(dataContinuation[0], resContinuation.data[4])
        assertEquals(dataContinuation[1], resContinuation.data[5])
        assertEquals(dataContinuation[2], resContinuation.data[6])

        assertEquals(7, resContinuation.data.size)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testProcessGeneralMessage_messageTooLong_expectOnlyGetLengthValues() {
        val header: UByte = 0x03U
        val id: UByte = 0x01U
        val status: UByte = 0x05U
        val data = ubyteArrayOf (0x10U, 0x20U)

        val testData = ubyteArrayOf(header,id, status, data[0], data[1]) // Header with payload length 0
        messageProcessor.processMessage(testData)

        val res = messageProcessor.getPacket()
        assertEquals(id.toInt(), res.id)
        assertEquals(status.toInt(), res.status)
        assertEquals(id, res.data[0])
        assertEquals(status, res.data[1])
        assertEquals(data[0], res.data[2])

        //test that that the last byte is not added to the packet
        assertEquals(3, res.data.size)
        assertFalse(res.data.contains(0x20U))
        assertTrue(messageProcessor.isReceived())
        assertFalse(res.missingData)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testProcessGeneralMessage_messageToShort_expectOnlyGetGivenValues() {
        val header: UByte = 0x05U
        val id: UByte = 0x01U
        val status: UByte = 0x05U
        val data = ubyteArrayOf (0x10U, 0x20U)

        val testData = ubyteArrayOf(header,id, status, data[0], data[1]) // Header with payload length 0
        messageProcessor.processMessage(testData)

        val res = messageProcessor.getPacket()
        assertEquals(id.toInt(), res.id)
        assertEquals(status.toInt(), res.status)
        assertEquals(id, res.data[0])
        assertEquals(status, res.data[1])
        assertEquals(data[0], res.data[2])
        assertEquals(data[1], res.data[3])

        //all data is stored but the packet is not complete
        assertEquals(4, res.data.size)
        assertFalse(messageProcessor.isReceived())
        assertTrue(res.missingData)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testProcessExtended13_ExpectMessageParsed() {
        //EXT_13 header
        val header: UByte = 0x20U
        //Length goes shared in the first and second bytes
        val header1: UByte = 0x04U
        val id: UByte = 0x15U
        val status: UByte = 0x11U
        val value = ubyteArrayOf (0x10U, 0x20U)
        // Example of a GENERAL header message
        val testData = ubyteArrayOf(header,header1, id, status, value[0], value[1]) // Header with payload length 0
        messageProcessor.processMessage(testData)

        val res = messageProcessor.getPacket()
        assertEquals(id.toInt(), res.id)
        assertEquals(status.toInt(), res.status)
        assertEquals(value[0], res.data[2])
        assertEquals(value[1], res.data[3])
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testProcessExtended16_ExpectMessageParsed() {
        //EXT_13 header
        val header: UByte = 0x50U
        //Length goes in the second byte
        val header1: UByte = 0x00U
        //Length goes in the third byte
        val header2: UByte = 0x04U

        val id: UByte = 0x15U
        val status: UByte = 0x11U
        val value = ubyteArrayOf (0x10U, 0x20U)
        // Example of a GENERAL header message
        val testData = ubyteArrayOf(header,header1,header2, id, status, value[0], value[1]) // Header with payload length 0
        messageProcessor.processMessage(testData)

        val res = messageProcessor.getPacket()
        assertEquals(id.toInt(), res.id)
        assertEquals(status.toInt(), res.status)
        assertEquals(value[0], res.data[2])
        assertEquals(value[1], res.data[3])
    }

    @Test
    fun testProcessContinuationMessage() {
        // Example of processing a continuation message
        // Add your test logic here
    }

    @Test
    fun testClearData() {
        // Add test logic for clearData method
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testReservedHeaderException() {
        val reservedHeaderData = ubyteArrayOf(0x77U, 0x01U, 0x02U) // Reserved header
        assertThrows(NoSuchElementException::class.java) {
            messageProcessor.processMessage(reservedHeaderData)
        }
    }

    // Add more tests for different scenarios and edge cases
}
