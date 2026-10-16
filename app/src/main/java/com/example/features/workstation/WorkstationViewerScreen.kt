package com.example.features.workstation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.VaultFile
import com.example.core.sync.CloudUploadPipeline
import com.example.features.studio.AppLanguage
import com.example.ui.components.AnimatedCloudDownloadButton
import com.example.ui.components.ProminentDownloadPill
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkstationViewerScreen(
    file: VaultFile,
    language: AppLanguage,
    onBack: () -> Unit,
    onSaveNewVersion: (fileId: String, newContent: String) -> Unit,
    onShareTicket: (VaultFile) -> Unit,
    onDownloadToDevice: (VaultFile) -> Unit = {},
    onShareDecrypted: (VaultFile) -> Unit = {},
    onEnsureFileCached: ((VaultFile, (Boolean) -> Unit) -> Unit)? = null
) {
    val isHindi = language == AppLanguage.HI
    val context = LocalContext.current

    // Locate local cached file
    val cachedFile = remember(file.id) {
        CloudUploadPipeline.getLocalCachedFile(context, file.id, file.name)
    }

    var isFileAvailableLocally by remember { mutableStateOf(cachedFile.exists() && cachedFile.length() > 0) }
    var isDownloadingToCache by remember { mutableStateOf(!isFileAvailableLocally) }

    // If file is not yet cached locally, request download to cache
    LaunchedEffect(file.id) {
        if (!cachedFile.exists() || cachedFile.length() == 0L) {
            isDownloadingToCache = true
            onEnsureFileCached?.invoke(file) { success ->
                isDownloadingToCache = false
                isFileAvailableLocally = success || (cachedFile.exists() && cachedFile.length() > 0)
            }
        } else {
            isFileAvailableLocally = true
            isDownloadingToCache = false
        }
    }

    // Determine workstation format
    val isImage = file.mimeType.startsWith("image/") || file.extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    val isVideo = file.mimeType.startsWith("video/") || file.extension in listOf("mp4", "mkv", "webm", "3gp", "mov")
    val isAudio = file.mimeType.startsWith("audio/") || file.extension in listOf("mp3", "wav", "m4a", "ogg", "flac")
    val isPdf = file.mimeType == "application/pdf" || file.extension == "pdf"
    val isArchive = file.extension in listOf("zip", "apk", "jar", "tar", "gz")
    val isTextOrCode = !isImage && !isVideo && !isAudio && !isPdf && !isArchive

    // Real Text / Code Editor State
    var fileContentText by remember(file.id, isFileAvailableLocally) {
        mutableStateOf(
            if (isFileAvailableLocally && cachedFile.exists() && isTextOrCode) {
                try {
                    cachedFile.readText()
                } catch (e: Exception) {
                    "Error reading text content: ${e.message}"
                }
            } else {
                "Loading unencrypted text content..."
            }
        )
    }
    var codeSearchQuery by remember { mutableStateOf("") }
    var isSavedSnackbarVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "${file.mimeType} • ${formatBytes(file.totalSizeBytes)}",
                            fontSize = 11.sp,
                            color = CyanNeon
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("workstation_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Direct Download button with animation
                    AnimatedCloudDownloadButton(
                        onClick = { onDownloadToDevice(file) },
                        label = if (isHindi) "डाउनलोड" else "Download",
                        modifier = Modifier.padding(end = 4.dp),
                        isCompact = true,
                        testTag = "workstation_download_button"
                    )

                    // Share button
                    IconButton(onClick = { onShareDecrypted(file) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share File", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (isTextOrCode) {
                        Button(
                            onClick = {
                                onSaveNewVersion(file.id, fileContentText)
                                isSavedSnackbarVisible = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "सहेजें" else "Save", fontSize = 12.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // 1. REAL IMAGE VIEWER
                isImage -> {
                    RealImageViewer(
                        context = context,
                        file = file,
                        cachedFile = cachedFile,
                        isHindi = isHindi
                    )
                }

                // 2. REAL VIDEO PLAYER
                isVideo -> {
                    RealVideoPlayer(
                        context = context,
                        file = file,
                        cachedFile = cachedFile,
                        isHindi = isHindi
                    )
                }

                // 3. REAL AUDIO PLAYER
                isAudio -> {
                    RealAudioPlayer(
                        context = context,
                        file = file,
                        cachedFile = cachedFile,
                        isHindi = isHindi
                    )
                }

                // 4. REAL PDF VIEWER (native PdfRenderer)
                isPdf -> {
                    RealPdfDocumentViewer(
                        context = context,
                        file = file,
                        cachedFile = cachedFile,
                        isHindi = isHindi
                    )
                }

                // 5. REAL ZIP / ARCHIVE INSPECTOR
                isArchive -> {
                    RealArchiveInspector(
                        cachedFile = cachedFile,
                        fileName = file.name,
                        isHindi = isHindi
                    )
                }

                // 6. REAL TEXT & CODE STUDIO
                else -> {
                    RealTextCodeStudio(
                        text = fileContentText,
                        onTextChange = { fileContentText = it },
                        searchQuery = codeSearchQuery,
                        onSearchChange = { codeSearchQuery = it },
                        fileName = file.name,
                        isHindi = isHindi
                    )
                }
            }

            // Floating Quick Download Pill (Mobile restoration)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                ProminentDownloadPill(
                    onClick = { onDownloadToDevice(file) },
                    title = if (isHindi) "फ़ाइल डाउनलोड करें" else "Download ${file.name}",
                    subtitle = if (isHindi) "ओरिजिनल फॉर्मेट में अपने डिवाइस में सेव करें" else "Save original ${file.extension.uppercase()} to Downloads"
                )
            }

            if (isSavedSnackbarVisible) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    action = {
                        TextButton(onClick = { isSavedSnackbarVisible = false }) {
                            Text("OK", color = CyanNeon)
                        }
                    }
                ) {
                    Text(if (isHindi) "फ़ाइल सुरक्षित रूप से सहेज ली गई है!" else "File updated and saved to Cloud!")
                }
            }
        }
    }
}

