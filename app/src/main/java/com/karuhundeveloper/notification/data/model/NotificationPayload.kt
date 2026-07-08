package com.karuhundeveloper.notification.data.model

import com.google.gson.annotations.SerializedName

/**
 * The JSON body forwarded to the configured API endpoint whenever a notification is intercepted.
 *
 * Property names use snake_case via [SerializedName] so the API receives a consistent,
 * conventional JSON structure regardless of Gson's default naming policy.
 *
 * Example serialized output:
 * ```json
 * {
 *   "app_name":            "WhatsApp",
 *   "notification_title":  "John Doe",
 *   "notification_text":   "Hey, are you free tonight?",
 *   "timestamp_ms":        1720432800000
 * }
 * ```
 *
 * @property appName       Human-readable app label (e.g. "WhatsApp"), not the package name.
 * @property notificationTitle  The notification's title / ticker line.
 * @property notificationText   The main body text of the notification.
 * @property timestampMs   Unix epoch millisecond when the notification was intercepted.
 *                         Defaults to [System.currentTimeMillis] at construction time.
 */
data class NotificationPayload(
    @SerializedName("app_name")
    val appName: String,

    @SerializedName("notification_title")
    val notificationTitle: String,

    @SerializedName("notification_text")
    val notificationText: String,

    @SerializedName("timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis()
)
