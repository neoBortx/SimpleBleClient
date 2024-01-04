package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.util.Log
import com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor
import com.bortxapps.simplebleclient.api.data.BleNetworkMessage
import com.bortxapps.simplebleclient.providers.BleMessageProcessorProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import java.util.concurrent.CancellationException

internal class BleManagerGattCallBacks(bleConfiguration: BleConfiguration, bleMessageProcessorProvider: BleMessageProcessorProvider) :
    BluetoothGattCallback() {

    //region completions
    private var onConnectionEstablishedDeferred: CompletableDeferred<Boolean>? = null
    private var onDataReadDeferred: CompletableDeferred<BleNetworkMessage>? = null
    private var onDescriptorWriteDeferred: CompletableDeferred<Boolean>? = null
    private var onDisconnectedDeferred: CompletableDeferred<Boolean>? = null
    private var onServicesDiscoveredDeferred: CompletableDeferred<Boolean>? = null
    //endregion

    private val connectionStatus: MutableStateFlow<Int> = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    private val incomeMessages: MutableSharedFlow<BleNetworkMessage> = MutableSharedFlow(
        replay = bleConfiguration.messageBufferRetries,
        extraBufferCapacity = bleConfiguration.messageBufferSize
    )
    private val bleMessageProcessor: BleNetworkMessageProcessor = bleMessageProcessorProvider.getMessageProcessor()

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BleManagerGattCallBacks", "onConnectionStateChange: STATE_CONNECTED")
                    onConnectionEstablishedDeferred?.complete(true)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.e("BleManagerGattCallBacks", "onConnectionStateChange: STATE_DISCONNECTED")
                    onDisconnectedDeferred?.complete(true)
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    Log.e("BleManagerGattCallBacks", "onConnectionStateChange: STATE_CONNECTING")
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    Log.e("BleManagerGattCallBacks", "onConnectionStateChange: STATE_DISCONNECTING")
                }

                else -> {
                    Log.e("BleManagerGattCallBacks", "onConnectionStateChange: UNKNOWN")
                }
            }
            connectionStatus.tryEmit(newState)
        } else {
            Log.e("BleManagerGattCallBacks", "onConnectionStateChange: FAIL - status $status")
            onConnectionEstablishedDeferred?.cancel(
                CancellationException(
                    "Gatt Connection operation failed -> gat code $status"
                )
            )
            onDisconnectedDeferred?.cancel(
                CancellationException(
                    "Gatt Disconnection Operation failed -> gat code $status"
                )
            )
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            processCharacteristic(characteristic.uuid, value)
        } else {
            Log.e("BleManagerGattCallBacks", "onCharacteristicRead: ${characteristic.uuid} FAIL - status $status")
            bleMessageProcessor.clearData()
            onDataReadDeferred?.cancel(CancellationException("Gatt Read operation failed -> gat code $status"))
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        processCharacteristic(characteristic.uuid, value)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onServicesDiscoveredDeferred?.complete(true)
        } else {
            Log.e("BleManagerGattCallBacks", "onServicesDiscovered: FAIL - status $status")
            onServicesDiscoveredDeferred?.cancel(
                CancellationException("Gatt Service discover operation failed -> gat code $status")
            )
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onDescriptorWriteDeferred?.complete(true)
        } else {
            Log.e("BleManagerGattCallBacks", "onDescriptorWrite:  FAIL - status $status")
            onDescriptorWriteDeferred?.cancel(
                CancellationException("Gatt write descriptor operation failed -> gat code $status")
            )
        }
    }

    private fun processCharacteristic(uuid: UUID, value: ByteArray) {
        bleMessageProcessor.processMessage(uuid, value)
        if (bleMessageProcessor.isFullyReceived()) {
            incomeMessages.tryEmit(BleNetworkMessage(uuid, value))
            onDataReadDeferred?.complete(bleMessageProcessor.getPacket())
        }
    }

    internal fun reset() {
        onConnectionEstablishedDeferred?.cancel()
        onConnectionEstablishedDeferred = null
        onDataReadDeferred?.cancel()
        onDataReadDeferred = null
        onDescriptorWriteDeferred?.cancel()
        onDescriptorWriteDeferred = null
        onDisconnectedDeferred?.cancel()
        onDisconnectedDeferred = null
        onServicesDiscoveredDeferred?.cancel()
        onServicesDiscoveredDeferred = null
    }

    internal fun subscribeToConnectionStatusChanges() = connectionStatus

    internal fun subscribeToIncomeMessages() = incomeMessages

    //region init deferred operations
    internal fun initDeferredConnectOperation() {
        onConnectionEstablishedDeferred = CompletableDeferred()
    }

    internal fun initDeferredReadOperation() {
        onDataReadDeferred = CompletableDeferred()
    }

    internal fun initDeferredWriteDescriptorOperation() {
        onDescriptorWriteDeferred = CompletableDeferred()
    }

    internal fun initDeferredDisconnectOperation() {
        onDisconnectedDeferred = CompletableDeferred()
    }

    internal fun initDeferredDiscoverServicesOperation() {
        onServicesDiscoveredDeferred = CompletableDeferred()
    }
    //endregion

    //region wait deferred
    internal suspend fun waitForConnectionEstablished() = onConnectionEstablishedDeferred?.await()
        ?: throw UninitializedPropertyAccessException(
            "onConnectionEstablishedDeferred is null, you must call initConnectOperation() first"
        )

    internal suspend fun waitForDataRead() = onDataReadDeferred?.await()
        ?: throw UninitializedPropertyAccessException(
            "onDataReadDeferred is null, you must call initReadOperation() first"
        )

    internal suspend fun waitForWrittenDescriptor() = onDescriptorWriteDeferred?.await()
        ?: throw UninitializedPropertyAccessException(
            "onDescriptorWriteDeferred is null, you must call initWriteDescriptorOperation() first"
        )

    internal suspend fun waitForDisconnected() = onDisconnectedDeferred?.await()
        ?: throw UninitializedPropertyAccessException(
            "onDisconnectedDeferred is null, you must call initDisconnectOperation() first"
        )

    internal suspend fun waitForServicesDiscovered() = onServicesDiscoveredDeferred?.await()
        ?: throw UninitializedPropertyAccessException(
            "onServicesDiscoveredDeferred is null, you must call initDiscoverServicesOperation() first"
        )

    //endregion
}
