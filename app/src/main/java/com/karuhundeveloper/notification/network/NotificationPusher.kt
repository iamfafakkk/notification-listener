package com.karuhundeveloper.notification.network

import com.google.gson.Gson
import com.karuhundeveloper.notification.data.local.entity.InterceptRule
import com.karuhundeveloper.notification.data.model.NotificationPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Responsible for forwarding intercepted notifications to user-configured API endpoints.
 *
 * ### Threading
 * [push] is a suspend function that always switches to [dispatcher] (default: [Dispatchers.IO])
 * before touching the network, so it is safe to call from any coroutine — including one running
 * on the main thread.
 *
 * ### Supported HTTP methods
 * | Method | Body sent? |
 * |--------|-----------|
 * | POST   | ✅ JSON payload |
 * | PUT    | ✅ JSON payload |
 * | PATCH  | ✅ JSON payload |
 * | GET    | ❌ — payload appended as query-string params |
 * | DELETE | ❌ — no body |
 *
 * Any other method string returns [PushResult.UnsupportedMethod].
 *
 * ### Error handling
 * All outcomes (including failures) are surfaced through [PushResult] so the caller never
 * needs to catch exceptions from this class directly.
 *
 * @param client    Shared [OkHttpClient] instance. Pass in your app-level singleton to share
 *                  connection pools and thread pools across all pushes.
 * @param gson      Shared [Gson] instance used to serialize [NotificationPayload] to JSON.
 * @param dispatcher The [CoroutineDispatcher] used for the blocking OkHttp call.
 *                   Defaults to [Dispatchers.IO] and can be overridden in unit tests.
 */
class NotificationPusher(
    private val client: OkHttpClient = defaultClient(),
    private val gson: Gson = Gson(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds and executes an HTTP request according to [rule], forwarding [payload] as the body.
     *
     * @param rule    The [InterceptRule] that matched the incoming notification. Supplies the
     *                target URL, HTTP method, and any custom headers.
     * @param payload The [NotificationPayload] describing the intercepted notification.
     * @return A [PushResult] — never throws.
     */
    suspend fun push(
        rule: InterceptRule,
        payload: NotificationPayload
    ): PushResult = withContext(dispatcher) {
        try {
            val request = buildRequest(rule, payload)
                ?: return@withContext PushResult.UnsupportedMethod(rule.apiMethod)

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    PushResult.Success(response.code, body)
                } else {
                    PushResult.HttpError(response.code, body)
                }
            }
        } catch (e: IOException) {
            PushResult.NetworkError(e)
        }
    }

    // ── Request construction ──────────────────────────────────────────────────

    /**
     * Builds an [okhttp3.Request] for the given [rule] and [payload].
     *
     * Returns `null` when [InterceptRule.apiMethod] is not supported so the caller
     * can surface a [PushResult.UnsupportedMethod] without throwing.
     */
    private fun buildRequest(
        rule: InterceptRule,
        payload: NotificationPayload
    ): Request? {
        val method = rule.apiMethod.uppercase().trim()
        val jsonBody = gson.toJson(payload)

        val requestBuilder = Request.Builder()

        // ── Attach custom headers from the rule ───────────────────────────────
        rule.customHeaders.forEach { (name, value) ->
            requestBuilder.addHeader(name, value)
        }

        return when (method) {
            // ── Body methods ──────────────────────────────────────────────────
            "POST", "PUT", "PATCH" -> {
                val body: RequestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
                requestBuilder
                    .url(rule.apiUrl)
                    .method(method, body)
                    .build()
            }

            // ── GET: no body — append payload fields as query parameters ─────
            "GET" -> {
                val urlWithParams = rule.apiUrl.toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("app_name", payload.appName)
                    .addQueryParameter("notification_title", payload.notificationTitle)
                    .addQueryParameter("notification_text", payload.notificationText)
                    .addQueryParameter("timestamp_ms", payload.timestampMs.toString())
                    .build()

                requestBuilder
                    .url(urlWithParams)
                    .get()
                    .build()
            }

            // ── DELETE: fire with no body ─────────────────────────────────────
            "DELETE" -> {
                requestBuilder
                    .url(rule.apiUrl)
                    .delete()
                    .build()
            }

            // ── Unsupported ───────────────────────────────────────────────────
            else -> null
        }
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /**
         * Sensible production defaults.
         * - Connect / read / write timeouts: 15 s each — long enough for slow endpoints,
         *   short enough to avoid hanging the notification listener indefinitely.
         * - [OkHttpClient] is heavyweight; create it **once** at app startup and reuse it.
         */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
