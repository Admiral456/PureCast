package com.admiral456.purecast

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.admiral456.purecast.device.ReceiverBrandClassifier
import com.admiral456.purecast.device.SenderDeviceInfo
import com.admiral456.purecast.dlna.DlnaController
import com.admiral456.purecast.dlna.DlnaCompatibility
import com.admiral456.purecast.dlna.MediaCompatibility
import com.admiral456.purecast.dlna.DlnaDevice
import com.admiral456.purecast.dlna.DlnaDiscovery
import com.admiral456.purecast.dlna.DlnaTransportInfo
import com.admiral456.purecast.dlna.DlnaPositionInfo
import com.admiral456.purecast.dlna.UpnpTime
import com.admiral456.purecast.media.MediaFile
import com.admiral456.purecast.media.MediaInspector
import com.admiral456.purecast.media.MediaKindFilter
import com.admiral456.purecast.media.MediaLibraryFilter
import com.admiral456.purecast.media.MediaThumbnailLoader
import com.admiral456.purecast.media.MediaTechnicalInfo
import com.admiral456.purecast.playback.PureCastPlaybackService
import com.admiral456.purecast.playback.PureCastRuntime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PureCastLight = lightColorScheme(
    primary = Color(0xFF4059D6),
    onPrimary = Color.White,
    secondary = Color(0xFF007F78),
    background = Color(0xFFF4F6FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EDF6),
    onSurface = Color(0xFF182033),
    onSurfaceVariant = Color(0xFF667085)
)

private val PureCastDark = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF17265C),
    secondary = Color(0xFF78D8CD),
    background = Color(0xFF0D1220),
    surface = Color(0xFF171E2E),
    surfaceVariant = Color(0xFF242D40),
    onSurface = Color(0xFFF3F5FA),
    onSurfaceVariant = Color(0xFFB0B9CA)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) PureCastDark else PureCastLight) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PureCastScreen()
                }
            }
        }
    }
}

