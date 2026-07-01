package com.simpanvideo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDLRequest
import android.content.Context
import java.io.File
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.io.FileOutputStream
import android.content.Intent
import android.net.Uri
import android.app.PendingIntent
import android.widget.RemoteViews
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Shadow
import org.json.JSONObject
import org.json.JSONArray

val BgColor = Color(0xFF0D1117)
val SurfaceColor = Color(0xFF161B22)
val BorderColor = Color(0x33FFFFFF)
val CyanWarm = Color(0xFF00E5FF)
val TextMuted = Color(0xFF8B949E)
val GradientPrimary = Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF00B0FF)))

val PoppinsFont = FontFamily(
    Font(R.font.font_regular, FontWeight.Normal),
    Font(R.font.font_medium, FontWeight.Medium),
    Font(R.font.font_semibold, FontWeight.SemiBold),
    Font(R.font.font_bold, FontWeight.Bold),
    Font(R.font.font_extrabold, FontWeight.ExtraBold),
    Font(R.font.font_black, FontWeight.Black)
)

fun Modifier.glow(color: Color = CyanWarm, alpha: Float = 0.2f, radius: Float = 20f) = this.drawBehind {
    drawCircle(color = color.copy(alpha = alpha), radius = size.width / 2 + radius, center = Offset(size.width / 2, size.height / 2))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CompletedDownloadDatabase.init(this)
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        setContent {
            val customTypography = Typography(
                bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = PoppinsFont),
                bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFont),
                bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = PoppinsFont),
                labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = PoppinsFont),
                labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = PoppinsFont),
                labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = PoppinsFont),
                titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = PoppinsFont),
                titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = PoppinsFont),
                titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = PoppinsFont)
            )
            MaterialTheme(
                colorScheme = darkColorScheme(background = BgColor, surface = SurfaceColor, primary = CyanWarm, onPrimary = Color.Black),
                typography = customTypography
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = BgColor) {
                    ProvideTextStyle(TextStyle(fontFamily = PoppinsFont)) {
                        val engineStatus by EngineState.status.collectAsState()
                        
                        when (engineStatus) {
                            EngineStatus.INITIALIZING -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val context = LocalContext.current
                                        val imageLoader = remember(context) {
                                            ImageLoader.Builder(context).components {
                                                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                                            }.build()
                                        }
                                        AsyncImage(
                                            model = R.drawable.engine,
                                            imageLoader = imageLoader,
                                            contentDescription = "Loading Engine",
                                            modifier = Modifier.size(60.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Menyiapkan Mesin...", fontFamily = PoppinsFont, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(if(EngineState.errorMessage.isBlank()) "Memuat dependensi inti" else EngineState.errorMessage, fontFamily = PoppinsFont, color = TextMuted, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            EngineStatus.ERROR -> {
                                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(painter = painterResource(id = android.R.drawable.ic_dialog_alert), contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Gagal Menyalakan Mesin", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(EngineState.errorMessage, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            EngineStatus.READY -> {
                                App()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.getStringExtra("action") == "open_downloads") {
            NavigationState.currentTab = Tab.DOWNLOADS
        }
    }
}

object NavigationState {
    var currentTab by mutableStateOf(Tab.HOME)
}

data class ActiveDownload(
    val id: String,
    val title: String,
    val progress: Int,
    val speed: String,
    val eta: String,
    val typeLabel: String,
    val isPaused: Boolean = false
)

object DownloadTracker {
    val activeDownloads = mutableStateListOf<ActiveDownload>()
}

data class CompletedDownload(
    val id: String,
    val title: String,
    val platform: String,
    val uploader: String,
    val fileSize: String,
    val duration: String,
    val localThumbnailPath: String,
    val localFilePath: String,
    val timestamp: Long
)

object CompletedDownloadDatabase {
    private const val FILE_NAME = "completed_downloads_v2.json"
    val completedList = mutableStateListOf<CompletedDownload>()

    fun init(context: Context) {
        completedList.clear()
        completedList.addAll(loadFromStorage(context))
    }

    fun add(context: Context, download: CompletedDownload) {
        completedList.removeIf { it.id == download.id }
        completedList.add(0, download)
        saveToStorage(context, completedList)
    }

    fun remove(context: Context, id: String) {
        completedList.removeIf { it.id == id }
        saveToStorage(context, completedList)
    }

    private fun loadFromStorage(context: Context): List<CompletedDownload> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText()
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<CompletedDownload>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CompletedDownload(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        platform = obj.getString("platform"),
                        uploader = obj.getString("uploader"),
                        fileSize = obj.getString("fileSize"),
                        duration = obj.getString("duration"),
                        localThumbnailPath = obj.getString("localThumbnailPath"),
                        localFilePath = obj.getString("localFilePath"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveToStorage(context: Context, list: List<CompletedDownload>) {
        val file = File(context.filesDir, FILE_NAME)
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("platform", item.platform)
                obj.put("uploader", item.uploader)
                obj.put("fileSize", item.fileSize)
                obj.put("duration", item.duration)
                obj.put("localThumbnailPath", item.localThumbnailPath)
                obj.put("localFilePath", item.localFilePath)
                obj.put("timestamp", item.timestamp)
                arr.put(obj)
            }
            file.writeText(arr.toString())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

enum class Tab { HOME, DOWNLOADS, SETTINGS }

@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 90.dp)) {
            Crossfade(
                targetState = NavigationState.currentTab, 
                label = "TabTransition",
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) { tab ->
                when (tab) {
                    Tab.HOME -> HomeScreen()
                    Tab.DOWNLOADS -> DownloadsScreen()
                    Tab.SETTINGS -> SettingsScreen()
                }
            }
        }
        BottomNav(currentTab = NavigationState.currentTab, onTabSelected = { NavigationState.currentTab = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

fun getUniqueTitle(downloadDir: File, safeTitle: String): String {
    var candidate = safeTitle
    var counter = 1
    val files = downloadDir.listFiles() ?: emptyArray()
    while (files.any { file -> 
        file.nameWithoutExtension.equals(candidate, ignoreCase = true)
    }) {
        candidate = "$safeTitle ($counter)"
        counter++
    }
    return candidate
}

fun downloadThumbnailLocally(context: Context, id: String, thumbnailUrl: String): String {
    if (thumbnailUrl.isEmpty()) return ""
    val thumbnailsDir = File(context.filesDir, "thumbnails")
    if (!thumbnailsDir.exists()) {
        thumbnailsDir.mkdirs()
    }
    val outputFile = File(thumbnailsDir, "thumb_$id.jpg")
    var connection: HttpURLConnection? = null
    try {
        val url = URL(thumbnailUrl)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.doInput = true
        connection.connect()
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            return outputFile.absolutePath
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.disconnect()
    }
    return ""
}

fun fetchBitmapFromUrl(imageUrl: String): Bitmap? {
    if (imageUrl.isEmpty()) return null
    var connection: HttpURLConnection? = null
    return try {
        val url = URL(imageUrl)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.doInput = true
        connection.connect()
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            BitmapFactory.decodeStream(connection.inputStream)
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        connection?.disconnect()
    }
}

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSecond.toDouble() / (1024 * 1024))
        bytesPerSecond >= 1024 -> String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSecond.toDouble() / 1024)
        else -> "$bytesPerSecond B/s"
    }
}

fun formatEta(seconds: Long): String {
    return when {
        seconds >= 3600 -> String.format(java.util.Locale.US, "%d jam %d mnt", seconds / 3600, (seconds % 3600) / 60)
        seconds >= 60 -> String.format(java.util.Locale.US, "%d mnt %d dtk", seconds / 60, seconds % 60)
        else -> "$seconds dtk"
    }
}

fun startDownload(
    context: Context, 
    videoUrl: String, 
    formatId: String, 
    title: String, 
    isAudio: Boolean = false,
    thumbnailUrl: String = "",
    uploader: String = "",
    durationSeconds: Int = 0,
    platform: String = ""
) {
    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
    val safeTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val uniqueTitleBase = getUniqueTitle(downloadDir, safeTitle)
    val displayTitle = uniqueTitleBase.replace("_", " ")

    Toast.makeText(context, "Memulai unduhan: $displayTitle", Toast.LENGTH_SHORT).show()
    
    val activeItem = ActiveDownload(
        id = uniqueTitleBase,
        title = displayTitle,
        progress = 0,
        speed = "0 KB/s",
        eta = "Menghubungkan...",
        typeLabel = if (isAudio) "audio" else "video"
    )
    DownloadTracker.activeDownloads.add(activeItem)

    CoroutineScope(Dispatchers.IO).launch {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "simpanvideo_downloads"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Unduhan", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationId = uniqueTitleBase.hashCode()
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val thumbnailBitmap = fetchBitmapFromUrl(thumbnailUrl)

        val remoteViews = RemoteViews(context.packageName, R.layout.custom_notification)
        remoteViews.setTextViewText(R.id.notification_title, displayTitle)
        remoteViews.setTextViewText(R.id.notification_creator, uploader.ifEmpty { "SimpanVideo" })
        remoteViews.setProgressBar(R.id.notification_progress_bar, 100, 0, false)
        remoteViews.setTextViewText(R.id.notification_progress_text, "0%")
        if (thumbnailBitmap != null) {
            remoteViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
        } else {
            remoteViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        notificationManager.notify(notificationId, builder.build())

        try {
            val request = YoutubeDLRequest(videoUrl)
            request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                request.addOption("--referer", "https://www.youtube.com")
            } else if (videoUrl.contains("tiktok.com") || videoUrl.contains("tiktokcdn") || videoUrl.contains("akamaized.net")) {
                request.addOption("--referer", "https://www.tiktok.com/")
            }

            if (isAudio) {
                request.addOption("-f", "bestaudio/best")
                request.addOption("--extract-audio")
                request.addOption("--audio-format", "mp3")
            } else {
                if (formatId != "best") {
                    request.addOption("-f", formatId)
                }
            }
            
            request.addOption("-o", "${downloadDir.absolutePath}/$uniqueTitleBase.%(ext)s")
            request.addOption("-4")
            request.addOption("--no-check-formats")
            request.addOption("--cache-dir", "${context.cacheDir.absolutePath}/yt-dlp-cache")

            var lastProgress = -1
            var isAudioPhase = isAudio
            com.yausername.youtubedl_android.YoutubeDL.getInstance().execute(request, "task_${System.currentTimeMillis()}") { progress, _, line ->
                val currentProgress = progress.toInt()
                if (!isAudioPhase && currentProgress < lastProgress && lastProgress > 80) {
                    isAudioPhase = true
                }

                if (currentProgress != lastProgress && currentProgress >= 0) {
                    lastProgress = currentProgress

                    val speedRegex = """\bat\s+(\S+)""".toRegex()
                    val speedMatch = speedRegex.find(line ?: "")
                    val speed = speedMatch?.groupValues?.get(1) ?: ""

                    val etaRegex = """ETA\s+(\S+)""".toRegex()
                    val etaMatch = etaRegex.find(line ?: "")
                    val eta = etaMatch?.groupValues?.get(1) ?: ""

                    val isMerging = line?.contains("merger", ignoreCase = true) == true || 
                                    line?.contains("merging", ignoreCase = true) == true

                    val phasePrefix = when {
                        isMerging -> "Menggabungkan..."
                        isAudioPhase -> "Audio"
                        else -> "Video"
                    }

                    val index = DownloadTracker.activeDownloads.indexOfFirst { it.id == uniqueTitleBase }
                    if (index != -1) {
                        DownloadTracker.activeDownloads[index] = DownloadTracker.activeDownloads[index].copy(
                            progress = currentProgress,
                            speed = if (speed.isNotEmpty()) "$phasePrefix ($speed)" else phasePrefix,
                            eta = eta
                        )
                    }

                    val updateViews = RemoteViews(context.packageName, R.layout.custom_notification)
                    updateViews.setTextViewText(R.id.notification_title, displayTitle)
                    val creatorText = when {
                        isMerging -> "Menggabungkan Video & Audio..."
                        speed.isNotEmpty() && eta.isNotEmpty() -> "Mengunduh... ($speed) · ETA $eta"
                        speed.isNotEmpty() -> "Mengunduh... ($speed)"
                        else -> uploader.ifEmpty { "SimpanVideo" }
                    }
                    updateViews.setTextViewText(R.id.notification_creator, creatorText)
                    updateViews.setProgressBar(R.id.notification_progress_bar, 100, currentProgress, false)
                    updateViews.setTextViewText(R.id.notification_progress_text, "$currentProgress%")
                    if (thumbnailBitmap != null) {
                        updateViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
                    } else {
                        updateViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
                    }

                    val updateBuilder = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setCustomContentView(updateViews)
                        .setCustomBigContentView(updateViews)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)

                    notificationManager.notify(notificationId, updateBuilder.build())
                }
            }

            DownloadTracker.activeDownloads.removeIf { it.id == uniqueTitleBase }

            val localThumbPath = downloadThumbnailLocally(context, uniqueTitleBase, thumbnailUrl)

            val files = downloadDir.listFiles() ?: emptyArray()
            val downloadedFile = files.firstOrNull { it.nameWithoutExtension.equals(uniqueTitleBase, ignoreCase = true) }
            val sizeStr = if (downloadedFile != null) {
                val sizeBytes = downloadedFile.length()
                String.format(java.util.Locale.US, "%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
            } else "0.0 MB"
            val localFilePath = downloadedFile?.absolutePath ?: ""

            val completed = CompletedDownload(
                id = uniqueTitleBase,
                title = displayTitle,
                platform = platform.ifEmpty { if (videoUrl.contains("tiktok.com")) "TikTok" else "YouTube" },
                uploader = uploader.ifEmpty { "Unknown" },
                fileSize = sizeStr,
                duration = formatDuration(durationSeconds),
                localThumbnailPath = localThumbPath,
                localFilePath = localFilePath,
                timestamp = System.currentTimeMillis()
            )
            CompletedDownloadDatabase.add(context, completed)

            if (downloadedFile != null) {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(downloadedFile.absolutePath), null, null)
            }

            val finishedViews = RemoteViews(context.packageName, R.layout.custom_notification)
            finishedViews.setTextViewText(R.id.notification_title, displayTitle)
            finishedViews.setTextViewText(R.id.notification_creator, "Selesai diunduh!")
            finishedViews.setProgressBar(R.id.notification_progress_bar, 100, 100, false)
            finishedViews.setTextViewText(R.id.notification_progress_text, "100%")
            if (thumbnailBitmap != null) {
                finishedViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
            } else {
                finishedViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
            }

            val finishedBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setCustomContentView(finishedViews)
                .setCustomBigContentView(finishedViews)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)

            notificationManager.notify(notificationId, finishedBuilder.build())
            withContext(Dispatchers.Main) { Toast.makeText(context, "Selesai mengunduh $displayTitle", Toast.LENGTH_LONG).show() }
        } catch (e: Exception) {
            e.printStackTrace()
            DownloadTracker.activeDownloads.removeIf { it.id == uniqueTitleBase }

            val errorViews = RemoteViews(context.packageName, R.layout.custom_notification)
            errorViews.setTextViewText(R.id.notification_title, displayTitle)
            errorViews.setTextViewText(R.id.notification_creator, "Gagal mengunduh!")
            errorViews.setProgressBar(R.id.notification_progress_bar, 100, 0, false)
            errorViews.setTextViewText(R.id.notification_progress_text, "Error")
            if (thumbnailBitmap != null) {
                errorViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
            } else {
                errorViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
            }

            val errorBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setCustomContentView(errorViews)
                .setCustomBigContentView(errorViews)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)

            notificationManager.notify(notificationId, errorBuilder.build())
            withContext(Dispatchers.Main) { Toast.makeText(context, "Gagal mengunduh: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
}

fun downloadDirectFile(
    context: Context, 
    fileUrl: String, 
    fileName: String,
    thumbnailUrl: String = "",
    uploader: String = "",
    durationSeconds: Int = 0,
    platform: String = "",
    typeLabel: String = ""
) {
    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
    val safeTitle = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val uniqueTitleBase = getUniqueTitle(downloadDir, safeTitle)
    val displayTitle = uniqueTitleBase.replace("_", " ")

    val isAudio = fileUrl.contains(".mp3", ignoreCase = true) || fileName.contains("audio", ignoreCase = true) || typeLabel == "audio"
    val isVideo = fileUrl.contains(".mp4", ignoreCase = true) || fileName.contains("video", ignoreCase = true) || typeLabel == "video"
    val resolvedTypeLabel = when {
        isAudio -> "audio"
        isVideo -> "video"
        else -> "gambar"
    }

    Toast.makeText(context, "Memulai unduhan $resolvedTypeLabel...", Toast.LENGTH_SHORT).show()

    val activeItem = ActiveDownload(
        id = uniqueTitleBase,
        title = displayTitle,
        progress = 0,
        speed = "0 KB/s",
        eta = "Menghubungkan...",
        typeLabel = resolvedTypeLabel
    )
    DownloadTracker.activeDownloads.add(activeItem)

    CoroutineScope(Dispatchers.IO).launch {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "simpanvideo_downloads"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Unduhan", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationId = uniqueTitleBase.hashCode()
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val thumbnailBitmap = fetchBitmapFromUrl(thumbnailUrl)

        val remoteViews = RemoteViews(context.packageName, R.layout.custom_notification)
        remoteViews.setTextViewText(R.id.notification_title, displayTitle)
        remoteViews.setTextViewText(R.id.notification_creator, uploader.ifEmpty { "SimpanVideo" })
        remoteViews.setProgressBar(R.id.notification_progress_bar, 100, 0, false)
        remoteViews.setTextViewText(R.id.notification_progress_text, "0%")
        if (thumbnailBitmap != null) {
            remoteViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
        } else {
            remoteViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        notificationManager.notify(notificationId, builder.build())

        var connection: HttpURLConnection? = null
        try {
            var currentUrl = fileUrl
            var redirectsFollowed = 0
            val maxRedirects = 5
            var responseCode = -1
            
            while (redirectsFollowed < maxRedirects) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                if (currentUrl.contains("tiktok.com") || currentUrl.contains("tiktokcdn") || currentUrl.contains("akamaized.net")) {
                    connection.setRequestProperty("Referer", "https://www.tiktok.com/")
                }
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                    responseCode == 307 || responseCode == 308) {
                    
                    val newUrl = connection.getHeaderField("Location")
                    if (newUrl != null && newUrl.isNotEmpty()) {
                        currentUrl = newUrl
                        redirectsFollowed++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }

            if (responseCode == HttpURLConnection.HTTP_OK && connection != null) {
                val inputStream = connection.inputStream
                val contentType = connection.contentType
                val ext = when {
                    contentType != null && contentType.contains("image/png") -> "png"
                    contentType != null && contentType.contains("image/webp") -> "webp"
                    contentType != null && (contentType.contains("audio/mpeg") || contentType.contains("audio/mp3")) -> "mp3"
                    contentType != null && contentType.contains("audio/") -> "mp3"
                    contentType != null && contentType.contains("video/mp4") -> "mp4"
                    contentType != null && contentType.contains("video/") -> "mp4"
                    fileUrl.contains(".mp3", ignoreCase = true) -> "mp3"
                    fileUrl.contains(".mp4", ignoreCase = true) -> "mp4"
                    else -> if (isAudio) "mp3" else if (isVideo) "mp4" else "jpg"
                }
                val outputFile = File(downloadDir, "$uniqueTitleBase.$ext")
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L
                val fileLength = connection.contentLengthLong

                var lastProgress = -1
                var lastUpdateTime = System.currentTimeMillis()
                var lastBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    if (fileLength > 0) {
                        val progress = ((totalBytesRead * 100) / fileLength).toInt()
                        val now = System.currentTimeMillis()
                        val timeDiff = now - lastUpdateTime
                        
                        if (progress != lastProgress && (timeDiff >= 1000 || progress == 100)) {
                            lastProgress = progress
                            
                            val bytesDiff = totalBytesRead - lastBytesRead
                            val speedBps = if (timeDiff > 0) (bytesDiff * 1000) / timeDiff else 0L
                            val speedText = formatSpeed(speedBps)
                            val etaText = if (speedBps > 0) {
                                val secondsRemaining = (fileLength - totalBytesRead) / speedBps
                                formatEta(secondsRemaining)
                            } else ""
                            
                            lastUpdateTime = now
                            lastBytesRead = totalBytesRead

                            val index = DownloadTracker.activeDownloads.indexOfFirst { it.id == uniqueTitleBase }
                            if (index != -1) {
                                DownloadTracker.activeDownloads[index] = DownloadTracker.activeDownloads[index].copy(
                                    progress = progress,
                                    speed = speedText,
                                    eta = etaText
                                )
                            }

                            val updateViews = RemoteViews(context.packageName, R.layout.custom_notification)
                            updateViews.setTextViewText(R.id.notification_title, displayTitle)
                            updateViews.setTextViewText(R.id.notification_creator, "Mengunduh... ($speedText) · ETA $etaText")
                            updateViews.setProgressBar(R.id.notification_progress_bar, 100, progress, false)
                            updateViews.setTextViewText(R.id.notification_progress_text, "$progress%")
                            if (thumbnailBitmap != null) {
                                updateViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
                            } else {
                                updateViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
                            }

                            val updateBuilder = NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setCustomContentView(updateViews)
                                .setCustomBigContentView(updateViews)
                                .setContentIntent(pendingIntent)
                                .setPriority(NotificationCompat.PRIORITY_LOW)
                                .setOngoing(true)

                            notificationManager.notify(notificationId, updateBuilder.build())
                        }
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                DownloadTracker.activeDownloads.removeIf { it.id == uniqueTitleBase }

                val localThumbPath = downloadThumbnailLocally(context, uniqueTitleBase, thumbnailUrl)

                val sizeBytes = outputFile.length()
                val sizeStr = String.format(java.util.Locale.US, "%.1f MB", sizeBytes.toDouble() / (1024 * 1024))

                val completed = CompletedDownload(
                    id = uniqueTitleBase,
                    title = displayTitle,
                    platform = platform.ifEmpty { if (fileUrl.contains("tiktok")) "TikTok" else "YouTube" },
                    uploader = uploader.ifEmpty { "Unknown" },
                    fileSize = sizeStr,
                    duration = if (resolvedTypeLabel == "gambar") "00:00" else formatDuration(durationSeconds),
                    localThumbnailPath = localThumbPath,
                    localFilePath = outputFile.absolutePath,
                    timestamp = System.currentTimeMillis()
                )
                CompletedDownloadDatabase.add(context, completed)

                android.media.MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null, null)

                val finishedViews = RemoteViews(context.packageName, R.layout.custom_notification)
                finishedViews.setTextViewText(R.id.notification_title, displayTitle)
                finishedViews.setTextViewText(R.id.notification_creator, "Selesai diunduh!")
                finishedViews.setProgressBar(R.id.notification_progress_bar, 100, 100, false)
                finishedViews.setTextViewText(R.id.notification_progress_text, "100%")
                if (thumbnailBitmap != null) {
                    finishedViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
                } else {
                    finishedViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
                }

                val finishedBuilder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setCustomContentView(finishedViews)
                    .setCustomBigContentView(finishedViews)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(false)

                notificationManager.notify(notificationId, finishedBuilder.build())
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Selesai mengunduh $displayTitle", Toast.LENGTH_SHORT).show()
                }
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DownloadTracker.activeDownloads.removeIf { it.id == uniqueTitleBase }

            val errorViews = RemoteViews(context.packageName, R.layout.custom_notification)
            errorViews.setTextViewText(R.id.notification_title, displayTitle)
            errorViews.setTextViewText(R.id.notification_creator, "Gagal mengunduh!")
            errorViews.setProgressBar(R.id.notification_progress_bar, 100, 0, false)
            errorViews.setTextViewText(R.id.notification_progress_text, "Error")
            if (thumbnailBitmap != null) {
                errorViews.setImageViewBitmap(R.id.notification_bg, thumbnailBitmap)
            } else {
                errorViews.setImageViewResource(R.id.notification_bg, android.R.color.black)
            }

            val errorBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setCustomContentView(errorViews)
                .setCustomBigContentView(errorViews)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)

            notificationManager.notify(notificationId, errorBuilder.build())
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal mengunduh $displayTitle: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            connection?.disconnect()
        }
    }
}

fun formatDuration(durationSeconds: Int): String {
    if (durationSeconds >= 3600) {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}

fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

fun mapToVideoInfo(scraped: ScrapedMetadata): VideoInfo {
    val info = VideoInfo()
    
    // Log all fields of VideoInfo to see if the names match
    try {
        val fieldsList = info.javaClass.declaredFields.map { "${it.name} (${it.type.name})" }.joinToString(", ")
        android.util.Log.d("SimpanVideoDebug", "VideoInfo declared fields: $fieldsList")
    } catch (e: Exception) {
        android.util.Log.e("SimpanVideoDebug", "Failed to print fields", e)
    }

    fun setPrivateField(obj: Any, fieldName: String, value: Any?) {
        try {
            var clazz: Class<*>? = obj.javaClass
            var field: java.lang.reflect.Field? = null
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName)
                    break
                } catch (e: NoSuchFieldException) {
                    clazz = clazz.superclass
                }
            }
            if (field == null) {
                val snakeCaseName = fieldName.replace(Regex("([A-Z])")) { "_" + it.value.lowercase() }
                if (snakeCaseName != fieldName) {
                    clazz = obj.javaClass
                    while (clazz != null) {
                        try {
                            field = clazz.getDeclaredField(snakeCaseName)
                            break
                        } catch (e: NoSuchFieldException) {
                            clazz = clazz.superclass
                        }
                    }
                }
            }
            if (field == null) {
                android.util.Log.w("SimpanVideoDebug", "Field '$fieldName' (and its snake_case fallback) not found in class hierarchy")
                return
            }
            field.isAccessible = true
            val fieldType = field.type
            when {
                fieldType == String::class.java -> field.set(obj, value?.toString())
                fieldType == Int::class.java || fieldType == java.lang.Integer::class.java -> {
                    field.set(obj, value?.toString()?.toIntOrNull() ?: 0)
                }
                fieldType == Long::class.java || fieldType == java.lang.Long::class.java -> {
                    field.set(obj, value?.toString()?.toLongOrNull() ?: 0L)
                }
                else -> field.set(obj, value)
            }
            android.util.Log.d("SimpanVideoDebug", "Successfully set field '$fieldName' to '$value'")
        } catch (e: Exception) {
            android.util.Log.e("SimpanVideoDebug", "Failed to set field '$fieldName'", e)
        }
    }
    
    setPrivateField(info, "title", scraped.title)
    setPrivateField(info, "uploader", scraped.uploader)
    setPrivateField(info, "thumbnail", scraped.thumbnail)
    setPrivateField(info, "duration", scraped.duration)
    setPrivateField(info, "viewCount", scraped.viewCount)
    setPrivateField(info, "likeCount", scraped.likeCount)
    return info
}

fun parseQualitiesString(str: String): Map<String, Long> {
    if (str.isEmpty()) return emptyMap()
    val map = mutableMapOf<String, Long>()
    try {
        val parts = str.split(",")
        for (part in parts) {
            val subparts = part.split(":")
            if (subparts.size == 2) {
                val quality = subparts[0]
                val size = subparts[1].toLongOrNull() ?: 0L
                map[quality] = size
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return map
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var mediaInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var openOptions by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var parsedQualities by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var videoType by remember { mutableStateOf("reguler") }
    var tiktokVideoUrl by remember { mutableStateOf("") }
    var tiktokAudioMusikUrl by remember { mutableStateOf("") }
    var tiktokSlides by remember { mutableStateOf<List<String>>(emptyList()) }
    
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    fun analyzeUrl() {
        if (urlInput.isBlank()) return
        isLoading = true
        errorMessage = null
        mediaInfo = null
        parsedQualities = emptyMap()
        videoType = "reguler"
        tiktokVideoUrl = ""
        tiktokAudioMusikUrl = ""
        tiktokSlides = emptyList()
        openOptions = false
        
        coroutineScope.launch {
            try {
                var tempQualities = emptyMap<String, Long>()
                var tempType = "reguler"
                var tempTiktokVideoUrl = ""
                var tempTiktokAudioMusikUrl = ""
                var tempTiktokSlides = emptyList<String>()
                val info = withContext(Dispatchers.IO) {
                    val isYoutube = urlInput.contains("youtube.com", ignoreCase = true) || urlInput.contains("youtu.be", ignoreCase = true)
                    val isTikTok = urlInput.contains("tiktok.com", ignoreCase = true)
                    
                    var result: VideoInfo? = null
                    
                    if (isYoutube) {
                        try {
                            val scraped = MetadataScraper.fetchYoutubeMetadata(urlInput)
                            if (scraped != null) {
                                tempQualities = parseQualitiesString(scraped.qualities)
                                tempType = scraped.videoType
                                result = mapToVideoInfo(scraped)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else if (isTikTok) {
                        try {
                            val scraped = MetadataScraper.fetchTikTokMetadata(urlInput)
                            if (scraped != null) {
                                tempType = scraped.videoType
                                tempTiktokVideoUrl = scraped.tiktokVideoUrl
                                tempTiktokAudioMusikUrl = scraped.tiktokAudioMusikUrl
                                tempTiktokSlides = scraped.tiktokSlides
                                result = mapToVideoInfo(scraped)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    if (result == null) {
                        val req = YoutubeDLRequest(urlInput)
                        req.addOption("--no-playlist")
                        req.addOption("--no-warnings")
                        req.addOption("--compat-options", "no-youtube-unavailable-videos")
                        req.addOption("-R", "1") // Retries = 1
                        req.addOption("--socket-timeout", "5") // Timeout 5 detik
                        req.addOption("--no-check-certificate") // Lewati validasi SSL
                        req.addOption("--no-check-certificates")
                        req.addOption("--flat-playlist") // Cegah load detail item playlist
                        req.addOption("--skip-download")
                        req.addOption("--quiet")
                        req.addOption("-4") // Paksa IPv4 untuk menghindari delay DNS IPv6 pada Android
                        req.addOption("--no-check-formats") // Jangan cek URL format via HTTP HEAD
                        req.addOption("--cache-dir", "${context.cacheDir.absolutePath}/yt-dlp-cache") // Aktifkan caching player JS
                        result = YoutubeDL.getInstance().getInfo(req)
                    }
                    
                    result!!
                }
                parsedQualities = tempQualities
                videoType = tempType
                tiktokVideoUrl = tempTiktokVideoUrl
                tiktokAudioMusikUrl = tempTiktokAudioMusikUrl
                tiktokSlides = tempTiktokSlides
                mediaInfo = info
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Gagal: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 20.dp, end = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).glow(alpha = 0.3f).clip(RoundedCornerShape(16.dp)).background(GradientPrimary), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("SimpanVideo", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White, letterSpacing = (-0.5).sp)
                    Text("Unduh tanpa batas", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PLATFORM DIDUKUNG", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, letterSpacing = 0.5.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val platforms = listOf(
                Triple("TikTok", R.drawable.ic_tiktok, Color(0xFF000000)), // Box Hitam pekat
                Triple("YouTube", R.drawable.ic_youtube, Color.White)      // Box Putih
            )
            platforms.forEach { (name, resId, boxColor) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp).clip(RoundedCornerShape(24.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(24.dp)).clickable { }.padding(vertical = 10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(boxColor), contentAlignment = Alignment.Center) {
                        val iconTint = if (name == "TikTok") Color.White else Color.Unspecified
                        Icon(painterResource(resId), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(32.dp)).padding(16.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(BgColor).border(1.dp, BorderColor, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // BasicTextField asli yang bisa diketik
                    BasicTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = PoppinsFont),
                        cursorBrush = SolidColor(CyanWarm),
                        singleLine = true,
                        maxLines = 1,
                        decorationBox = { innerTextField ->
                            if (urlInput.isEmpty()) {
                                Text("Tempel link video di sini…", color = TextMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            innerTextField()
                        }
                    )
                    
                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x3300E5FF)).clickable { 
                        clipboardManager.getText()?.text?.let { 
                            urlInput = it 
                            analyzeUrl()
                        }
                    }.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text("PASTE", color = CyanWarm, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).glow(alpha = 0.2f).background(GradientPrimary).clickable { analyzeUrl() }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menganalisa...", color = Color(0xFF050505), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analisa & Unduh", color = Color(0xFF050505), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp))
        }

        if (mediaInfo != null) {
            val info = mediaInfo!!
            val isTikTok = videoType.startsWith("Tiktok", ignoreCase = true)
            
            // Logika Hapus Tagar untuk YouTube
            val finalTitle = if (isTikTok) {
                info.title ?: "Tanpa Judul"
            } else {
                info.title?.replace(Regex("#\\S+"), "")?.trim() ?: "Tanpa Judul"
            }
            
            val durationStr = formatDuration(info.duration)
            val viewsStr = formatNumber(info.viewCount?.toString()?.toLongOrNull() ?: 0L)
            val likesStr = formatNumber(info.likeCount?.toString()?.toLongOrNull() ?: 0L)

            Box(modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(32.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(32.dp))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    
                    // Thumbnail Dinamis
                    val isPortrait = isTikTok
                    Box(
                        modifier = Modifier
                            .then(if (isPortrait) Modifier.fillMaxWidth(0.7f).padding(top = 16.dp).clip(RoundedCornerShape(20.dp)).aspectRatio(9f/16f) else Modifier.fillMaxWidth().aspectRatio(16f/9f))
                            .background(Color(0xFF2A1C30))
                    ) {
                        // Gambar Asli dari yt-dlp
                        AsyncImage(
                            model = info.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                        Row(modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x80000000)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(if (isTikTok) R.drawable.ic_tiktok_mini else R.drawable.ic_youtube), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTikTok) {
                                    if (videoType == "Tiktok Slide") "TikTok Slide" else "TikTok Video"
                                } else {
                                    when (videoType) {
                                        "short" -> "YouTube Short"
                                        "live" -> "YouTube Live"
                                        else -> "YouTube"
                                    }
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        if (isTikTok && videoType == "Tiktok Slide" && tiktokSlides.isNotEmpty()) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x80000000)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("${tiktokSlides.size} Slide", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else if (info.duration > 0) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x80000000)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(durationStr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        Box(modifier = Modifier.align(Alignment.Center).size(64.dp).glow(Color.White, 0.1f, 10f).clip(CircleShape).background(Color.White).clickable {
                            val playUrl = if (isTikTok && tiktokVideoUrl.isNotEmpty()) tiktokVideoUrl else urlInput
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tidak dapat memutar video", Toast.LENGTH_SHORT).show()
                            }
                        }, contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_play_large), contentDescription = null, tint = CyanWarm, modifier = Modifier.size(24.dp))
                        }
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        // Judul Video
                        Text(finalTitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(info.uploader ?: "Unknown", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("$viewsStr views · $likesStr likes", color = TextMuted, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        val rotation by animateFloatAsState(if (openOptions) 180f else 0f)
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgColor).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).clickable { openOptions = !openOptions }.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Pilih kualitas unduhan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = rotation })
                        }

                        AnimatedVisibility(visible = openOptions, enter = expandVertically(spring(stiffness = Spring.StiffnessLow)), exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow))) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                val context = LocalContext.current
                                
                                if (isTikTok) {
                                    if (videoType == "Tiktok Slide") {
                                        // 1. Image Slides
                                        tiktokSlides.forEachIndexed { idx, slideUrl ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x80161B22)).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x22FE2C55)), contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_slide), contentDescription = null, tint = Color(0xFFFE2C55), modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Slide ${idx + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Unduh Gambar", color = TextMuted, fontSize = 11.sp)
                                                }
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GradientPrimary).clickable { 
                                                    downloadDirectFile(
                                                        context = context, 
                                                        fileUrl = slideUrl, 
                                                        fileName = "$finalTitle - Slide ${idx + 1}",
                                                        thumbnailUrl = info.thumbnail ?: "",
                                                        uploader = info.uploader ?: "",
                                                        durationSeconds = 0,
                                                        platform = "TikTok",
                                                        typeLabel = "gambar"
                                                    )
                                                }, contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        // 2. Audio Musik
                                        if (tiktokAudioMusikUrl.isNotEmpty()) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x80161B22)).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33A020F0)), contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_audio), contentDescription = null, tint = CyanWarm, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("audio musik", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("mp3", color = TextMuted, fontSize = 11.sp)
                                                }
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GradientPrimary).clickable { 
                                                    downloadDirectFile(
                                                        context = context, 
                                                        fileUrl = tiktokAudioMusikUrl, 
                                                        fileName = "$finalTitle - Audio Musik",
                                                        thumbnailUrl = info.thumbnail ?: "",
                                                        uploader = info.uploader ?: "",
                                                        durationSeconds = 0,
                                                        platform = "TikTok",
                                                        typeLabel = "audio"
                                                    )
                                                }, contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        // TikTok Video
                                        // 1. Video
                                        if (tiktokVideoUrl.isNotEmpty()) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x80161B22)).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x2200E5FF)), contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_video), contentDescription = null, tint = CyanWarm, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("video", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Kualitas Standar", color = TextMuted, fontSize = 11.sp)
                                                }
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GradientPrimary).clickable { 
                                                    downloadDirectFile(
                                                        context = context, 
                                                        fileUrl = tiktokVideoUrl, 
                                                        fileName = finalTitle,
                                                        thumbnailUrl = info.thumbnail ?: "",
                                                        uploader = info.uploader ?: "",
                                                        durationSeconds = info.duration,
                                                        platform = "TikTok",
                                                        typeLabel = "video"
                                                    )
                                                }, contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        // 2. Audio Konten
                                        if (tiktokVideoUrl.isNotEmpty()) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x80161B22)).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33A020F0)), contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_audio), contentDescription = null, tint = CyanWarm, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("audio konten", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("mp3", color = TextMuted, fontSize = 11.sp)
                                                }
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GradientPrimary).clickable { 
                                                    startDownload(
                                                        context = context, 
                                                        videoUrl = tiktokVideoUrl, 
                                                        formatId = "bestaudio", 
                                                        title = finalTitle, 
                                                        isAudio = true,
                                                        thumbnailUrl = info.thumbnail ?: "",
                                                        uploader = info.uploader ?: "",
                                                        durationSeconds = info.duration,
                                                        platform = "TikTok"
                                                    )
                                                }, contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        // 3. Audio Musik
                                        if (tiktokAudioMusikUrl.isNotEmpty()) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x80161B22)).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33A020F0)), contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_audio), contentDescription = null, tint = CyanWarm, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("audio musik", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("mp3", color = TextMuted, fontSize = 11.sp)
                                                }
                                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GradientPrimary).clickable { 
                                                    downloadDirectFile(
                                                        context = context, 
                                                        fileUrl = tiktokAudioMusikUrl, 
                                                        fileName = "$finalTitle - Audio Musik",
                                                        thumbnailUrl = info.thumbnail ?: "",
                                                        uploader = info.uploader ?: "",
                                                        durationSeconds = 0,
                                                        platform = "TikTok",
                                                        typeLabel = "audio"
                                                    )
                                                }, contentAlignment = Alignment.Center) {
                                                    Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // YouTube Options
                                    data class DlOption(val label: String, val desc: String, val kind: String, val formatId: String, val height: Int = 0)
                                    
                                    val downloadOptions = listOf(
                                        DlOption("2160p (4K)", "Ultra HD", "video", "bestvideo[height<=2160]+bestaudio/best", 2160),
                                        DlOption("1440p (2K)", "Quad HD", "video", "bestvideo[height<=1440]+bestaudio/best", 1440),
                                        DlOption("1080p", "Full HD", "video", "bestvideo[height<=1080]+bestaudio/best", 1080),
                                        DlOption("720p", "High Definition", "video", "bestvideo[height<=720]+bestaudio/best", 720),
                                        DlOption("480p", "Standard", "video", "bestvideo[height<=480]+bestaudio/best", 480),
                                        DlOption("360p", "Low", "video", "bestvideo[height<=360]+bestaudio/best", 360),
                                        DlOption("240p", "Sangat Rendah", "video", "bestvideo[height<=240]+bestaudio/best", 240),
                                        DlOption("Audio", "MP3", "audio", "bestaudio/best", 0)
                                    )

                                    val filteredOptions = if (parsedQualities.isNotEmpty()) {
                                        downloadOptions.filter { option ->
                                            option.kind == "audio" ||
                                            parsedQualities.containsKey("${option.height}p")
                                        }
                                    } else {
                                        downloadOptions
                                    }

                                    val finalOptions = filteredOptions.map { option ->
                                        val sizeBytes = when {
                                            option.kind == "video" -> {
                                                val vSize = parsedQualities["${option.height}p"] ?: 0L
                                                val aSize = parsedQualities["audio"] ?: 0L
                                                if (vSize > 0L) vSize + aSize else 0L
                                            }
                                            option.label == "Audio" -> {
                                                parsedQualities["audio"] ?: 0L
                                            }
                                            else -> 0L
                                        }
                                        
                                        if (sizeBytes > 0L) {
                                            val sizeMb = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
                                            option.copy(desc = "${option.desc} · $sizeMb")
                                        } else {
                                            option
                                        }
                                    }

                                    finalOptions.forEachIndexed { index, option ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x80161B22)).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).clickable { }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (option.kind == "audio") Color(0x33A020F0) else Color(0x2200E5FF)), contentAlignment = Alignment.Center) {
                                                Icon(painterResource(if (option.kind == "audio") R.drawable.ic_audio else R.drawable.ic_video), contentDescription = null, tint = CyanWarm, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(option.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text(option.desc, color = TextMuted, fontSize = 11.sp)
                                            }
                                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(GradientPrimary).clickable { 
                                                startDownload(
                                                    context = context, 
                                                    videoUrl = urlInput, 
                                                    formatId = option.formatId, 
                                                    title = finalTitle, 
                                                    isAudio = (option.kind == "audio"),
                                                    thumbnailUrl = info.thumbnail ?: "",
                                                    uploader = info.uploader ?: "",
                                                    durationSeconds = info.duration,
                                                    platform = "YouTube"
                                                )
                                            }, contentAlignment = Alignment.Center) {
                                                Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen() {
    var filter by remember { mutableStateOf("Semua") }
    val context = LocalContext.current
    
    var itemToDelete by remember { mutableStateOf<CompletedDownload?>(null) }
    
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Riwayat", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = PoppinsFont) },
            text = { Text("Hapus riwayat unduhan '${itemToDelete?.title}'? File asli tidak akan dihapus dari HP Anda.", color = TextMuted, fontFamily = PoppinsFont) },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { completed ->
                        CompletedDownloadDatabase.remove(context, completed.id)
                    }
                    itemToDelete = null
                }) {
                    Text("Hapus", color = Color.Red, fontWeight = FontWeight.Bold, fontFamily = PoppinsFont)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal", color = Color.White, fontFamily = PoppinsFont)
                }
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 20.dp, end = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Unduhan", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(SurfaceColor).border(1.dp, BorderColor, CircleShape).clickable {
                Toast.makeText(context, "Gunakan tombol hapus di setiap unduhan", Toast.LENGTH_SHORT).show()
            }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf("Semua", "Video", "Audio", "Gambar").forEach { f ->
                val isSelected = filter == f
                val bgBrush: Brush = if(isSelected) GradientPrimary else SolidColor(SurfaceColor)
                Box(modifier = Modifier.padding(end = 10.dp).height(40.dp).clip(CircleShape).background(bgBrush).border(if(!isSelected) 1.dp else 0.dp, BorderColor, CircleShape).clickable { filter = f }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text(f, color = if(isSelected) Color(0xFF050505) else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        
        if (DownloadTracker.activeDownloads.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanWarm))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SEDANG BERJALAN · ${DownloadTracker.activeDownloads.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            DownloadTracker.activeDownloads.forEach { active ->
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(24.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(24.dp)).padding(14.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x2200E5FF)), contentAlignment = Alignment.Center) {
                                val iconRes = when (active.typeLabel) {
                                    "audio" -> R.drawable.ic_audio
                                    "gambar" -> R.drawable.ic_slide
                                    else -> R.drawable.ic_video
                                }
                                Icon(painterResource(iconRes), contentDescription = null, tint = CyanWarm, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(active.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val descText = if (active.speed.isNotEmpty() && active.eta.isNotEmpty()) {
                                    "${active.speed} · ETA ${active.eta}"
                                } else if (active.speed.isNotEmpty()) {
                                    active.speed
                                } else active.eta
                                Text(descText.ifEmpty { "Menghubungkan..." }, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(BgColor)) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(active.progress / 100f).background(GradientPrimary))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${active.progress}%", color = CyanWarm, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("SELESAI · OFFLINE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        
        val completedList = CompletedDownloadDatabase.completedList
        val filteredList = when (filter) {
            "Video" -> completedList.filter { it.localFilePath.endsWith(".mp4", ignoreCase = true) || it.localFilePath.contains("video", ignoreCase = true) }
            "Audio" -> completedList.filter { it.localFilePath.endsWith(".mp3", ignoreCase = true) || it.localFilePath.contains("audio", ignoreCase = true) }
            "Gambar" -> completedList.filter { it.localFilePath.endsWith(".png", ignoreCase = true) || it.localFilePath.endsWith(".jpg", ignoreCase = true) || it.localFilePath.endsWith(".jpeg", ignoreCase = true) || it.localFilePath.endsWith(".webp", ignoreCase = true) || it.localFilePath.contains("gambar", ignoreCase = true) }
            else -> completedList
        }
        
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("Belum ada berkas terunduh", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                filteredList.forEach { completed ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                            .clickable {
                                try {
                                    val file = File(completed.localFilePath)
                                    if (file.exists()) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "com.simpanvideo.app.provider",
                                            file
                                        )
                                        val mimeType = context.contentResolver.getType(uri) ?: when {
                                            completed.localFilePath.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
                                            completed.localFilePath.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                                            completed.localFilePath.endsWith(".png", ignoreCase = true) -> "image/png"
                                            completed.localFilePath.endsWith(".jpg", ignoreCase = true) || completed.localFilePath.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                                            else -> "*/*"
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "Berkas tidak ditemukan di HP", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Gagal memutar berkas", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        if (completed.localThumbnailPath.isNotEmpty() && File(completed.localThumbnailPath).exists()) {
                            AsyncImage(
                                model = File(completed.localThumbnailPath),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x33000000),
                                            Color(0xD9000000)
                                        )
                                    )
                                )
                        )

                        val shadowStyle = TextStyle(
                            fontFamily = PoppinsFont,
                            color = Color.White,
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                        val shadowMutedStyle = TextStyle(
                            fontFamily = PoppinsFont,
                            color = Color(0xFFE0E0E0),
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(1.5f, 1.5f),
                                blurRadius = 3f
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = completed.title,
                                    style = shadowStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x80000000))
                                            .border(0.5.dp, BorderColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = completed.platform,
                                            style = shadowStyle.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = completed.uploader,
                                        style = shadowMutedStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${completed.fileSize} · ${if(completed.duration == "00:00") "Gambar" else completed.duration}",
                                    style = shadowMutedStyle.copy(fontSize = 10.sp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GradientPrimary)
                                        .clickable {
                                            try {
                                                val file = File(completed.localFilePath)
                                                if (file.exists()) {
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "com.simpanvideo.app.provider",
                                                        file
                                                    )
                                                    val mimeType = context.contentResolver.getType(uri) ?: when {
                                                        completed.localFilePath.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
                                                        completed.localFilePath.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                                                        completed.localFilePath.endsWith(".png", ignoreCase = true) -> "image/png"
                                                        completed.localFilePath.endsWith(".jpg", ignoreCase = true) || completed.localFilePath.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                                                        else -> "*/*"
                                                    }
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, mimeType)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } else {
                                                    Toast.makeText(context, "Berkas tidak ditemukan", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Gagal memutar berkas", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_play),
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FF0000))
                                        .clickable {
                                            itemToDelete = completed
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    var mode by remember { mutableStateOf("queue") }
    val infiniteTransition = rememberInfiniteTransition(label = "saweria")
    val colorOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "saweria_color")
    val saweriaBrush = Brush.linearGradient(colors = listOf(Color(0xFFE5B05C), Color(0xFFFFD54F), Color(0xFFD69E4A)), start = Offset(0f, 0f), end = Offset(1000f * colorOffset, 1000f * colorOffset))
    
    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 20.dp, end = 20.dp)) {
        Text("Pengaturan", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
        Text("Sesuaikan SimpanVideo sesukamu", fontSize = 12.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(saweriaBrush).clickable { }.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Column {
                Box(modifier = Modifier.clip(CircleShape).background(Color(0x4D000000)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("DUKUNG DEVELOPER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Traktir secangkir kopi ☕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text("SimpanVideo gratis & tanpa iklan. Bantu jaga tetap hidup lewat Saweria.", color = Color(0xE6FFFFFF), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.clip(CircleShape).background(Color(0x33FFFFFF)).clickable { }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_coffee), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Dukung di Saweria", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("UNDUHAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(24.dp))) {
            Column {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Mode unduhan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgColor).border(1.dp, BorderColor, RoundedCornerShape(16.dp)).padding(6.dp)) {
                        val qActive = mode == "queue"
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(qActive) GradientPrimary else SolidColor(Color.Transparent)).clickable { mode = "queue" }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("Antrian", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if(qActive) Color(0xFF050505) else TextMuted)
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(!qActive) GradientPrimary else SolidColor(Color.Transparent)).clickable { mode = "bulk" }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("Bulk Paralel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if(!qActive) Color(0xFF050505) else TextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(if (mode == "queue") "Satu unduhan dijalankan secara bergiliran." else "Beberapa unduhan berjalan bersamaan.", fontSize = 11.sp, color = TextMuted)
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-retry saat gagal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Box(modifier = Modifier.width(44.dp).height(26.dp).clip(CircleShape).background(GradientPrimary).padding(2.dp)) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF050505)).align(Alignment.CenterEnd))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("PENYIMPANAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(24.dp))) {
            Column {
                Column(modifier = Modifier.padding(18.dp).clickable { }) {
                    Text("Folder default", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgColor).border(1.dp, BorderColor, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("/Download/SimpanVideo", fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
                        Text("Ubah", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyanWarm)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Bersihkan cache", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("42 MB", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("TENTANG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(24.dp))) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cek pembaruan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("v1.0.0", fontSize = 12.sp, color = CyanWarm, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Kebijakan privasi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Beri rating ⭐", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BottomNav(currentTab: Tab, onTabSelected: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, BgColor, BgColor))).padding(horizontal = 20.dp, vertical = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().clip(CircleShape).background(Color(0xE6161B22)).border(1.dp, BorderColor, CircleShape).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val tabs = listOf(Triple(Tab.HOME, "Beranda", Pair(R.drawable.ic_home_filled, R.drawable.ic_home_outline)), Triple(Tab.DOWNLOADS, "Unduhan", Pair(R.drawable.ic_downloads_filled, R.drawable.ic_downloads_outline)), Triple(Tab.SETTINGS, "Pengaturan", Pair(R.drawable.ic_settings_filled, R.drawable.ic_settings_outline)))
            tabs.forEach { (tab, label, icons) ->
                val active = currentTab == tab
                Box(modifier = Modifier.height(52.dp).then(if(active) Modifier.weight(1f) else Modifier.width(52.dp)).clip(CircleShape).background(if(active) GradientPrimary else SolidColor(Color.Transparent)).clickable { onTabSelected(tab) }, contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(if(active) icons.first else icons.second), contentDescription = null, tint = if(active) Color.Black else TextMuted, modifier = Modifier.size(24.dp))
                        AnimatedVisibility(visible = active, enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(), exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()) {
                            Row {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(label, color = Color(0xFF050505), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