/**
 * 1. REAL IMAGE VIEWER: Renders true image bitmap with zero compression loss
 */
@Composable
private fun RealImageViewer(
    context: Context,
    file: VaultFile,
    cachedFile: File,
    isHindi: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17)),
        contentAlignment = Alignment.Center
    ) {
        if (cachedFile.exists() && cachedFile.length() > 0) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(cachedFile)
                    .crossfade(true)
                    .build(),
                contentDescription = file.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )
        } else if (file.microThumbWebp != null && file.microThumbWebp.isNotEmpty()) {
            val bitmap = remember(file.id) {
                android.graphics.BitmapFactory.decodeByteArray(file.microThumbWebp, 0, file.microThumbWebp.size)?.asImageBitmap()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = CyanNeon)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isHindi) "इमेज लोड हो रही है..." else "Loading Image...",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * 2. REAL VIDEO PLAYER: Native VideoView with live playback controls
 */
@Composable
private fun RealVideoPlayer(
    context: Context,
    file: VaultFile,
    cachedFile: File,
    isHindi: Boolean
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isVideoReady by remember { mutableStateOf(false) }

    DisposableEffect(cachedFile.absolutePath) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (cachedFile.exists() && cachedFile.length() > 0) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setVideoPath(cachedFile.absolutePath)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            isVideoReady = true
                            start()
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = CyanNeon)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isHindi) "वीडियो लोड हो रहा है..." else "Streaming Video...",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * 3. REAL AUDIO PLAYER: Native MediaPlayer playing audio tracks
 */
@Composable
private fun RealAudioPlayer(
    context: Context,
    file: VaultFile,
    cachedFile: File,
    isHindi: Boolean
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }

    LaunchedEffect(cachedFile.absolutePath) {
        if (cachedFile.exists()) {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(cachedFile.absolutePath)
                    prepare()
                    durationMs = duration
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(IndigoHyper.copy(alpha = 0.2f))
                .border(2.dp, CyanNeon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(56.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(file.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text(formatBytes(file.totalSizeBytes), fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val mp = mediaPlayer ?: return@IconButton
                    if (isPlaying) {
                        mp.pause()
                        isPlaying = false
                    } else {
                        mp.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(CyanNeon)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * 4. REAL PDF VIEWER: Native PdfRenderer rendering real document pages as Bitmaps
 */
@Composable
private fun RealPdfDocumentViewer(
    context: Context,
    file: VaultFile,
    cachedFile: File,
    isHindi: Boolean
) {
    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoadingPdf by remember { mutableStateOf(true) }
    var pdfError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cachedFile.absolutePath) {
        if (!cachedFile.exists() || cachedFile.length() == 0L) {
            isLoadingPdf = false
            pdfError = "PDF file is downloading or not yet cached."
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                val pages = mutableListOf<Bitmap>()

                // Render up to 20 pages
                val maxPages = pageCount.coerceAtMost(20)
                for (i in 0 until maxPages) {
                    val page = renderer.openPage(i)
                    val width = (page.width * 1.5f).toInt()
                    val height = (page.height * 1.5f).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pages.add(bitmap)
                }
                renderer.close()
                pfd.close()
                renderedPages = pages
                isLoadingPdf = false
            } catch (e: Exception) {
                isLoadingPdf = false
                pdfError = "PDF render error: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingPdf) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = IndigoHyper)
                Spacer(modifier = Modifier.height(10.dp))
                Text(if (isHindi) "पीडीएफ पेज रेंडर हो रहे हैं..." else "Rendering PDF Pages...", color = Color.DarkGray)
            }
        } else if (renderedPages.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(renderedPages) { index, pageBitmap ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 4.dp,
                        color = Color.White
                    ) {
                        Column {
                            Image(
                                bitmap = pageBitmap.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                contentScale = ContentScale.FillWidth
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Page ${index + 1} of ${renderedPages.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(pdfError ?: "PDF Document ready", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}

/**
 * 5. REAL ARCHIVE INSPECTOR: Reads real contents of ZIP or APK
 */
@Composable
private fun RealArchiveInspector(
    cachedFile: File,
    fileName: String,
    isHindi: Boolean
) {
    val archiveEntries = remember(cachedFile.absolutePath) {
        val list = mutableListOf<String>()
        if (cachedFile.exists()) {
            try {
                java.io.FileInputStream(cachedFile).use { fis ->
                    ZipInputStream(fis).use { zis ->
                        var entry = zis.nextEntry
                        var count = 0
                        while (entry != null && count < 50) {
                            list.add("${entry.name} (${formatBytes(entry.size.coerceAtLeast(0L))})")
                            zis.closeEntry()
                            entry = zis.nextEntry
                            count++
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isHindi) "संग्रह फाइलें (Archive Entries):" else "Archive Contents ($fileName):",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (archiveEntries.isEmpty()) {
            Text(if (isHindi) "संग्रह खोला जा रहा है..." else "Inspecting archive contents...", fontSize = 12.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(archiveEntries) { entryName ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(entryName, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * 6. REAL TEXT & CODE STUDIO: Reads and allows editing actual text content
 */
@Composable
private fun RealTextCodeStudio(
    text: String,
    onTextChange: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    fileName: String,
    isHindi: Boolean
) {
    val lines = remember(text) { text.lines() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(if (isHindi) "पाठ या कोड में खोजें..." else "Search in text / code...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${lines.size} L",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Full Editable text area
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
