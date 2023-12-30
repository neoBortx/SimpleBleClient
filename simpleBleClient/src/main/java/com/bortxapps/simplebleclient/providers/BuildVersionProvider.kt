package com.bortxapps.simplebleclient.providers

import android.os.Build

internal class BuildVersionProvider {

    fun getSdkVersion() = Build.VERSION.SDK_INT
}