@Composable
private fun PureCastScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val discovery = remember { DlnaDiscovery(appContext) }
    val controller = remember { DlnaController() }
    val senderInfo = remember { SenderDeviceInfo.current() }

    var devices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    val selectedFiles = remember { mutableStateListOf<MediaFile>() }
    val technicalInfo = remember { mutableStateMapOf<String, MediaTechnicalInfo>() }
    var mediaSearchQuery by remember { mutableStateOf("") }
    var mediaKindFilter by remember { mutableStateOf(MediaKindFilter.ALL) }
    val visibleFiles = MediaLibraryFilter.filter(selectedFiles, mediaSearchQuery, mediaKindFilter)
    var isScanning by remember { mutableStateOf(false) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Připoj telefon a televizi ke stejné Wi-Fi síti.") }
    var isError by remember { mutableStateOf(false) }
    var playingDevice by remember { mutableStateOf<DlnaDevice?>(null) }
    var playingFile by remember { mutableStateOf<MediaFile?>(null) }
    var browserUrl by remember { mutableStateOf<String?>(null) }
    var transportInfo by remember { mutableStateOf<DlnaTransportInfo?>(null) }
    var positionInfo by remember { mutableStateOf<DlnaPositionInfo?>(null) }
    var seekPreviewSeconds by remember { mutableStateOf(0f) }

    val activeDevice = devices.firstOrNull { it.id == selectedDeviceId }
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    appContext.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some picker implementations provide temporary grants only.
                } catch (_: IllegalArgumentException) {
                    // Some picker implementations do not offer persistable grants.
                }
            }
            scope.launch {
                val files = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri -> runCatching { MediaFile.fromUri(appContext, uri) }.getOrNull() }
                }
                technicalInfo.clear()
                selectedFiles.clear()
                selectedFiles.addAll(files)
                mediaSearchQuery = ""
                mediaKindFilter = MediaKindFilter.ALL
                isError = files.isEmpty()
                status = if (files.isEmpty()) {
                    "Vybrané soubory se nepodařilo načíst."
                } else {
                    "Načteno ${files.size} souborů. Náhledy a technické údaje se připravují na pozadí."
                }

                files.chunked(3).forEach { batch ->
                    val details = batch.map { file ->
                        async(Dispatchers.IO) {
                            runCatching {
                                MediaInspector.inspect(appContext, file)?.let { file.uri.toString() to it }
                            }.getOrNull()
                        }
                    }.awaitAll().filterNotNull()
                    details.forEach { (uri, info) -> technicalInfo[uri] = info }
                }
                if (files.isNotEmpty()) status = "Připraveno ${files.size} souborů. Vyber televizi a spusť přehrávání."
            }
        }
    }

    fun playOn(device: DlnaDevice, file: MediaFile) {
        scope.launch {
            busyLabel = file.displayName
            isError = false
            var compatibilityNote = "Kompatibilitu formátu se nepodařilo ověřit."
            status = "Připravuji přímý přenos: ${file.displayName}"
            try {
                val sinkProfiles = runCatching { controller.getSinkProtocolInfo(device) }.getOrNull()
                compatibilityNote = when (DlnaCompatibility.evaluate(file.mimeType, sinkProfiles)) {
                    MediaCompatibility.SUPPORTED_MIME_TYPE ->
                        "Televize deklaruje podporu MIME typu ${file.mimeType}; kodek, FPS a HDR tím nejsou ověřené."
                    MediaCompatibility.MIME_TYPE_NOT_ADVERTISED ->
                        "Televize tento MIME typ neuvádí mezi podporovanými. Zkouším Direct Play bez převodu."
                    MediaCompatibility.UNKNOWN ->
                        "Televize neposkytla seznam formátů; kompatibilita zůstává neověřená."
                }
                PureCastPlaybackService.start(appContext)
                val streamUrl = withContext(Dispatchers.IO) {
                    PureCastRuntime.getMediaServer(appContext).publish(file)
                }
                playingDevice = device
                playingFile = file
                browserUrl = PureCastRuntime.getMediaServer(appContext).browserPlayerUrl(streamUrl)
                transportInfo = null
                positionInfo = null
                seekPreviewSeconds = 0f
                controller.play(device, streamUrl, file)
                status = "Příkaz k přehrání byl odeslán do ${device.friendlyName}. " + compatibilityNote
            } catch (error: Exception) {
                playingDevice = null
                isError = true
                status = (error.message ?: "Přehrávání se nepodařilo.") +
                    " " + compatibilityNote + " Můžeš zkusit přehrávač v prohlížeči televize níže."
            } finally {
                busyLabel = null
            }
        }
    }

    fun prepareBrowser(file: MediaFile) {
        scope.launch {
            busyLabel = file.displayName
            isError = false
            status = "Připravuji odkaz pro prohlížeč televize…"
            try {
                PureCastPlaybackService.start(appContext)
                val streamUrl = withContext(Dispatchers.IO) {
                    PureCastRuntime.getMediaServer(appContext).publish(file)
                }
                browserUrl = PureCastRuntime.getMediaServer(appContext).browserPlayerUrl(streamUrl)
                playingDevice = null
                playingFile = file
                transportInfo = null
                status = "Odkaz pro TV prohlížeč je připraven. Otevři jej přímo na televizi."
            } catch (error: Exception) {
                isError = true
                status = error.message ?: "Odkaz pro televizi se nepodařilo připravit."
            } finally {
                busyLabel = null
            }
        }
    }

    fun runPlaybackAction(label: String, action: suspend (DlnaDevice) -> Unit) {
        val device = playingDevice ?: return
        scope.launch {
            busyLabel = label
            isError = false
            try {
                action(device)
                status = label
            } catch (error: Exception) {
                isError = true
                status = error.message ?: "Příkaz se nepodařil."
            } finally {
                busyLabel = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF192B63), Color(0xFF4059D6), Color(0xFF087F8C))))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.White.copy(alpha = 0.16f), shape = CircleShape) {
                            Text("PC", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("PURECAST", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Surface(color = Color.White.copy(alpha = 0.14f), shape = CircleShape) {
                            Text("DIRECT PLAY", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Tvoje média.\nNa velké obrazovce.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Přenášej fotky a videa přes domácí Wi-Fi bez automatické komprese.",
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Telefon: ${senderInfo.brand.displayName} · ${senderInfo.model} · Android ${senderInfo.androidVersion}",
                        color = Color.White.copy(alpha = 0.80f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderTag("Bez cloudu")
                        HeaderTag("Bez převodu")
                        HeaderTag("Lokální síť")
                    }
                }
            }
        }

        item {
            SectionTitle("Televize", "Vyhledej DLNA přehrávače v domácí síti.")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isScanning = true
                                isError = false
                                status = "Prohledávám síť přes UPnP / SSDP…"
                                try {
                                    val found = discovery.discover()
                                    devices = found
                                    if (selectedDeviceId == null || found.none { it.id == selectedDeviceId }) {
                                        selectedDeviceId = found.firstOrNull()?.id
                                    }
                                    status = if (found.isEmpty()) {
                                        "Nenalezen žádný ovladatelný DLNA přehrávač. Zkus režim prohlížeče televize níže."
                                    } else {
                                        "Nalezeno ${found.size} kompatibilních přehrávačů."
                                    }
                                } catch (error: Exception) {
                                    isError = true
                                    status = error.message ?: "Vyhledávání televizí selhalo."
                                } finally {
                                    isScanning = false
                                }
                            }
                        },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(18.dp).height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Vyhledávám…")
                        } else {
                            Text("Vyhledat televizory")
                        }
                    }

                    StatusMessage(text = status, error = isError)
                    if (devices.isNotEmpty()) {
                        devices.forEach { device ->
                            val isSelected = device.id == selectedDeviceId
                            val receiverBrand = ReceiverBrandClassifier.classify(
                                device.manufacturer,
                                device.modelName,
                                device.friendlyName
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(device.friendlyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                listOfNotNull(device.manufacturer, device.modelName).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                                                    .ifBlank { device.location.substringAfter("://").substringBefore('/') },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            StatusTag("Vybráno", MaterialTheme.colorScheme.primary)
                                        } else {
                                            TextButton(onClick = { selectedDeviceId = device.id }) { Text("Vybrat") }
                                        }
                                    }
                                    Text(
                                        "${receiverBrand.displayName} · ${receiverBrand.platformName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text("UPnP AVTransport · Direct Play", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("Fotky a videa", "Vyber originální soubory z telefonu. PureCast je nebude převádět.")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("＋  Vybrat fotky a videa")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vybrané soubory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        StatusTag("${selectedFiles.size} / 50", MaterialTheme.colorScheme.secondary)
                    }
                    if (selectedFiles.isNotEmpty()) {
                        OutlinedTextField(
                            value = mediaSearchQuery,
                            onValueChange = { mediaSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Hledat podle názvu") },
                            placeholder = { Text("Např. dovolená nebo video") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = mediaKindFilter == MediaKindFilter.ALL,
                                onClick = { mediaKindFilter = MediaKindFilter.ALL },
                                label = { Text("Vše") }
                            )
                            FilterChip(
                                selected = mediaKindFilter == MediaKindFilter.PHOTOS,
                                onClick = { mediaKindFilter = MediaKindFilter.PHOTOS },
                                label = { Text("Fotky") }
                            )
                            FilterChip(
                                selected = mediaKindFilter == MediaKindFilter.VIDEOS,
                                onClick = { mediaKindFilter = MediaKindFilter.VIDEOS },
                                label = { Text("Videa") }
                            )
                        }
                    }
                    if (selectedFiles.isEmpty()) {
                        EmptyState("Zatím tu nic není", "Vyber jednu nebo více fotek či videí a potom je odešli do televize.")
                    } else if (visibleFiles.isEmpty()) {
                        EmptyState("Nic nenalezeno", "Zkus upravit hledaný název nebo zvolit jiný filtr.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(visibleFiles, key = { it.uri.toString() }) { file ->
                                file ->
                                    Card(
                                        shape = RoundedCornerShape(15.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                MediaThumbnail(file)
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                            shape = RoundedCornerShape(11.dp)
                                                        ) {
                                                            Text(
                                                                if (file.isImage) "FOTO" else if (file.isVideo) "VIDEO" else "SOUBOR",
                                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        Spacer(Modifier.weight(1f))
                                                        TextButton(
                                                            onClick = {
                                                                selectedFiles.remove(file)
                                                                technicalInfo.remove(file.uri.toString())
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                                        ) { Text("Odebrat") }
                                                    }
                                                    Text(
                                                        file.displayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "${file.mimeType}  ·  ${formatSize(file.sizeBytes)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    technicalInfo[file.uri.toString()]?.let { info ->
                                                        val technicalParts = listOfNotNull(
                                                            info.resolutionLabel(),
                                                            info.frameRate?.let { String.format(Locale.US, "%.2f FPS", it) },
                                                            info.videoMimeType,
                                                            info.audioMimeTypes.firstOrNull(),
                                                            info.bitrateBitsPerSecond?.let(::formatBitrate),
                                                            info.durationMillis?.let { "Délka " + formatPlaybackTime(it / 1_000L) },
                                                            if (file.isVideo) "HDR " + (info.hdrLabel ?: "neověřeno") else null
                                                        )
                                                        if (technicalParts.isNotEmpty()) {
                                                            Text(
                                                                technicalParts.joinToString(" · "),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Button(
                                                    onClick = {
                                                        val device = activeDevice
                                                        if (device == null) {
                                                            isError = true
                                                            status = "Nejdříve vyber nebo vyhledej DLNA televizor."
                                                        } else {
                                                            playOn(device, file)
                                                        }
                                                    },
                                                    enabled = activeDevice != null && busyLabel == null,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                                ) {
                                                    if (busyLabel == file.displayName) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.width(17.dp).height(17.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                        Spacer(Modifier.width(7.dp))
                                                    }
                                                    Text("DLNA")
                                                }
                                                OutlinedButton(
                                                    onClick = { prepareBrowser(file) },
                                                    enabled = busyLabel == null,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                                ) {
                                                    Text("Prohlížeč TV")
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

        if (playingFile != null) {
            item {
                SectionTitle("Přehrávání", "Vyber režim přehrávání podle možností své televize.")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(playingFile?.displayName.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(playingDevice?.friendlyName ?: "Režim prohlížeče televize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (playingDevice != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { runPlaybackAction("Přehrávání pozastaveno.") { controller.pause(it) } },
                                enabled = busyLabel == null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) { Text("Pozastavit") }
                            OutlinedButton(
                                onClick = { runPlaybackAction("Pokračuji v přehrávání.") { controller.resume(it) } },
                                enabled = busyLabel == null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) { Text("Pokračovat") }
                            OutlinedButton(
                                onClick = {
                                    runPlaybackAction("Přehrávání zastaveno.") {
                                        controller.stop(it)
                                        PureCastPlaybackService.stop(appContext)
                                        playingFile = null
                                        browserUrl = null
                                        transportInfo = null
                                        positionInfo = null
                                    }
                                },
                                enabled = busyLabel == null,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) { Text("Zastavit") }
                        }
                        OutlinedButton(
                            onClick = {
                                runPlaybackAction("Stahuji stav z televize…") {
                                    transportInfo = controller.getTransportInfo(it)
                                    positionInfo = controller.getPositionInfo(it)
                                    seekPreviewSeconds = positionInfo?.positionSeconds?.toFloat() ?: 0f
                                    status = "Stav televize byl aktualizován."
                                }
                            },
                            enabled = busyLabel == null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Obnovit stav přehrávání") }
                        transportInfo?.let {
                            StatusMessage(
                                text = "Stav: ${it.state.replace('_', ' ')} · ${it.status}",
                                error = it.status != "OK" && it.status != "UNKNOWN"
                            )
                        }
                        positionInfo?.let { position ->
                            val duration = position.durationSeconds
                            if (duration != null && duration > 0L) {
                                val maxPosition = duration.toFloat().coerceAtLeast(1f)
                                Text(
                                    formatPlaybackTime(position.positionSeconds) + " / " + formatPlaybackTime(duration),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = seekPreviewSeconds.coerceIn(0f, maxPosition),
                                    onValueChange = { seekPreviewSeconds = it },
                                    valueRange = 0f..maxPosition,
                                    enabled = busyLabel == null,
                                    onValueChangeFinished = {
                                        val target = seekPreviewSeconds.toLong().coerceIn(0L, duration)
                                        runPlaybackAction("Posouvám video na " + formatPlaybackTime(target)) {
                                            controller.seekToSeconds(it, target)
                                            positionInfo = positionInfo?.copy(positionSeconds = target)
                                        }
                                    }
                                )
                            } else {
                                Text(
                                    "Televize neposkytla délku ani pozici videa.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    PureCastPlaybackService.stop(appContext)
                                    playingFile = null
                                    browserUrl = null
                                    transportInfo = null
                                    positionInfo = null
                                    status = "Přenos ukončen."
                                    isError = false
                                },
                                enabled = busyLabel == null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ukončit přenos")
                            }
                        }
                        browserUrl?.let { url ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Záložní režim přes prohlížeč TV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Pokud DLNA ovládání televize nefunguje, otevři na televizi webový prohlížeč a zadej tuto adresu:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("PureCast TV přehrávač", url))
                                        Toast.makeText(context, "Adresa zkopírována", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Zkopírovat adresu přehrávače") }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Původní kvalita je priorita", fontWeight = FontWeight.SemiBold)
                    Text(
                        "PureCast neposílá soubory přes cloud a nepřevádí je. Televizor ale musí sám podporovat daný formát, kodek, rozlišení, FPS a HDR. Když ho nepodporuje, aplikace kvalitu potichu nesníží.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(media: MediaFile) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = media.uri) {
        value = withContext(Dispatchers.IO) {
            MediaThumbnailLoader.load(context, media)
        }
    }
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = media.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                if (media.isVideo) "VIDEO" else if (media.isImage) "FOTO" else "SOUBOR",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeaderTag(text: String) {
    Surface(color = Color.White.copy(alpha = 0.14f), shape = CircleShape) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 2.dp, bottom = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusTag(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = CircleShape) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusMessage(text: String, error: Boolean) {
    val tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    Surface(color = tint.copy(alpha = 0.09f), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatPlaybackTime(seconds: Long?): String =
    seconds?.let(UpnpTime::formatSeconds) ?: "--:--:--"

private fun formatBitrate(bitsPerSecond: Long): String {
    val megabits = bitsPerSecond / 1_000_000.0
    return String.format(Locale.US, "%.1f Mbps", megabits)
}

private fun formatSize(bytes: Long?): String {
    if (bytes == null || bytes < 0L) return "Velikost neznámá"
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}
