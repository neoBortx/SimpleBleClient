# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.lang.invoke.StringConcatFactory

-keepclassmembers class ** {
    public void enableLogging();
}
-keep class android.util.Log {
    public static *** e(...);
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

-keep public interface com.bortxapps.simplebleclient.api.contracts.BleNetworkMessageProcessor { public *; }
-keep public interface com.bortxapps.simplebleclient.api.contracts.SimpleBleClient { public *; }
-keep class com.bortxapps.simplebleclient.api.contracts.SimpleBleClient$DefaultImpls { *; }
-keep public interface com.bortxapps.simplebleclient.api.contracts.SimpleBleClientConnection { public *; }
-keep class com.bortxapps.simplebleclient.api.contracts.SimpleBleClientConnection$DefaultImpls { *; }
-keep public interface com.bortxapps.simplebleclient.api.contracts.SimpleBleClientDeviceSeeker { public *; }
-keep class com.bortxapps.simplebleclient.api.contracts.SimpleBleClientDeviceSeeker$DefaultImpls { *; }
-keep public interface com.bortxapps.simplebleclient.api.contracts.SimpleBleClientReader { public *; }
-keep class com.bortxapps.simplebleclient.api.contracts.SimpleBleClientReader$DefaultImpls { *; }
-keep public interface com.bortxapps.simplebleclient.api.contracts.SimpleBleClientWriter { public *; }
-keep class com.bortxapps.simplebleclient.api.contracts.SimpleBleClientWriter$DefaultImpls { *; }
-keep public interface com.bortxapps.simplebleclient.api.contracts.SimpleBleClientSubscription { public *; }
-keep class com.bortxapps.simplebleclient.api.contracts.SimpleBleClientSubscription$DefaultImpls { *; }
-keep public class com.bortxapps.simplebleclient.api.data.BleConnectionStatus { public *; }
-keep public class com.bortxapps.simplebleclient.api.data.BleNetworkMessage { public *; }
-keep public class com.bortxapps.simplebleclient.api.data.BleCharacteristic { public *; }
-keep public class com.bortxapps.simplebleclient.api.SimpleBleClientBuilder { public *; }
-keep public class com.bortxapps.simplebleclient.exceptions.BleError { public *; }
-keep public class com.bortxapps.simplebleclient.exceptions.SimpleBleClientException { public *; }