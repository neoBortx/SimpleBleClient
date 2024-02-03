[![](https://jitpack.io/v/neoBortx/SimpleBleClient.svg)](https://jitpack.io/#neoBortx/SimpleBleClient)

# SimpleBleClient

SimpleBleClient is an Android library designed to simplify Bluetooth Low Energy (BLE) operations, providing a straightforward and coroutine-based API for BLE device interaction.

## Features

- Easy connection and communication with BLE devices.
- Asynchronous operations using Kotlin coroutines.
- Search and filter BLE devices based on services.
- Read and write data from/to BLE devices.
- Subscribe to connection status changes.
- Subscribe to characteristics changes


## Gradle

Add Jitpack repository to your root build.gradle at the end of repositories:

```groovy
repositories {
  mavenCentral()
  maven { url 'https://jitpack.io' }
}
```

Add the dependency
```groovy
dependencies {
  implementation 'com.github.neoBortx:SimpleBleClient:0.0.4'
}
```


## Initialization

First you need to build the client, some behaviors of the BLE client can be configured during the building procedure

```kotlin
    companion object {

        /**
         * Number of messages to store in incoming message buffer, if the buffer is full, the oldest message will be 
         * removed
         * Default 1
         */
        const val MESSAGE_BUFFER_SIZE = 20

        /**
         * BLE operation Timeout. If the operation (read, write, subscription or connection) takes longer than this an 
         * exception will be thrown
         * Default 7000 milliseconds
         */

        const val OPERATION_TIMEOUT_MILLIS = 8000L

        /**
         * The duration of the timeout for scanning BLE devices
         * Default 10000 milliseconds
         */

        const val SCAN_PERIOD_MILLIS = 30000L

        /**
         * The number of messages to store in the incoming message buffer to new consumer of the incoming message flow
         * Default 1
        **/
        const val MESSAGE_BUFFER_RETRIES = 0
    }

    /**
     * Depending on the protocol used by the device, the message received from the device can be fragmented or may 
     * require a special treatment.
     *
     * BLE operations are asynchronous but you only can perform one in a time. When you're expecting a fragmented 
     * messages, you need to wait for the last message to be received before sending the next operation. 
     * Because this is not straightforward, this interface is used to
     * encapsulate the logic to handle this kind of messages in lower layers for you.
     *
     * You should implement your own and pass it to the SimpleBleClientBuilder.
     *
     * Default null
     */
    private val messageProcessor = BleNetworkMessageProcessorImpl()

    private val simpleBleClient = SimpleBleClientBuilder()
        .setMessageBufferRetries(MESSAGE_BUFFER_RETRIES)
        .setMessageBufferSize(MESSAGE_BUFFER_SIZE)
        .setOperationTimeOutMillis(OPERATION_TIMEOUT_MILLIS)
        .setScanPeriodMillis(SCAN_PERIOD_MILLIS)
        .setMessageProcessor(messageProcessor)
        .build(context)
```


## Search devices

You can retrieve the list of all BLE devices detected by the Android phone in this moment

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val deviceFlow = simpleBleClient.deviceSeeker.getDevicesNearby()
    deviceFlow.collect { bluetoothDevice ->
        // Handle each found device
        // `bluetoothDevice` is an instance of BluetoothDevice
    }
}
```

Or filter by name and/or service UUID

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val serviceUUID = UUID.fromString("your-service-uuid")
    val deviceName = "MyCamera"

    val deviceFlow = simpleBleClient.deviceSeeker.getDevicesNearby(serviceUUID, deviceName)
    deviceFlow.collect { bluetoothDevice ->
        // Handle each found device
        // `bluetoothDevice` is an instance of BluetoothDevice
    }
}
```

This function returns a cold flow with the list of all detected devices. When the timeout ends, the flow will be closed.

During the search operation, you can stop the procedure anytime:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    simpleBleClient.deviceSeeker.stopSearchDevices()
}
```


## Connection

Once you have the MAC address of the device you can start the connection procedure

```kotlin
CoroutineScope(Dispatchers.IO).launch {
  val result: Boolean = simpleBleClient.connectToDevice(context, "Device_MAC_Address")
}
```

To monitor connection status changes, you can subscribe to the status flow and collect the updates in a coroutine:

```kotlin
val connectionStatusFlow = simpleBleClient.subscribeToConnectionStatusChanges().collect { status ->
  // Handle the connection status update
  // `status` could represent different states like connected, disconnected, connection and disconnecting.
}
```

In this example, the collect function of the Flow is used to receive updates about the connection status. The possible states are:
- CONNECTED
- DISCONNECTED
- CONNECTING
- DISCONNECTING
- UNKNOWN


Also, you can disconnect the BLE device anytime:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    simpleBleClient.connection.disconnect()
}
```


