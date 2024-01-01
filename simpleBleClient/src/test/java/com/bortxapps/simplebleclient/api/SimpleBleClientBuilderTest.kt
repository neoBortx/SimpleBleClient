package com.bortxapps.simplebleclient.api

import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.di.getBlueToothScannerFactory
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

internal class SimpleBleClientBuilderTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(::getBlueToothScannerFactory)
        every { context.applicationContext } returns context
        every { getBlueToothScannerFactory(context) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `build SimpleBleClientBuilder without values should return a default SimpleBleClient instance`() {
        val builder = SimpleBleClientBuilder()

        assertNotNull(builder.build(context))

        assertEquals(7000, builder.bleLibraryContainer.getBleConfiguration().operationTimeoutMillis)
        assertEquals(10000, builder.bleLibraryContainer.getBleConfiguration().scanPeriodMillis)
        assertNull(builder.bleLibraryContainer.getBleConfiguration().messageProcessor)
    }

    @Test
    fun `build SimpleBleClientBuilder with values should return a SimpleBleClient instance with the values`() {
        val customTimeout = 10000L
        val customScanPeriod = 20000L
        val customMessageProcessor = mockk<BleNetworkMessageProcessor>()

        val builder = SimpleBleClientBuilder()

        assertNotNull(builder
            .setOperationTimeOutMillis(customTimeout)
            .setScanPeriodMillis(customScanPeriod)
            .setMessageProcessor(customMessageProcessor)
            .build(context))

        assertEquals(customTimeout, builder.bleLibraryContainer.getBleConfiguration().operationTimeoutMillis)
        assertEquals(customScanPeriod, builder.bleLibraryContainer.getBleConfiguration().scanPeriodMillis)
        assertEquals(customMessageProcessor, builder.bleLibraryContainer.getBleConfiguration().messageProcessor)
    }


}