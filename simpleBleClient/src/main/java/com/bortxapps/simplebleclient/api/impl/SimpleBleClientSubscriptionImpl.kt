package com.bortxapps.simplebleclient.api.impl

import android.content.Context
import com.bortxapps.simplebleclient.api.contracts.SimpleBleClientSubscription
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleManagerGattConnectionOperations
import com.bortxapps.simplebleclient.manager.BleManagerGattSubscriptions
import com.bortxapps.simplebleclient.manager.utils.launchBleOperationWithValidations
import java.util.UUID

internal class SimpleBleClientSubscriptionImpl(
    private val context: Context,
    private val gattHolder: GattHolder,
    private val bleManagerGattSubscriptions: BleManagerGattSubscriptions,
    private val bleManagerGattConnectionOperations: BleManagerGattConnectionOperations
) : SimpleBleClientSubscription {

    override suspend fun subscribeToCharacteristicChanges(characteristicsUUid: List<UUID>): Boolean =
        launchBleOperationWithValidations(context) {
            gattHolder.checkGatt()
            if (bleManagerGattConnectionOperations.discoverServices(gattHolder.getGatt()!!)) {
                bleManagerGattSubscriptions.subscribeToNotifications(gattHolder.getGatt()!!, characteristicsUUid)
            } else {
                throw SimpleBleClientException(BleError.UNABLE_TO_SUBSCRIBE_TO_NOTIFICATIONS)
            }
        }

    override fun subscribeToIncomeMessages() = bleManagerGattSubscriptions.subscribeToIncomeMessages()
}
