package com.bortxapps.simplebleclient.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.util.Log
import com.bortxapps.simplebleclient.data.BleNetworkMessage
import com.bortxapps.simplebleclient.data.BleNetworkMessageProcessor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CancellationException

@OptIn(ExperimentalUnsignedTypes::class)
internal class BleManagerGattCallBacks(private val bleNetworkMessageProcessor: BleNetworkMessageProcessor) : BluetoothGattCallback() {

    //region completions
    private var onConnectionEstablishedDeferred: CompletableDeferred<Boolean>? = null
    private var onDataReadDeferred: CompletableDeferred<BleNetworkMessage>? = null
    private var onDescriptorWriteDeferred: CompletableDeferred<Boolean>? = null
    private var onDisconnectedDeferred: CompletableDeferred<Boolean>? = null
    private var onServicesDiscoveredDeferred: CompletableDeferred<Boolean>? = null
    //endregion

    private var connectionStatus: MutableStateFlow<Int> = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)

    private var readComplexResponse: Boolean = false

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
            onConnectionEstablishedDeferred?.cancel(CancellationException("Gatt Connection operation failed -> gat code $status"))
            onDisconnectedDeferred?.cancel(CancellationException("Gatt Disconnection Operation failed -> gat code $status"))
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            processCharacteristic(value.toUByteArray())
        } else {
            Log.e("BleManagerGattCallBacks", "onCharacteristicRead: ${characteristic.uuid} FAIL - status $status")
            bleNetworkMessageProcessor.clearData()
            onDataReadDeferred?.cancel(CancellationException("Gatt Read operation failed -> gat code $status"))
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        processCharacteristic(value.toUByteArray())
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onServicesDiscoveredDeferred?.complete(true)
        } else {
            Log.e("BleManagerGattCallBacks", "onServicesDiscovered: FAIL - status $status")
            onServicesDiscoveredDeferred?.cancel(CancellationException("Gatt Service discover operation failed -> gat code $status"))
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onDescriptorWriteDeferred?.complete(true)
        } else {
            Log.e("BleManagerGattCallBacks", "onDescriptorWrite:  FAIL - status $status")
            onDescriptorWriteDeferred?.cancel(CancellationException("Gatt write descriptor operation failed -> gat code $status"))
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun processCharacteristic(value: UByteArray) {
        if (readComplexResponse) {
            bleNetworkMessageProcessor.processMessage(value)
        } else {
            bleNetworkMessageProcessor.processSimpleMessage(value)
        }
        if (bleNetworkMessageProcessor.isReceived()) {
            onDataReadDeferred?.complete(bleNetworkMessageProcessor.getPacket())
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

    //region init completions
    internal fun initConnectOperation() {
        onConnectionEstablishedDeferred = CompletableDeferred()
    }

    internal fun initReadOperation(readComplexResponse: Boolean) {
        this.readComplexResponse = readComplexResponse
        onDataReadDeferred = CompletableDeferred()
    }

    internal fun initWriteDescriptorOperation() {
        onDescriptorWriteDeferred = CompletableDeferred()
    }

    internal fun initDisconnectOperation() {
        onDisconnectedDeferred = CompletableDeferred()
    }

    internal fun initDiscoverServicesOperation() {
        onServicesDiscoveredDeferred = CompletableDeferred()
    }
    //endregion

    //region wait completions
    internal suspend fun waitForConnectionEstablished() = onConnectionEstablishedDeferred?.await()
        ?: throw UninitializedPropertyAccessException("onConnectionEstablishedDeferred is null, you must call initConnectOperation() first")

    internal suspend fun waitForDataRead() = onDataReadDeferred?.await()
        ?: throw UninitializedPropertyAccessException("onDataReadDeferred is null, you must call initReadOperation() first")

    internal suspend fun waitForWrittenDescriptor() = onDescriptorWriteDeferred?.await()
        ?: throw UninitializedPropertyAccessException("onDescriptorWriteDeferred is null, you must call initWriteDescriptorOperation() first")

    internal suspend fun waitForDisconnected() = onDisconnectedDeferred?.await()
        ?: throw UninitializedPropertyAccessException("onDisconnectedDeferred is null, you must call initDisconnectOperation() first")

    internal suspend fun waitForServicesDiscovered() = onServicesDiscoveredDeferred?.await()
        ?: throw UninitializedPropertyAccessException("onServicesDiscoveredDeferred is null, you must call initDiscoverServicesOperation() first")

    //endregion
}
