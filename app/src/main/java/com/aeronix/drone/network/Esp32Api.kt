package com.aeronix.drone.network

import android.net.Network
import android.os.SystemClock
import com.aeronix.drone.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * HTTP client for the ESP32. Read-only: the app only ever sends GET to the status endpoint.
 * There is deliberately no code here that sends motor or flight commands.
 *
 * Requests are made through the Wi-Fi [Network] object, so they reach the ESP32 even when
 * Android considers that Wi-Fi "without internet" and prefers mobile data.
 */
class Esp32Api {

    suspend fun fetchStatus(network: Network, s: AppSettings): StatusReading = withContext(Dispatchers.IO) {
        val path = if (s.statusPath.startsWith("/")) s.statusPath else "/${s.statusPath}"
        var conn: HttpURLConnection? = null
        try {
            val url = URL("http", s.host, s.port, path)
            val started = SystemClock.elapsedRealtime()
            conn = network.openConnection(url) as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = s.timeoutMs
            conn.readTimeout = s.timeoutMs
            conn.useCaches = false
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code !in 200..299) {
                throw ConnectionException(
                    ErrorKind.HTTP_ERROR,
                    "The ESP32 answered HTTP $code for $path. Check the status path in Settings."
                )
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }.take(MAX_BODY_CHARS)
            StatusReading(parseTelemetry(body), SystemClock.elapsedRealtime() - started)
        } catch (e: ConnectionException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw ConnectionException(
                ErrorKind.TIMEOUT,
                "The ESP32 did not answer within ${s.timeoutMs / 1000.0} s."
            )
        } catch (e: IOException) {
            throw ConnectionException(
                ErrorKind.UNREACHABLE,
                "Cannot reach ${s.host}:${s.port} (${e.message ?: "network error"})."
            )
        } finally {
            conn?.disconnect()
        }
    }

    companion object {
        private const val MAX_BODY_CHARS = 16_384

        /**
         * Expected JSON (all keys optional, use null or omit what you cannot measure):
         * {"device":"AERONIX-ESP32","firmware":"1.0.0","chip":"ESP32","uptime_ms":12345,
         *  "free_heap":210000,"rssi_dbm":-52,"battery_v":7.9,"battery_pct":82,
         *  "altitude_m":1.2,"armed":false}
         */
        internal fun parseTelemetry(body: String): Telemetry {
            val o = try {
                JSONObject(body)
            } catch (e: JSONException) {
                throw ConnectionException(ErrorKind.BAD_RESPONSE, "The ESP32 replied, but not with valid JSON.")
            }
            return Telemetry(
                device = o.stringOrNull("device"),
                firmware = o.stringOrNull("firmware"),
                chip = o.stringOrNull("chip"),
                uptimeMs = o.doubleOrNull("uptime_ms")?.toLong(),
                freeHeapBytes = o.doubleOrNull("free_heap")?.toLong(),
                rssiDbm = o.doubleOrNull("rssi_dbm")?.toInt()?.takeIf { it in -127..0 },
                batteryVolts = o.doubleOrNull("battery_v")?.takeIf { it in 0.0..60.0 },
                batteryPercent = o.doubleOrNull("battery_pct")?.toInt()?.takeIf { it in 0..100 },
                altitudeM = o.doubleOrNull("altitude_m"),
                armed = if (!o.has("armed") || o.isNull("armed")) null else o.optBoolean("armed"),
            )
        }

        private fun JSONObject.stringOrNull(key: String): String? =
            if (!has(key) || isNull(key)) null else optString(key).ifBlank { null }

        private fun JSONObject.doubleOrNull(key: String): Double? =
            if (!has(key) || isNull(key)) null else optDouble(key, Double.NaN).takeIf { !it.isNaN() }
    }
}
