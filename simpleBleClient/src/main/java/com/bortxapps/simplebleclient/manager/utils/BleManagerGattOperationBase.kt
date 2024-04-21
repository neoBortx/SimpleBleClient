package com.bortxapps.simplebleclient.manager.utils

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
        try {
            return withTimeout(bleConfiguration.operationTimeoutMillis) {
                gattMutex.withLock {
                    operation()
                }
            }
        } catch (e: TimeoutCancellationException) {
            handleException(e, BleError.BLE_DEVICE_NOT_RESPONDING)
        } catch (e: SimpleBleClientException) {
            throw e
        } catch (e: Exception) {
            handleException(e, BleError.COMMUNICATION_FAILED)
        }
    }

    protected suspend fun <T> launchDeferredOperation(operation: suspend () -> T): T {
        try {
            return operation()
        } catch (e: CancellationException) {
            handleException(e, BleError.INTERNAL_ERROR_COMMUNICATION_CANCELLED_DUE_COROUTINE)
        } catch (e: UninitializedPropertyAccessException) {
            handleException(e, BleError.INTERNAL_ERROR)
        } catch (e: SimpleBleClientException) {
            throw e
        } catch (e: Exception) {
            handleException(e, BleError.COMMUNICATION_FAILED)
        }
    }

    private fun handleException(e: Exception, error: BleError): Nothing {
        throw SimpleBleClientException(error, "${e.message} ${e.stackTraceToString()}")
    }
}
