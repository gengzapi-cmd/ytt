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
    val extractor: String,
    val qualities: String = "",
    val videoType: String = "reguler",
    val tiktokVideoUrl: String = "",
    val tiktokAudioMusikUrl: String = "",
    val tiktokSlides: List<String> = emptyList()
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
        var likeCount = 0L
        var qualities = ""

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
                likeCount = extractJsonString(jsonStr, "likeCount")?.toLongOrNull() ?: 0L
                qualities = parseQualities(jsonStr)
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

        val isLive = html.contains("\"isLive\":true") || html.contains("\"isLiveContent\":true") || urlString.contains("/live/", ignoreCase = true)
        val isShort = urlString.contains("/shorts/", ignoreCase = true)
        val videoType = when {
            isLive -> "live"
            isShort -> "short"
            else -> "reguler"
        }

        return ScrapedMetadata(
            title = title,
            uploader = uploader,
            thumbnail = thumbnail,
            duration = duration,
            viewCount = viewCount,
            likeCount = likeCount,
            extractor = "youtube",
            qualities = qualities,
            videoType = videoType
        )
    }

    private fun findJsonObjectRecursively(json: org.json.JSONObject, keyToFind: String): org.json.JSONObject? {
        if (json.has(keyToFind)) {
            val obj = json.optJSONObject(keyToFind)
            if (obj != null) return obj
        }
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val innerObj = json.optJSONObject(key)
            if (innerObj != null) {
                val found = findJsonObjectRecursively(innerObj, keyToFind)
                if (found != null) return found
            } else {
                val arr = json.optJSONArray(key)
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val arrObj = arr.optJSONObject(i)
                        if (arrObj != null) {
                            val found = findJsonObjectRecursively(arrObj, keyToFind)
                            if (found != null) return found
                        }
                    }
                }
            }
        }
        return null
    }

    fun fetchTikTokMetadata(urlString: String): ScrapedMetadata? {
        val html = fetchHtml(urlString) ?: return null

        var title = extractMeta(html, "<meta property=\"og:title\" content=\"([^\"]+)\"")
            ?: extractMeta(html, "<title>([^<]+)</title>")
            ?: "Video TikTok"
        title = title.trim()

        var uploader = extractMeta(html, "<meta property=\"og:description\" content=\"([^\"]+)\"")
            ?.split(" ")?.firstOrNull { it.startsWith("@") }
            ?: "TikTok User"

        var thumbnail = extractMeta(html, "<meta property=\"og:image\" content=\"([^\"]+)\"")
            ?: ""

        var videoUrl = ""
        var audioMusikUrl = ""
        var duration = 0
        var viewCount = 0L
        var likeCount = 0L
        val slides = mutableListOf<String>()
        var videoType = "Tiktok Video"

        var jsonObject: org.json.JSONObject? = null

        // 1. Try __UNIVERSAL_DATA_FOR_REHYDRATION__
        try {
            val startTag = "<script id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\" type=\"application/json\">"
            val endTag = "</script>"
            val idx = html.indexOf(startTag)
            if (idx != -1) {
                val startJson = idx + startTag.length
                val endJson = html.indexOf(endTag, startJson)
                if (endJson != -1) {
                    val jsonStr = html.substring(startJson, endJson).trim()
                    jsonObject = org.json.JSONObject(jsonStr)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try SIGI_STATE as fallback
        if (jsonObject == null) {
            try {
                val startTag = "<script id=\"SIGI_STATE\" type=\"application/json\">"
                val endTag = "</script>"
                val idx = html.indexOf(startTag)
                if (idx != -1) {
                    val startJson = idx + startTag.length
                    val endJson = html.indexOf(endTag, startJson)
                    if (endJson != -1) {
                        val jsonStr = html.substring(startJson, endJson).trim()
                        jsonObject = org.json.JSONObject(jsonStr)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Parse fields from jsonObject using recursive helper
        if (jsonObject != null) {
            try {
                val itemStruct = findJsonObjectRecursively(jsonObject, "itemStruct") 
                    ?: findJsonObjectRecursively(jsonObject, "item_struct")
                    ?: findJsonObjectRecursively(jsonObject, "ItemModule")?.let { itemModule ->
                        // SIGI_STATE style item module
                        val keys = itemModule.keys()
                        if (keys.hasNext()) {
                            itemModule.optJSONObject(keys.next())
                        } else null
                    }
                    ?: findJsonObjectRecursively(jsonObject, "video")?.let { jsonObject }
                
                if (itemStruct != null) {
                    val desc = itemStruct.optString("desc") ?: itemStruct.optString("title")
                    if (!desc.isNullOrEmpty()) title = desc
                    
                    val authorObj = itemStruct.optJSONObject("author") ?: itemStruct.optJSONObject("author_info")
                    if (authorObj != null) {
                        val uniqueId = authorObj.optString("uniqueId") ?: authorObj.optString("unique_id") ?: authorObj.optString("nickname")
                        if (uniqueId.isNotEmpty()) uploader = "@$uniqueId"
                    } else {
                        val authorName = itemStruct.optString("author")
                        if (authorName.isNotEmpty()) uploader = "@$authorName"
                    }

                    val videoObj = itemStruct.optJSONObject("video") ?: findJsonObjectRecursively(itemStruct, "video")
                    if (videoObj != null) {
                        videoUrl = videoObj.optString("playAddr")
                        if (videoUrl.isEmpty()) videoUrl = videoObj.optString("downloadAddr")
                        if (videoUrl.isEmpty()) videoUrl = videoObj.optString("play_addr")
                        if (videoUrl.isEmpty()) videoUrl = videoObj.optString("download_addr")
                        duration = videoObj.optInt("duration")
                        val cover = videoObj.optString("cover") ?: videoObj.optString("cover_addr")
                        if (cover.isNotEmpty()) thumbnail = cover
                    }

                    val statsObj = itemStruct.optJSONObject("stats") ?: itemStruct.optJSONObject("statistics") ?: findJsonObjectRecursively(itemStruct, "stats")
                    if (statsObj != null) {
                        viewCount = statsObj.optLong("playCount") ?: statsObj.optLong("play_count") ?: statsObj.optLong("viewCount")
                        likeCount = statsObj.optLong("diggCount") ?: statsObj.optLong("digg_count") ?: statsObj.optLong("likeCount")
                    }

                    val musicObj = itemStruct.optJSONObject("music") ?: itemStruct.optJSONObject("music_info") ?: findJsonObjectRecursively(itemStruct, "music")
                    if (musicObj != null) {
                        audioMusikUrl = musicObj.optString("playUrl") ?: musicObj.optString("play_url")
                    }

                    val imagePostInfoObj = itemStruct.optJSONObject("imagePostInfo") 
                        ?: itemStruct.optJSONObject("image_post_info")
                        ?: findJsonObjectRecursively(itemStruct, "imagePostInfo")
                        ?: findJsonObjectRecursively(itemStruct, "image_post_info")
                    if (imagePostInfoObj != null) {
                        val imagesArr = imagePostInfoObj.optJSONArray("images") ?: imagePostInfoObj.optJSONArray("image_list")
                        if (imagesArr != null && imagesArr.length() > 0) {
                            for (i in 0 until imagesArr.length()) {
                                val imgObj = imagesArr.optJSONObject(i)
                                val displayImage = imgObj?.optJSONObject("displayImage") 
                                    ?: imgObj?.optJSONObject("display_image")
                                    ?: imgObj?.optJSONObject("imageURL")
                                    ?: imgObj?.optJSONObject("image_url")
                                    ?: imgObj
                                val urlList = displayImage?.optJSONArray("urlList") ?: displayImage?.optJSONArray("url_list")
                                if (urlList != null && urlList.length() > 0) {
                                    val firstUrl = urlList.optString(0)
                                    if (firstUrl.isNotEmpty()) slides.add(firstUrl)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. General Regex Fallbacks
        if (videoUrl.isEmpty()) {
            videoUrl = extractMeta(html, "\"playAddr\":\"([^\"]+)\"") ?: ""
            if (videoUrl.isNotEmpty()) {
                videoUrl = videoUrl.replace("\\u002F", "/").replace("\\u0026", "&")
            }
        }
        if (audioMusikUrl.isEmpty()) {
            audioMusikUrl = extractMeta(html, "\"playUrl\":\"([^\"]+)\"") ?: ""
            if (audioMusikUrl.isNotEmpty()) {
                audioMusikUrl = audioMusikUrl.replace("\\u002F", "/").replace("\\u0026", "&")
            }
        }

        if (slides.isNotEmpty()) {
            videoType = "Tiktok Slide"
        }

        return ScrapedMetadata(
            title = title,
            uploader = uploader,
            thumbnail = thumbnail,
            duration = duration,
            viewCount = viewCount,
            likeCount = likeCount,
            extractor = "tiktok",
            videoType = videoType,
            tiktokVideoUrl = videoUrl,
            tiktokAudioMusikUrl = audioMusikUrl,
            tiktokSlides = slides
        )
    }

    private fun fetchHtml(urlString: String): String? {
        var currentUrl = urlString
        var connection: HttpURLConnection? = null
        var redirectsFollowed = 0
        val maxRedirects = 5

        while (redirectsFollowed < maxRedirects) {
            try {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                connection.setRequestProperty("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                connection.setRequestProperty("Sec-Ch-Ua-Mobile", "?0")
                connection.setRequestProperty("Sec-Ch-Ua-Platform", "\"Windows\"")
                connection.setRequestProperty("Sec-Fetch-Dest", "document")
                connection.setRequestProperty("Sec-Fetch-Mode", "navigate")
                connection.setRequestProperty("Sec-Fetch-Site", "none")
                connection.setRequestProperty("Sec-Fetch-User", "?1")
                connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
                if (currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be")) {
                    connection.setRequestProperty("Referer", "https://www.youtube.com")
                } else if (currentUrl.contains("tiktok.com")) {
                    connection.setRequestProperty("Referer", "https://www.tiktok.com/")
                }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                    responseCode == 307 || responseCode == 308) {
                    
                    val newUrl = connection.getHeaderField("Location")
                    if (newUrl != null && newUrl.isNotEmpty()) {
                        // Handle relative redirects
                        currentUrl = if (newUrl.startsWith("http")) {
                            newUrl
                        } else {
                            val uri = java.net.URI(currentUrl)
                            val portSuffix = if (uri.port != -1) ":${uri.port}" else ""
                            "${uri.scheme}://${uri.host}$portSuffix$newUrl"
                        }
                        redirectsFollowed++
                        connection.disconnect()
                        continue
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line).append("\n")
                    }
                    reader.close()
                    return response.toString()
                } else {
                    android.util.Log.e("SimpanVideoDebug", "HTTP error response code for $currentUrl: $responseCode")
                    return null
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpanVideoDebug", "fetchHtml error for $currentUrl", e)
                return null
            } finally {
                connection?.disconnect()
            }
        }
        return null
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
            "youtube\\.com/shorts/([^&#\n\r?]+)",
            "youtube\\.com/live/([^&#\n\r?]+)"
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

    private fun parseQualities(jsonStr: String): String {
        try {
            val afIdx = jsonStr.indexOf("\"adaptiveFormats\":[")
            if (afIdx == -1) return ""
            val start = afIdx + "\"adaptiveFormats\":[".length
            val end = jsonStr.indexOf("]", start)
            if (end == -1) return ""
            val afStr = jsonStr.substring(start, end)
            
            val blocks = afStr.split("\"itag\":")
            val map = mutableMapOf<String, Long>()
            
            val qlPattern = Pattern.compile("\"qualityLabel\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
            val clPattern = Pattern.compile("\"contentLength\"\\s*:\\s*\"?(\\d+)\"?", Pattern.CASE_INSENSITIVE)
            val mimePattern = Pattern.compile("\"mimeType\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
            
            for (i in 1 until blocks.size) {
                val block = blocks[i]
                
                val qlMatcher = qlPattern.matcher(block)
                val clMatcher = clPattern.matcher(block)
                val mimeMatcher = mimePattern.matcher(block)
                
                var ql: String? = null
                var cl: Long? = null
                var mime = ""
                
                if (qlMatcher.find()) {
                    ql = qlMatcher.group(1)
                }
                if (clMatcher.find()) {
                    cl = clMatcher.group(1)?.toLongOrNull()
                }
                if (mimeMatcher.find()) {
                    mime = mimeMatcher.group(1) ?: ""
                }
                
                if (ql != null && cl != null) {
                    val pIdx = ql.indexOf('p')
                    val cleanQl = if (pIdx != -1) ql.substring(0, pIdx + 1) else ql
                    val existing = map[cleanQl]
                    if (existing == null || mime.contains("video/mp4")) {
                        map[cleanQl] = cl
                    }
                } else if (cl != null && mime.contains("audio/")) {
                    val existingAudio = map["audio"]
                    if (existingAudio == null || cl > existingAudio) {
                        map["audio"] = cl
                    }
                }
            }
            return map.entries.joinToString(",") { "${it.key}:${it.value}" }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
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
