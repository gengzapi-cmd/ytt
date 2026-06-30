package com.simpanvideo.app

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class ScrapedMetadata(
    val title: String,
    val uploader: String,
    val thumbnail: String,
    val duration: Int, // detik
    val viewCount: Long,
    val likeCount: Long,
    val extractor: String
)

object MetadataScraper {
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun fetchYoutubeMetadata(urlString: String): ScrapedMetadata? {
        val videoId = getYoutubeVideoId(urlString) ?: return null
        val embedUrl = "https://www.youtube.com/embed/$videoId"
        
        // Coba ambil dari halaman embed karena HTML-nya jauh lebih kecil dan cepat di-load!
        val html = fetchHtml(embedUrl) ?: fetchHtml(urlString) ?: return null
        
        // Ekstraksi Judul
        var title = extractMeta(html, "<meta property=\"og:title\" content=\"([^\"]+)\"")
            ?: extractMeta(html, "<title>([^<]+)</title>")
            ?: "Video YouTube"
        title = title.removeSuffix(" - YouTube").trim()

        // Ekstraksi Uploader/Author
        val uploader = extractMeta(html, "<link itemprop=\"name\" content=\"([^\"]+)\"")
            ?: extractMeta(html, "<meta name=\"author\" content=\"([^\"]+)\"")
            ?: "YouTube Channel"

        // Ekstraksi Durasi (ISO 8601 format like PT3M15S)
        val durationStr = extractMeta(html, "<meta itemprop=\"duration\" content=\"([^\"]+)\"")
        val duration = if (durationStr != null) parseIsoDuration(durationStr) else 0

        // Thumbnail
        val thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        // Views
        val viewsStr = extractMeta(html, "<meta itemprop=\"interactionCount\" content=\"([^\"]+)\"")
        val viewCount = viewsStr?.toLongOrNull() ?: 0L

        return ScrapedMetadata(
            title = title,
            uploader = uploader,
            thumbnail = thumbnail,
            duration = duration,
            viewCount = viewCount,
            likeCount = 0L,
            extractor = "youtube"
        )
    }

    fun fetchTikTokMetadata(urlString: String): ScrapedMetadata? {
        val html = fetchHtml(urlString) ?: return null

        var title = extractMeta(html, "<meta property=\"og:title\" content=\"([^\"]+)\"")
            ?: extractMeta(html, "<title>([^<]+)</title>")
            ?: "Video TikTok"
        title = title.trim()

        val uploader = extractMeta(html, "<meta property=\"og:description\" content=\"([^\"]+)\"")
            ?.split(" ")?.firstOrNull { it.startsWith("@") }
            ?: "TikTok User"

        val thumbnail = extractMeta(html, "<meta property=\"og:image\" content=\"([^\"]+)\"")
            ?: ""

        return ScrapedMetadata(
            title = title,
            uploader = uploader,
            thumbnail = thumbnail,
            duration = 0,
            viewCount = 0L,
            likeCount = 0L,
            extractor = "tiktok"
        )
    }

    private fun fetchHtml(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                var linesRead = 0
                // Kita batasi pembacaan agar sangat cepat
                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                    linesRead++
                    if (linesRead > 500 || response.contains("</head>")) {
                        break
                    }
                }
                reader.close()
                response.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractMeta(html: String, patternString: String): String? {
        return try {
            val pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                decodeHtml(matcher.group(1))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeHtml(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&ndash;", "-")
            .replace("&mdash;", "-")
    }

    private fun getYoutubeVideoId(url: String): String? {
        val patterns = listOf(
            "youtube\\.com/watch\\?v=([^&#\n\r?]+)",
            "youtu\\.be/([^&#\n\r?]+)",
            "youtube\\.com/embed/([^&#\n\r?]+)",
            "youtube\\.com/v/([^&#\n\r?]+)",
            "youtube\\.com/shorts/([^&#\n\r?]+)"
        )
        for (p in patterns) {
            val pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    private fun parseIsoDuration(durationStr: String): Int {
        return try {
            var temp = durationStr.removePrefix("PT")
            var hours = 0
            var minutes = 0
            var seconds = 0
            
            if (temp.contains("H")) {
                val parts = temp.split("H")
                hours = parts[0].toIntOrNull() ?: 0
                temp = if (parts.size > 1) parts[1] else ""
            }
            if (temp.contains("M")) {
                val parts = temp.split("M")
                minutes = parts[0].toIntOrNull() ?: 0
                temp = if (parts.size > 1) parts[1] else ""
            }
            if (temp.contains("S")) {
                val parts = temp.split("S")
                seconds = parts[0].toIntOrNull() ?: 0
            }
            hours * 3600 + minutes * 60 + seconds
        } catch (e: Exception) {
            0
        }
    }
}
