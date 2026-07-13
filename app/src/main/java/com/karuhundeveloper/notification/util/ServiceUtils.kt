package com.karuhundeveloper.notification.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.karuhundeveloper.notification.service.AppNotificationListener

object ServiceUtils {
    private const val TAG = "ServiceUtils"

    /**
     * Toggles the [AppNotificationListener] component.
     * This "kicks" the Android system into re-binding the service if it has
     * stalled or was killed without being restarted.
     */
    fun toggleNotificationListenerService(context: Context) {
        val componentName = ComponentName(context, AppNotificationListener::class.java)
        val pm = context.packageManager

        Log.d(TAG, "Toggling NotificationListenerService to force re-bind...")

        // Disable then re-enable the component
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
