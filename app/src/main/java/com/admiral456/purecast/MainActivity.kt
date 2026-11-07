package com.admiral456.purecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.admiral456.purecast.dlna.DlnaController
import com.admiral456.purecast.dlna.DlnaDevice
import com.admiral456.purecast.dlna.DlnaDiscovery
import com.admiral456.purecast.media.MediaFile
import com.admiral456.purecast.server.LocalMediaServer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PureCastScreen()
                }
            }
        }
    }
}

@Composable
private fun PureCastScreen() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val discovery = remember { DlnaDiscovery(context) }
    val controller = remember { DlnaController() }
    val mediaServer = remember { LocalMediaServer(context) }

    var devices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    val selectedFiles = remember { mutableStateListOf<MediaFile>() }
    var isScanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Připoj telefon a televizi ke stejné Wi-Fi síti.") }

    DisposableEffect(mediaServer) {
        onDispose { mediaServer.close() }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val files = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri -> MediaFile.fromUri(context, uri) }
                }
                selectedFiles.clear()
                selectedFiles.addAll(files)
                status = if (files.isEmpty()) {
                    "Vybrané soubory se nepodařilo načíst."
                } else {
                    "Vybráno souborů: " + files.size
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PureCast",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Přehrávání originálních souborů na televizi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Televize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isScanning = true
                                status = "Vyhledávám DLNA televizory v místní síti…"
                                try {
                                    devices = discovery.discover()
                                    status = if (devices.isEmpty()) {
                                        "Nenašel jsem DLNA přehrávač. Zkontroluj stejnou Wi-Fi a to, zda TV nabízí DLNA renderer."
                                    } else {
                                        "Nalezeno zařízení: " + devices.size
                                    }
                                } catch (error: Exception) {
                                    status = "Vyhledávání selhalo: " + (error.message ?: error.javaClass.simpleName)
                                } finally {
                                    isScanning = false
                                }
                            }
                        },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Vyhledat televizory")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(status, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Fotky a videa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Vybrat fotky a videa")
                    }
                    if (selectedFiles.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Zatím nemáš vybraný žádný soubor.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        selectedFiles.forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        file.mimeType + " • " + formatSize(file.sizeBytes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { selectedFiles.removeAt(index) }) {
                                    Text("Odebrat")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "3. Odeslání do televize",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (devices.isEmpty()) {
            item {
                Text(
                    "Nejdříve vyhledej kompatibilní DLNA televizor.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            items(devices, key = { it.id }) { device ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(device.friendlyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val detail = listOfNotNull(device.manufacturer, device.modelName)
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString(" • ")
                        if (detail.isNotBlank()) {
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "UPnP AVTransport",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val file = selectedFiles.firstOrNull()
                                if (file == null) {
                                    status = "Nejdříve vyber alespoň jednu fotografii nebo video."
                                } else {
                                    scope.launch {
                                        status = "Připravuji přímé přehrávání…"
                                        try {
                                            val mediaUrl = withContext(Dispatchers.IO) {
                                                mediaServer.publish(file)
                                            }
                                            controller.play(device, mediaUrl, file)
                                            status = "Příkaz k přehrání byl odeslán. TV musí podporovat formát souboru."
                                        } catch (error: Exception) {
                                            status = "Přehrávání se nepodařilo: " + (error.message ?: error.javaClass.simpleName)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedFiles.isNotEmpty()
                        ) {
                            Text("Přehrát první vybraný soubor")
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(2.dp))
            Text(
                "PureCast soubor nepřevádí. Pokud TV nepodporuje jeho kodek nebo kontejner, přehrávání může selhat místo automatického snížení kvality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long?): String {
    if (bytes == null || bytes < 0L) return "Velikost neznámá"
    if (bytes < 1024L) return bytes.toString() + " B"
    val megabytes = bytes / (1024.0 * 1024.0)
    return String.format(Locale.getDefault(), "%.1f MB", megabytes)
}
