package com.bortxapps.simplebleclient.api.contracts

import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

public interface SimpleBleClientSubscription {
    /**
     * Subscribes to changes of specified BLE characteristics.
     *
     * @param characteristicsUUid A list of UUIDs for the characteristics to monitor. If the list is empty,
     *                           all characteristics Noticeable and Indictable will be monitored.
     *
     * @return A Shared flow that emits [BleNetworkMessage] objects with the changes in the subscribed characteristics.
     *
     * @throws SimpleBleClientException Thrown when an error occurs during the BLE operation.
     */
    public suspend fun subscribeToCharacteristicChanges(characteristicsUUid: List<UUID>): SharedFlow<BleNetworkMessage>

    /**
     * Ge the shared flow that emits [BleNetworkMessage] objects with the changes in the subscribed characteristics.
     * You must call [subscribeToCharacteristicChanges] before calling this method or the flow won't emit any value.
     *
     * @return A Shared flow that emits [BleNetworkMessage] objects with the changes in the subscribed characteristics.
     */
    public fun subscribeToIncomeMessages(): SharedFlow<BleNetworkMessage>
}
