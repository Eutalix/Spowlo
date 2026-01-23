package com.bobbyesp.spowlo.utils

import android.util.Log
import com.bobbyesp.spowlo.utils.PreferencesUtil.getInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object NewsUtil {

    private const val TAG = "NewsUtil"
    
    // URL pointing to the raw JSON file 
    private const val NEWS_URL = "https://raw.githubusercontent.com/Eutalix/Spowlo/main/spowlo_news.json"

    private val client = OkHttpClient()
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    @Serializable
    data class NewsRelease(
        val id: Int = 0,
        val title: String = "",
        val body: String = "",
        val action_url: String? = null,
        val action_text: String? = null,
        val is_critical: Boolean = false
    )

    /**
     * Checks for new news/announcements.
     * Returns a [NewsRelease] only if the remote ID is greater than the locally stored ID.
     */
    suspend fun checkForNews(): NewsRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(NEWS_URL).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }

            val responseData = response.body?.string() ?: return@withContext null
            response.close()

            val news = jsonFormat.decodeFromString<NewsRelease>(responseData)
            val lastSeenId = LAST_NEWS_ID.getInt()

            Log.d(TAG, "Remote News ID: ${news.id} | Local Last Seen: $lastSeenId")

            // Only return news if it is new
            if (news.id > lastSeenId) {
                return@withContext news
            } else {
                return@withContext null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun markNewsAsRead(newsId: Int) {
        PreferencesUtil.setLastNewsId(newsId)
    }
}