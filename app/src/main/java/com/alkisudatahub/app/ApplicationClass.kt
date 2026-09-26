package com.alkisudatahub.app

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApplicationClass : Application() {

    // Get this from https://dashboard.onesignal.com -> your app -> Settings -> Keys & IDs
    private val oneSignalAppId = "f618c427-6b8c-4b53-aaf2-67e32f688705"

    override fun onCreate() {
        super.onCreate()

        // Remove or set to LogLevel.NONE before publishing to the Play Store
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        OneSignal.initWithContext(this, oneSignalAppId)

        // Ask the user for notification permission the first time the app opens.
        // fallbackToSettings=false means it won't send them to system Settings if
        // they deny it once (they can be re-prompted with your own in-app message later).
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }
    }
}
