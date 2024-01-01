package com.bortxapps.simplebleclient.provider

import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.utils.BleNetworkMessageProcessorDefaultImpl
import com.bortxapps.simplebleclient.providers.BleMessageProcessorProvider
import io.mockk.mockk
import org.junit.Test

public class BleMessageProcessorProviderTest {

    @Test
    public fun `getMessageProcessor should return a BleNetworkMessageProcessorDefaultImpl when not processor is not provided in the BleConfiguration`() {
        // Given
        val bleConfiguration = BleConfiguration()
        val bleMessageProcessorProvider = BleMessageProcessorProvider(bleConfiguration)

        // When
        val messageProcessor = bleMessageProcessorProvider.getMessageProcessor()

        // Then
        assert(messageProcessor is BleNetworkMessageProcessorDefaultImpl)
    }

    @Test
    public fun `getMessageProcessor should return the BleNetworkMessageProcessor provided in the BleConfiguration`() {
        // Given
        val bleConfiguration = BleConfiguration()
        val bleNetworkMessageProcessor = mockk<BleNetworkMessageProcessor>()

        bleConfiguration.messageProcessor = bleNetworkMessageProcessor
        val bleMessageProcessorProvider = BleMessageProcessorProvider(bleConfiguration)

        // When
        val messageProcessor = bleMessageProcessorProvider.getMessageProcessor()

        // Then
        assert(messageProcessor == bleNetworkMessageProcessor)
    }

}