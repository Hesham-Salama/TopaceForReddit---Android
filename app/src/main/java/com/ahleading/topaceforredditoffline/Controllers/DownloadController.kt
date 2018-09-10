package com.ahleading.topaceforredditoffline.Controllers

import android.os.AsyncTask
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DownloadController {
    private val ACCESS_TOKEN_STR = "access_token"
    private var accessToken: String? = null

    private fun setAccessTokenReddit() {
        val temp = GetAccessJson().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get()
        when {
            temp.contains("TIMEOUT_TOPACE") -> throw Exception("Couldn't fetch posts")
            temp.contains("NO_CONNECTION_TOPACE") -> throw Exception("No Internet connection available")
            temp.contains("UNKNOWN_TOPACE") -> throw Exception("Unknown error")
        }
        accessToken = extractToken(temp)
    }

    private fun extractToken(jsonStr: String): String {
        val response = JSONObject(jsonStr)
        return response.getString(ACCESS_TOKEN_STR)
    }

    fun getJSONReddit(urlReddit: String): String? {
        if (accessToken == null) {
            setAccessTokenReddit()
        }
        var pass = true
        var jsonStr: String?
        var loop = 0
        do {
            if (!pass) {
                setAccessTokenReddit()
            }
            jsonStr = GetJson().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, accessToken, urlReddit).get()
            when {
                jsonStr.contains("TIMEOUT_TOPACE") -> throw Exception("Couldn't fetch posts")
                jsonStr.contains("NO_CONNECTION_TOPACE") -> throw Exception("No Internet connection available")
                jsonStr.contains("UNKNOWN_TOPACE") -> throw Exception("Unknown error")
                else -> {
                    pass = if (jsonStr?.contains("ERROR_404")!! && loop == 0) {
                        loop++
                        false
                        // token may be corrupt, allow only once to set token
                    } else {
                        true
                    }
                }
            }
        } while (!pass)
        return jsonStr
    }

    private class GetJson : AsyncTask<String, Void, String>() {
        private val timeout = 10 * 1000

        fun getJson(accessToken: String, urlReddit: String): String {
            val response: Connection.Response?

            try {
                response = Jsoup.connect(urlReddit).method(Connection.Method.GET)
                        .header("Authorization", "Bearer $accessToken")
                        .ignoreContentType(true)
                        .timeout(timeout)
                        .maxBodySize(0)
                        .execute()
                val reponseStr = response.body()
                return reponseStr
            } catch (e: SocketTimeoutException) {
                return "TIMEOUT_TOPACE"
            } catch (e: UnknownHostException) {
                return "NO_CONNECTION_TOPACE"
            } catch (e: HttpStatusException) {
                return "ERROR_404"
            } catch (e: Exception) {
                return "UNKNOWN_TOPACE"
            }

        }

        override fun doInBackground(vararg urls: String): String {
            return getJson(urls[0], urls[1])
        }
    }

    private class GetAccessJson : AsyncTask<Void, Void, String>() {

        private val CLIENT_ID = "8K8_9JAcZKKXIw"
        private val ACCESS_TOKEN_URL = "https://www.reddit.com/api/v1/access_token"
        private val GRANT_TYPE_VAL = "https://oauth.reddit.com/grants/installed_client"
        private val DONT_TRACK = "DO_NOT_TRACK_THIS_DEVICE"
        private val timeout = 5 * 1000

        fun ByteArray.encodeBase64(): ByteArray {
            val table = (CharRange('A', 'Z') + CharRange('a', 'z') + CharRange('0', '9') + '+' + '/').toCharArray()
            val output = ByteArrayOutputStream()
            var padding = 0
            var position = 0
            while (position < this.size) {
                var b = this[position].toInt() and 0xFF shl 16 and 0xFFFFFF
                if (position + 1 < this.size) b = b or (this[position + 1].toInt() and 0xFF shl 8) else padding++
                if (position + 2 < this.size) b = b or (this[position + 2].toInt() and 0xFF) else padding++
                for (i in 0 until 4 - padding) {
                    val c = b and 0xFC0000 shr 18
                    output.write(table[c].toInt())
                    b = b shl 6
                }
                position += 3
            }
            for (i in 0 until padding) {
                output.write('='.toInt())
            }
            return output.toByteArray()
        }

        fun getToken(): String {
            try {
                val uncodedClientIDAndPassword = "$CLIENT_ID:"
                val encodedStringInBase64 = String(uncodedClientIDAndPassword.toByteArray().encodeBase64())
                val finalAuthStr = "Basic $encodedStringInBase64"
                val conn = Jsoup.connect(ACCESS_TOKEN_URL).method(Connection.Method.POST)
                        .data("grant_type", GRANT_TYPE_VAL)
                        .data("device_id", DONT_TRACK)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Authorization", finalAuthStr)
                        .ignoreContentType(true)
                        .timeout(timeout)

                val resp = conn.execute()
                val json = resp.body()
                return json
                /* {"access_token": "-NmRQOJNDpXf58X5x5S3s6PIT-Sw",
                "token_type": "bearer",
                 "device_id": "DO_NOT_TRACK_THIS_DEVICE",
                  "expires_in": 3600, "scope": "*"} */
            } catch (e: SocketTimeoutException) {
                return "TIMEOUT_TOPACE"
            } catch (e: UnknownHostException) {
                return "NO_CONNECTION_TOPACE"
            } catch (e: Exception) {
                return "UNKNOWN"
            }
        }

        override fun doInBackground(vararg urls: Void): String {
            return getToken()
        }
    }

}