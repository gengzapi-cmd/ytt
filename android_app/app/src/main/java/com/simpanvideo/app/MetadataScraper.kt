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
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        
        val html = fetchHtml(watchUrl) ?: return null
        
        var title = ""
        var uploader = ""
        var duration = 0
        var viewCount = 0L

        // Primary: Parse using ytInitialPlayerResponse JSON payload
        val marker = "ytInitialPlayerResponse = "
        val index = html.indexOf(marker)
        if (index != -1) {
            val start = index + marker.length
            val end = html.indexOf("};", start)
            if (end != -1) {
                val jsonStr = html.substring(start, end + 1)
                val vdetailsIdx = jsonStr.indexOf("\"videoDetails\":")
                val searchStr = if (vdetailsIdx != -1) jsonStr.substring(vdetailsIdx) else jsonStr

                title = extractJsonString(searchStr, "title") ?: ""
                uploader = extractJsonString(searchStr, "author") ?: ""
                duration = extractJsonString(searchStr, "lengthSeconds")?.toIntOrNull() ?: 0
                viewCount = extractJsonString(searchStr, "viewCount")?.toLongOrNull() ?: 0L
            }
        }

        // Secondary / Fallback: Parse using OpenGraph and meta tags
        if (title.isEmpty()) {
            title = extractMeta(html, "<meta property=\"og:title\" content=\"([^\"]+)\"")
                ?: extractMeta(html, "<title>([^<]+)</title>")
                ?: "Video YouTube"
            title = title.removeSuffix(" - YouTube").trim()
        }

        if (uploader.isEmpty()) {
            uploader = extractMeta(html, "<link itemprop=\"name\" content=\"([^\"]+)\"")
                ?: extractMeta(html, "<meta name=\"author\" content=\"([^\"]+)\"")
                ?: "YouTube Channel"
        }

        if (duration == 0) {
            val durationStr = extractMeta(html, "<meta itemprop=\"duration\" content=\"([^\"]+)\"")
            duration = if (durationStr != null) parseIsoDuration(durationStr) else 0
        }

        if (viewCount == 0L) {
            val viewsStr = extractMeta(html, "<meta itemprop=\"interactionCount\" content=\"([^\"]+)\"")
            viewCount = viewsStr?.toLongOrNull() ?: 0L
        }

        val thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

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
            if (urlString.contains("youtube.com") || urlString.contains("youtu.be")) {
                connection.setRequestProperty("Referer", "https://www.youtube.com")
            }
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                var linesRead = 0
                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                    linesRead++
                    
                    val hasYtResponse = response.contains("ytInitialPlayerResponse = ") && response.contains("};")
                    if (hasYtResponse) {
                        break
                    }
                    
                    if (!urlString.contains("youtube.com") && !urlString.contains("youtu.be")) {
                        if (response.contains("</head>") || linesRead > 500) {
                            break
                        }
                    }
                    
                    if (linesRead > 3000) {
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

    private fun extractJsonString(jsonStr: String, key: String): String? {
        try {
            val pattern = Pattern.compile("\"$key\"\\s*:\\s*\"?((?:[^\"\\\\]|\\\\.)*)\"?", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(jsonStr)
            if (matcher.find()) {
                val value = matcher.group(1) ?: return null
                return unescapeJson(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun unescapeJson(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                val next = input[i + 1]
                when (next) {
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '/' -> { sb.append('/'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000c'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'u' -> {
                        if (i + 5 < input.length) {
                            try {
                                val hex = input.substring(i + 2, i + 6)
                                val code = hex.toInt(16)
                                sb.append(code.toChar())
                                i += 6
                            } catch (e: Exception) {
                                sb.append("\\u")
                                i += 2
                            }
                        } else {
                            sb.append("\\u")
                            i += 2
                        }
                    }
                    else -> {
                        sb.append(c)
                        i++
                    }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
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
