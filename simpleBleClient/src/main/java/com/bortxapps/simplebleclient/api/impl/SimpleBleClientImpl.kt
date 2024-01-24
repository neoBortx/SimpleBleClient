package com.bortxapps.simplebleclient.api.impl

import com.bortxapps.simplebleclient.api.contracts.SimpleBleClient

internal class SimpleBleClientImpl(
    override val deviceSeeker: SimpleBleClientDeviceSeekerImpl,
    override val connection: SimpleBleClientConnectionImpl,
    override val reader: SimpleBleClientReaderImpl,
    override val writer: SimpleBleClientWriterImpl,
    override val subscription: SimpleBleClientSubscriptionImpl
) : SimpleBleClient
