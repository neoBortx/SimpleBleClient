package com.bortxapps.simplebleclient.api.contracts

public interface SimpleBleClient : SimpleBleClientDeviceSeeker,
    SimpleBleClientConnection,
    SimpleBleClientReader,
    SimpleBleClientWriter,
    SimpleBleClientSubscription
