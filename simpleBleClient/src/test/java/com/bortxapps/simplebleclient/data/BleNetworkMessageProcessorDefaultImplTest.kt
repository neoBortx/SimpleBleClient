package com.bortxapps.simplebleclient.data

import com.bortxapps.simplebleclient.manager.utils.BleNetworkMessageProcessorDefaultImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

internal class BleNetworkMessageProcessorDefaultImplTest {

    private lateinit var messageProcessor: BleNetworkMessageProcessorDefaultImpl
    private val characteristicUUID = UUID.randomUUID()
    val testData = byteArrayOf(0x04, 0x01, 0x05, 0x10, 0x20)

    @Before
    fun setUp() {
        messageProcessor = BleNetworkMessageProcessorDefaultImpl()
    }

    @Test
    fun `test processMessage should fill internal buffer and store characteristics`() {
        messageProcessor.processMessage(characteristicUUID, testData)

        val res = messageProcessor.getPacket()

        assertTrue(messageProcessor.isFullyReceived())
        assertEquals(characteristicUUID, res.characteristicsId)
        assertEquals(testData, res.data)
        assertFalse(res.missingData)
    }

    @Test
    fun `call clearData should erase all information`() {
        messageProcessor.processMessage(characteristicUUID, testData)
        messageProcessor.clearData()

        val res = messageProcessor.getPacket()
        assertTrue(res.missingData)
        assertTrue(res.data.isEmpty())
        assertNull(res.characteristicsId)
    }

    // Add more tests for different scenarios and edge cases
}
