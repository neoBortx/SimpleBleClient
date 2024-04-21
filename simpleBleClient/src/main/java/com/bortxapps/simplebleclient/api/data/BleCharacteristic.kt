package com.bortxapps.simplebleclient.api.data

import java.util.UUID

/**
 * Represents a BLE characteristic IDs.
 *
 * @param serviceUUID The UUID of the service that contains the characteristic.
 * @param characteristicUUID The UUID of the characteristic.
 */
public data class BleCharacteristic(public val serviceUUID: UUID, public val characteristicUUID: UUID)