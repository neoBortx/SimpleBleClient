package com.bortxapps.simplebleclient.api.contracts

public interface SimpleBleClient {

    /**
     * Client in charge of searching for BLE devices
     */
    public val deviceSeeker: SimpleBleClientDeviceSeeker

    /**
     * Client in charge of manage the connection with the BLE device
     */
    public val connection: SimpleBleClientConnection

    /**
     * Client in charge of reading characteristics from the BLE device
     */
    public val reader: SimpleBleClientReader

    /**
     * Client in charge of writing characteristics to the BLE device
     */
    public val writer: SimpleBleClientWriter

    /**
     * Client in charge of subscribing to BLE characteristics changes
     */
    public val subscription: SimpleBleClientSubscription
}
