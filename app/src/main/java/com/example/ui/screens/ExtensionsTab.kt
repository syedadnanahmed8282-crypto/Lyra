package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.extension.ExtensionPlugin
import com.example.data.extension.OnlineSong
import com.example.model.Song
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

fun OnlineSong.toSong(): Song {
    val numericId = id.hashCode().toLong().let { if (it < 0) -it else it }
    return Song(
        id = numericId,
        title = title,
        artist = artist,
        album = album,
        albumId = extensionId.hashCode().toLong(),
        duration = durationMs,
        path = streamUrl,
        contentUri = Uri.parse(streamUrl),
        albumArtUri = if (artworkUrl.isNotBlank()) Uri.parse(artworkUrl) else null,
        folderName = "Online ($extensionName)",
        folderPath = "Online Extensions"
    )
}

@Composable
fun ExtensionsTab(
    plugins: List<ExtensionPlugin>,
    searchResults: List<OnlineSong>,
    isSearching: Boolean,
    themeColor: Color,
    onSearchQuery: (String) -> Unit,
    onInstallFromUrl: (String, (Boolean, String?) -> Unit) -> Unit,
    onInstallFromCode: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onInstallFromLocalUri: (Uri, String?, (Boolean, String?) -> Unit) -> Unit,
    onTogglePlugin: (String) -> Unit,
    onDeletePlugin: (String) -> Unit,
    onPlayOnlineSong: (Song) -> Unit
) {
    val context = LocalContext.current
    var subTabSelected by remember { mutableStateOf(0) } // 0: Search Online, 1: Installed Plugins
    var searchQuery by remember { mutableStateOf("") }

    var showInstallDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            onInstallFromLocalUri(uri, fileName) { success, error ->
                if (success) {
                    Toast.makeText(context, "Extension imported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Import failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Auto-search initially
    LaunchedEffect(Unit) {
        if (searchResults.isEmpty()) {
            onSearchQuery("")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("extensions_tab")
    ) {
        // Mode Switcher Bar
        TabRow(
            selectedTabIndex = subTabSelected,
            containerColor = Color.Transparent,
            contentColor = themeColor,
            indicator = { tabPositions ->
                if (subTabSelected < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[subTabSelected]),
                        color = themeColor
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Tab(
                selected = subTabSelected == 0,
                onClick = { subTabSelected = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Streams", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            )
            Tab(
                selected = subTabSelected == 1,
                onClick = { subTabSelected = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Plugins (${plugins.size})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            )
        }

        when (subTabSelected) {
            0 -> SearchStreamsView(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchClick = { onSearchQuery(searchQuery) },
                searchResults = searchResults,
                isSearching = isSearching,
                activePluginCount = plugins.count { it.isEnabled },
                themeColor = themeColor,
                onPlaySong = onPlayOnlineSong
            )
            1 -> InstalledPluginsView(
                plugins = plugins,
                themeColor = themeColor,
                onPickLocalFile = { filePickerLauncher.launch("*/*") },
                onInstallClick = { showInstallDialog = true },
                onTogglePlugin = onTogglePlugin,
                onDeletePlugin = onDeletePlugin
            )
        }
    }

    if (showInstallDialog) {
        InstallPluginDialog(
            themeColor = themeColor,
            onDismiss = { showInstallDialog = false },
            onPickLocalFile = {
                showInstallDialog = false
                filePickerLauncher.launch("*/*")
            },
            onInstallUrl = { url ->
                onInstallFromUrl(url) { success, error ->
                    if (success) {
                        Toast.makeText(context, "Plugin installed successfully!", Toast.LENGTH_SHORT).show()
                        showInstallDialog = false
                    } else {
                        Toast.makeText(context, "Failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onInstallCode = { code, name ->
                onInstallFromCode(code, name) { success, error ->
                    if (success) {
                        Toast.makeText(context, "Custom JS Plugin installed!", Toast.LENGTH_SHORT).show()
                        showInstallDialog = false
                    } else {
                        Toast.makeText(context, "Failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun SearchStreamsView(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    searchResults: List<OnlineSong>,
    isSearching: Boolean,
    activePluginCount: Int,
    themeColor: Color,
    onPlaySong: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search online streams...", fontSize = 14.sp, color = TextDarkSecondary) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("online_search_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = FrostedGlassBorder,
                    focusedContainerColor = FrostedGlassCard,
                    unfocusedContainerColor = FrostedGlassCard
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSearchClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }

        // Active extensions indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$activePluginCount Active Extension(s)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDarkSecondary
            )
            if (isSearching) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = themeColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fetching streams...", fontSize = 12.sp, color = themeColor)
                }
            }
        }

        if (searchResults.isEmpty() && !isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = themeColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No online stream results found", fontWeight = FontWeight.Medium, color = TextDarkSecondary)
                    Text("Try searching for songs or enable plugins in the Plugins tab", fontSize = 12.sp, color = TextDarkSecondary)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { onlineSong ->
                    OnlineSongItem(
                        onlineSong = onlineSong,
                        themeColor = themeColor,
                        onPlayClick = { onPlaySong(onlineSong.toSong()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineSongItem(
    onlineSong: OnlineSong,
    themeColor: Color,
    onPlayClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(16.dp),
        color = FrostedGlassCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, FrostedGlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (onlineSong.artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = onlineSong.artworkUrl,
                        contentDescription = onlineSong.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = onlineSong.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${onlineSong.artist} • ${onlineSong.album}",
                    fontSize = 13.sp,
                    color = TextDarkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(themeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = onlineSong.extensionName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(themeColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun InstalledPluginsView(
    plugins: List<ExtensionPlugin>,
    themeColor: Color,
    onPickLocalFile: () -> Unit,
    onInstallClick: () -> Unit,
    onTogglePlugin: (String) -> Unit,
    onDeletePlugin: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Install action buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onPickLocalFile,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pick_file_extension_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick .eapk / .js File", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onInstallClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("install_extension_dialog_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("URL / Code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (plugins.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No plugins installed", color = TextDarkSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(plugins) { plugin ->
                    PluginCard(
                        plugin = plugin,
                        themeColor = themeColor,
                        onToggle = { onTogglePlugin(plugin.id) },
                        onDelete = { onDeletePlugin(plugin.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginCard(
    plugin: ExtensionPlugin,
    themeColor: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = FrostedGlassCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, FrostedGlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plugin.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "v${plugin.version}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        text = "Author: ${plugin.author}",
                        fontSize = 12.sp,
                        color = TextDarkSecondary
                    )
                }

                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColor, checkedTrackColor = themeColor.copy(alpha = 0.4f))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plugin.description,
                fontSize = 13.sp,
                color = TextDarkPrimary.copy(alpha = 0.8f)
            )

            if (!plugin.sourceUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(12.dp), tint = TextDarkSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = plugin.sourceUrl,
                        fontSize = 11.sp,
                        color = TextDarkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Plugin", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun InstallPluginDialog(
    themeColor: Color,
    onDismiss: () -> Unit,
    onPickLocalFile: () -> Unit,
    onInstallUrl: (String) -> Unit,
    onInstallCode: (String, String) -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0: Local File, 1: URL, 2: Code
    var urlInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var pluginNameInput by remember { mutableStateOf("Custom Plugin") }
    var isInstalling by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isInstalling) onDismiss() },
        title = {
            Text("Install Music Extension", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPickLocalFile,
                        enabled = !isInstalling,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = themeColor.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("File", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = { mode = 1 },
                        enabled = !isInstalling,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (mode == 1) themeColor.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("URL", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = { mode = 2 },
                        enabled = !isInstalling,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (mode == 2) themeColor.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("JS Code", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isInstalling) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = themeColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Downloading & installing extension...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                } else if (mode == 1) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Plugin Script / Shortcode URL (.js / .eapk)") },
                        placeholder = { Text("https://example.com/extension.eapk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (mode == 2) {
                    OutlinedTextField(
                        value = pluginNameInput,
                        onValueChange = { pluginNameInput = it },
                        label = { Text("Plugin Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("JavaScript Extension Code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("function search(query) { return JSON.stringify([...]); }") }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(themeColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isInstalling) { onPickLocalFile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = themeColor, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Click to select .eapk or .js file", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (mode == 0) {
                Button(
                    onClick = onPickLocalFile,
                    enabled = !isInstalling,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("Select File")
                }
            } else {
                Button(
                    onClick = {
                        if (mode == 1) {
                            if (urlInput.isNotBlank()) {
                                isInstalling = true
                                onInstallUrl(urlInput.trim())
                            }
                        } else {
                            if (codeInput.isNotBlank()) {
                                isInstalling = true
                                onInstallCode(codeInput.trim(), pluginNameInput.trim())
                            }
                        }
                    },
                    enabled = !isInstalling && ((mode == 1 && urlInput.isNotBlank()) || (mode == 2 && codeInput.isNotBlank())),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text(if (isInstalling) "Installing..." else "Install")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isInstalling) {
                Text("Cancel")
            }
        }
    )
}
