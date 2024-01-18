package com.bortxapps.simplebleclient.api.contracts

import androidx.annotation.Keep

@Keep
public interface SimpleBleClient :
    SimpleBleClientDeviceSeeker,
    SimpleBleClientConnection,
    SimpleBleClientReader,
    SimpleBleClientWriter,
    SimpleBleClientSubscription
