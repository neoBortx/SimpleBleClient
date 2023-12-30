package com.bortxapps.simplebleclient.manager.utils

import android.util.Log
import com.bortxapps.simplebleclient.exceptions.BleError
import com.bortxapps.simplebleclient.exceptions.SimpleBleClientException
import com.bortxapps.simplebleclient.manager.BleConfiguration
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CancellationException

internal abstract class BleManagerGattOperationBase(
    private val gattMutex: Mutex,
    private val bleConfiguration: BleConfiguration
) {

    protected suspend fun <T> launchGattOperation(operation: suspend () -> T): T {
        val error: BleError = try {
            return withTimeout(bleConfiguration.operationTimeoutMillis) {
                gattMutex.withLock {
                    operation()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("BleManager", "launchGattOperation TIMEOUT ${e.message} ${e.stackTraceToString()}")
            BleError.BLE_DEVICE_NOT_RESPONDING
        } catch (e: SimpleBleClientException) {
            e.bleError
        } catch (e: Exception) {
            Log.e("BleManager", "launchGattOperation ERROR ${e.message} ${e.stackTraceToString()}")
            BleError.COMMUNICATION_FAILED
        }

        throw SimpleBleClientException(error)
    }

    protected suspend fun <T> launchDeferredOperation(operation: suspend () -> T): T {
        val error: BleError = try {
            return operation()
        } catch (e: CancellationException) {
            Log.e("BleManager", "launchGattOperation Failed ${e.message} ${e.stackTraceToString()}")
            BleError.COMMUNICATION_FAILED
        } catch (e: UninitializedPropertyAccessException) {
            Log.e("BleManager", "launchGattOperation Failed ${e.message} ${e.stackTraceToString()}")
            BleError.INTERNAL_ERROR
        } catch (e: Exception) {
            Log.e("BleManager", "launchGattOperation Failed ${e.message} ${e.stackTraceToString()}")
            BleError.OTHER
        }

        throw SimpleBleClientException(error)
    }
}
