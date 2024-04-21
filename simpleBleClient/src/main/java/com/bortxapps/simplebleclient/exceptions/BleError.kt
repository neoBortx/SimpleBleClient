package com.bortxapps.simplebleclient.exceptions

public enum class BleError {
    BLE_DEVICE_NOT_FOUND,
    BLE_DEVICE_NOT_CONNECTED,
    MISSING_BLE_PERMISSIONS,
    BLE_NOT_ENABLED,
    BLE_NOT_SUPPORTED,
    INTERNAL_ERROR_COMMUNICATION_CANCELLED_DUE_COROUTINE,
    COMMUNICATION_FAILED,
    SEND_COMMAND_FAILED_NO_DATA_RECEIVED_IN_RESPONSE,
    SEND_COMMAND_FAILED_NO_CHARACTERISTIC_FOUND_TO_SEND,
    UNABLE_TO_SUBSCRIBE_TO_NOTIFICATIONS,
    CANNOT_START_SEARCHING_DEVICES,
    ALREADY_SEARCHING_BLE_DEVICES,
    UNABLE_INITIALIZE_CONTROLLER,
    BLE_DEVICE_NOT_RESPONDING,
    LIBRARY_NOT_INITIALIZED,
    INTERNAL_ERROR,
    OTHER
}
