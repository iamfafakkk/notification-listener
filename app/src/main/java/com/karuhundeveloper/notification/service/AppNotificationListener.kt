package com.karuhundeveloper.notification.service

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.karuhundeveloper.notification.data.local.AppDatabase
import com.karuhundeveloper.notification.data.local.entity.InterceptRule
import com.karuhundeveloper.notification.data.model.NotificationPayload
import com.karuhundeveloper.notification.network.NotificationPusher
import com.karuhundeveloper.notification.network.PushResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A [NotificationListenerService] that receives every posted notification on the device,
 * matches it against user-configured [InterceptRule]s stored in [AppDatabase], and
 * forwards matching notifications to the rule's configured API endpoint via [NotificationPusher].
 *
 * ### Lifecycle
 * Android binds this service automatically once the user grants Notification Access in
 * Settings → Apps → Special app access → Notification access.
 * [onListenerConnected] / [onListenerDisconnected] fire when the binding is established or lost.
 *
 * ### Threading
 * All database and network work runs on [serviceScope], a [SupervisorJob]-backed
 * [CoroutineScope] bound to [Dispatchers.IO]. The scope is cancelled in [onDestroy] to
 * avoid leaking coroutines after the service is torn down.
 * A [SupervisorJob] is used so that a failure in one push job does not cancel other in-flight jobs.
 *
 * ### Registration
 * The service must be declared in AndroidManifest.xml with:
 * - `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"`
 * - An intent-filter for `android.service.notification.NotificationListenerService`
 */
class AppNotificationListener : NotificationListenerService() {

    // ── Coroutine scope ───────────────────────────────────────────────────────

    /**
     * Supervisor scope: a child job failure (e.g. one bad push) does not cancel sibling jobs.
     * Cancelled in [onDestroy] to free resources when the service is unbound.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Lazily-initialised collaborators ──────────────────────────────────────

    /** Singleton database; created once and reused for the lifetime of the service. */
    private val database: AppDatabase by lazy { AppDatabase.getInstance(applicationContext) }

    /**
     * Single [NotificationPusher] instance. The internal [okhttp3.OkHttpClient] maintains
     * a connection pool — sharing the instance avoids creating a new pool per notification.
     */
    private val pusher: NotificationPusher by lazy { NotificationPusher() }

    // ── NotificationListenerService callbacks ─────────────────────────────────

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected.")
    }

    /**
     * Called by the system for every notification posted anywhere on the device.
     *
     * This method runs on the **main thread**, so all heavy work is immediately
     * dispatched to [serviceScope] to keep the main thread unblocked.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return  // guard: paranoia null-check

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        // Extract title and text from the notification extras bundle
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        // Skip notifications with no useful content
        if (title.isBlank() && text.isBlank()) {
            Log.v(TAG, "Skipping empty notification from $packageName")
            return
        }

        Log.d(TAG, "Notification received from $packageName | title=$title")

        serviceScope.launch {
            processNotification(packageName, title, text)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all in-flight coroutine jobs when the service is torn down
        serviceScope.cancel("AppNotificationListener destroyed")
        Log.i(TAG, "Service destroyed — coroutine scope cancelled.")
    }

    // ── Internal logic ────────────────────────────────────────────────────────

    /**
     * Performs the full intercept pipeline on a background thread:
     * 1. Load all saved [InterceptRule]s from the database (single snapshot via [Flow.first]).
     * 2. Find every rule whose [InterceptRule.targetApps] list contains [packageName].
     * 3. For each matching rule, build a [NotificationPayload] and call [NotificationPusher.push].
     * 4. Log the outcome.
     *
     * Running all rules in parallel would be faster but risks flooding the same endpoint;
     * sequential execution is the safer default — change to `async`/`awaitAll` if needed.
     *
     * @param packageName The originating app's package name (e.g. "com.whatsapp").
     * @param title       Notification title string.
     * @param text        Notification body text.
     */
    private suspend fun processNotification(
        packageName: String,
        title: String,
        text: String
    ) {
        // Collect a single snapshot of rules; avoids keeping a long-lived collector open
        val allRules: List<InterceptRule> = try {
            database.interceptRuleDao().getAllRules().first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rules from database", e)
            return
        }

        // Find rules that explicitly target this package
        val matchingRules = allRules.filter { rule ->
            packageName in rule.targetApps
        }

        if (matchingRules.isEmpty()) {
            Log.v(TAG, "No rules match package $packageName — ignoring.")
            return
        }

        Log.d(TAG, "${matchingRules.size} rule(s) matched for $packageName")

        // Resolve the human-readable app label once for all matching rules
        val appName = resolveAppLabel(packageName)

        val payload = NotificationPayload(
            appName = appName,
            notificationTitle = title,
            notificationText = text
        )

        // Execute each matching rule sequentially
        matchingRules.forEach { rule ->
            dispatchPush(rule, payload)
        }
    }

    /**
     * Calls [NotificationPusher.push] for a single rule and logs the result.
     * Wrapped in try-catch as a last-resort safety net; [PushResult] handles all
     * expected failures without throwing.
     */
    private suspend fun dispatchPush(rule: InterceptRule, payload: NotificationPayload) {
        Log.d(TAG, "Pushing for rule '${rule.ruleName}' → ${rule.apiUrl}")
        try {
            when (val result = pusher.push(rule, payload)) {
                is PushResult.Success -> Log.i(
                    TAG,
                    "✅ Rule '${rule.ruleName}': delivered (HTTP ${result.statusCode})"
                )
                is PushResult.HttpError -> Log.w(
                    TAG,
                    "⚠️ Rule '${rule.ruleName}': HTTP ${result.statusCode} — ${result.errorBody}"
                )
                is PushResult.NetworkError -> Log.e(
                    TAG,
                    "❌ Rule '${rule.ruleName}': network error — ${result.cause.message}",
                    result.cause
                )
                is PushResult.UnsupportedMethod -> Log.e(
                    TAG,
                    "❌ Rule '${rule.ruleName}': unsupported method '${result.method}'"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error pushing for rule '${rule.ruleName}'", e)
        }
    }

    /**
     * Resolves the human-readable application label for a package name.
     * Falls back to the package name itself if the label cannot be retrieved
     * (e.g. package was just uninstalled).
     */
    private fun resolveAppLabel(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Could not resolve label for $packageName, using package name as fallback")
            packageName
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "AppNotificationListener"
    }
}
