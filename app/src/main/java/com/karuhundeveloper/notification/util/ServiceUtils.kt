package com.karuhundeveloper.notification.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import com.karuhundeveloper.notification.service.AppNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ServiceUtils {
    private const val TAG = "ServiceUtils"

    /**
     * Toggles the [AppNotificationListener] component with a short delay to force
     * the Android system to re-bind the service.
     */
    fun toggleNotificationListenerService(context: Context) {
        val componentName = ComponentName(context, AppNotificationListener::class.java)
        val pm = context.packageManager

        Log.d(TAG, "Toggling NotificationListenerService to force re-bind...")

        // Disable the component
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // Re-enable with a short delay to ensure the OS registers the change
        CoroutineScope(Dispatchers.Main).launch {
            delay(150)
            
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Explicitly request a re-bind from the system (API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    NotificationListenerService.requestRebind(componentName)
                    Log.d(TAG, "Explicit requestRebind sent.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to requestRebind", e)
                }
            }
            Log.i(TAG, "Service component toggled and re-enabled.")
        }
    }
}
