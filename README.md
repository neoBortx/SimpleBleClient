# SimpleBleClient

SimpleBleClient is an Android library designed to simplify Bluetooth Low Energy (BLE) operations, providing a straightforward and coroutine-based API for BLE device interaction.

## Features

- Easy connection and communication with BLE devices.
- Asynchronous operations using Kotlin coroutines.
- Search and filter BLE devices based on services.
- Read and write data to BLE devices.
- Subcribe to connection status changes.


## Gradle

TBD

## Ussage

First you need to initiaze the client

```kotlin
  val simpleBleClient = SimpleBleClientBuilder.build(context)
```

Before to connecto to the device if you don't now the MAC of the device, you should call `getDevicesByService method:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val serviceUUID = UUID.fromString("your-service-uuid")

    val deviceFlow = simpleBleClient.getDevicesByService(serviceUUID)
    deviceFlow.collect { bluetoothDevice ->
        // Handle each found device
        // `bluetoothDevice` is an instance of BluetoothDevice
    }
}
```

This method search only devices that publish a service with the given UUID.

Then with the MAC of the device you can start the connection procedure

```kotlin
CoroutineScope(Dispatchers.IO).launch {
  val result: Boolean = simpleBleClient.connectToDevice(context, "Device_MAC_Address")
}
```

To monitor connection status changes, you can subscribe to the status flow and collect the updates in a coroutine:

```kotlin
CoroutineScope(Dispatchers.Main).launch {
    val connectionStatusFlow = simpleBleClient.subscribeToConnectionStatusChanges()

    connectionStatusFlow.collect { status ->
        // Handle the connection status update
        // `status` could represent different states like connected, disconnected, connection and disconnecting.
    }
}
```

In this example, the collect function of the Flow is used to receive updates about the connection status. It's essential to collect the Flow in a coroutine scope. Since status updates typically result in UI changes, Dispatchers.Main is used. Adjust the dispatcher and handling logic according to your specific needs and application architecture.


To get or send data from/to the device, you must subcribe first to the charactaristics. List librray is prepared to subcribe to all characteristics marked as Noticeable or Indictable in the BLE device. You could filter them passing a list characteristic UUID to monitor.

```kotlin
CoroutineScope(Dispatchers.Main).launch {
    val characteristicsUUIDs = listOf(
        UUID.fromString("your-characteristic-uuid-1"), 
        UUID.fromString("your-characteristic-uuid-2")
    )

    val isSuccess = simpleBleClient.subscribeToCharacteristicChanges(characteristicsUUIDs)
}
```

Note: At this moment this library does not support realtime charaxcteristic monitoring, you have to check its status by calling readData

Once the subcriptions are configured you can read data like this:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val serviceUUID = UUID.fromString("your-service-uuid")
    val characteristicUUID = UUID.fromString("your-characteristic-uuid")

    val result = simpleBleClient.readData(serviceUUID, characteristicUUID)
    // Process the result
}
```

And write data:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val serviceUUID = UUID.fromString("your-service-uuid")
    val characteristicUUID = UUID.fromString("your-characteristic-uuid")
    val dataToSend = "Hello BLE".toByteArray()

    val result = simpleBleClient.sendData(serviceUUID, characteristicUUID, dataToSend)
    // Handle the result
}
```

Remember to replace "Device_MAC_Address", "your-service-uuid", and "your-characteristic-uuid" with actual values relevant to your BLE device and services.

## License

SimpleBleClient is released under the MIT License.