## Subscribe to characteristics changes

Before you get or send data from/to the device, you must first subscribe to its characteristics. This is mandatory and without making the subscription you won't be able to read any data sent from the BLE device. 
So, you must compose a list of characteristics that you want to observe, like this:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val characteristicsUUIDs = listOf(
        UUID.fromString("your-characteristic-uuid-1"), 
        UUID.fromString("your-characteristic-uuid-2")
    )

    val isSuccess = simpleBleClient.subscription.subscribeToCharacteristicChanges(characteristicsUUIDs)
}
```


If you want to handle incoming messages in a reactive way using flows, you must invoke subscribeToIncomeMessages, which will return a hot flow with all incoming messages.

```kotlin
val connectionStatusFlow = simpleBleClient.subscription.subscribeToIncomeMessages().collect { status ->
  // Handle the connection status update
  // `status` could represent different states like connected, disconnected, connection and disconnecting.
}
```


## Read characteristics

Note: You must subscribe to the characteristics changes before try to read it.

This is quite straightforward; you must pass the characteristic UUID and the service that handles this characteristic to the readData function.
The value of the characteristic will be returned as a result of these functions. Also, this message can be handled by collecting the flow given by subscribeToIncomeMessages.
Important: To manage fragmented characteristics, you should implement the interface BleNetworkMessageProcessor and pass it to SimpleBleClientBuilder at initialization time.


```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val serviceUUID = UUID.fromString("your-service-uuid")
    val characteristicUUID = UUID.fromString("your-characteristic-uuid")

    val result = simpleBleClient.reader.readData(serviceUUID, characteristicUUID)
    // Process the result
}
```


### Write characteristics

You can perform two different write operations:

### With response

In this case, you send some data to write to the BLE device, and it responds to you with some value, like an ACK or KO code. This operation returns only when the reponse
data has been processsed and will be included in the return object (BleNetworkMessage).

You must send to the library the characteristic UUID, the UUID of the service that handles the characteristic, and the byteArray of data saved in the BLE device.

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val serviceUUID = UUID.fromString("your-service-uuid")
    val characteristicUUID = UUID.fromString("your-characteristic-uuid")
    val dataToSend = "Hello BLE".toByteArray()

    val result = simpleBleClient.writer.sendDataWithResponse(serviceUUID, characteristicUUID, dataToSend)
    // Handle the result
}
```

The returned data is also sent to the hot flow given by the function subscribeToIncomeMessages.

## Without response

Fire and forget operation. It Just sent data to the BLE device without expecting any response.
You must send to the library the characteristic UUID, the UUID of the service that handles the characteristic, and the byteArray of data saved in the BLE device.

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val serviceUUID = UUID.fromString("your-service-uuid")
    val characteristicUUID = UUID.fromString("your-characteristic-uuid")
    val dataToSend = "Hello BLE".toByteArray()

    val result = simpleBleClient.writer.sendData(serviceUUID, characteristicUUID, dataToSend)
    // Handle the result
}
```


## License

SimpleBleClient is released under the MIT License.





