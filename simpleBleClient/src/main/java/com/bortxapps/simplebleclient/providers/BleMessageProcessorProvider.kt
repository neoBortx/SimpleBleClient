package com.bortxapps.simplebleclient.providers

import com.bortxapps.simplebleclient.manager.BleConfiguration
import com.bortxapps.simplebleclient.manager.utils.BleNetworkMessageProcessorDefaultImpl

internal class BleMessageProcessorProvider(val bleConfiguration: BleConfiguration) {

    fun getMessageProcessor() = bleConfiguration.messageProcessor ?: BleNetworkMessageProcessorDefaultImpl()
}
