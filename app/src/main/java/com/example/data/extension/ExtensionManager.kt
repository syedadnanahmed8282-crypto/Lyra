package com.example.data.extension

import android.content.Context
import com.squareup.duktape.Duktape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ExtensionPlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val scriptContent: String,
    val isEnabled: Boolean = true,
    val iconUrl: String? = null,
    val sourceUrl: String? = null
)

data class OnlineSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val streamUrl: String,
    val artworkUrl: String,
    val durationMs: Long,
    val extensionId: String,
    val extensionName: String
)

class ExtensionManager(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val extensionsDir = File(context.filesDir, "plugins")

    private val _installedPlugins = MutableStateFlow<List<ExtensionPlugin>>(emptyList())
    val installedPlugins: StateFlow<List<ExtensionPlugin>> = _installedPlugins.asStateFlow()

    private val _searchResults = MutableStateFlow<List<OnlineSong>>(emptyList())
    val searchResults: StateFlow<List<OnlineSong>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        if (!extensionsDir.exists()) {
            extensionsDir.mkdirs()
        }
        loadInstalledPlugins()
    }

    private fun loadInstalledPlugins() {
        val pluginsList = mutableListOf<ExtensionPlugin>()

        val pluginFiles = extensionsDir.listFiles { _, name -> name.endsWith(".js") || name.endsWith(".eapk") || name.endsWith(".json") }

        if (pluginFiles.isNullOrEmpty()) {
            val defaultPlugin1 = createDefaultNcsPlugin()
            val defaultPlugin2 = createDefaultRadioPlugin()
            savePluginToFile(defaultPlugin1)
            savePluginToFile(defaultPlugin2)
            pluginsList.add(defaultPlugin1)
            pluginsList.add(defaultPlugin2)
        } else {
            for (file in pluginFiles) {
                try {
                    val content = file.readText()
                    val plugin = parsePluginFromContent(file.nameWithoutExtension, content)
                    if (plugin != null) {
                        pluginsList.add(plugin)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        _installedPlugins.value = pluginsList
    }

    private fun savePluginToFile(plugin: ExtensionPlugin) {
        val file = File(extensionsDir, "${plugin.id}.js")
        val json = JSONObject().apply {
            put("id", plugin.id)
            put("name", plugin.name)
            put("version", plugin.version)
            put("author", plugin.author)
            put("description", plugin.description)
            put("isEnabled", plugin.isEnabled)
            put("script", plugin.scriptContent)
            put("iconUrl", plugin.iconUrl ?: "")
            put("sourceUrl", plugin.sourceUrl ?: "")
        }
        file.writeText(json.toString())
    }

    fun parsePluginFromContent(id: String, content: String): ExtensionPlugin? {
        return try {
            if (content.trim().startsWith("{")) {
                val json = JSONObject(content)
                ExtensionPlugin(
                    id = json.optString("id", id),
                    name = json.optString("name", "Unknown Extension"),
                    version = json.optString("version", "1.0.0"),
                    author = json.optString("author", "Community"),
                    description = json.optString("description", "Online audio plugin extension"),
                    scriptContent = json.optString("script", content),
                    isEnabled = json.optBoolean("isEnabled", true),
                    iconUrl = json.optString("iconUrl", null),
                    sourceUrl = json.optString("sourceUrl", null)
                )
            } else {
                var name = "Custom Plugin ($id)"
                var author = "Community"
                var version = "1.0.0"
                var description = "Custom JS Music Extension"

                val lines = content.lines().take(15)
                for (line in lines) {
                    if (line.contains("@name")) name = line.substringAfter("@name").trim()
                    if (line.contains("@author")) author = line.substringAfter("@author").trim()
                    if (line.contains("@version")) version = line.substringAfter("@version").trim()
                    if (line.contains("@description")) description = line.substringAfter("@description").trim()
                }

                ExtensionPlugin(
                    id = id,
                    name = name,
                    version = version,
                    author = author,
                    description = description,
                    scriptContent = content,
                    isEnabled = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun installPluginFromUrl(url: String): Result<ExtensionPlugin> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}"))
            }

            val bodyString = response.body?.string() ?: throw Exception("Empty response body")
            val id = "ext_" + UUID.randomUUID().toString().take(8)
            val plugin = parsePluginFromContent(id, bodyString) ?: throw Exception("Failed to parse extension code")
            val pluginWithSource = plugin.copy(sourceUrl = url)

            savePluginToFile(pluginWithSource)
            loadInstalledPlugins()
            Result.success(pluginWithSource)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installPluginFromCode(code: String, customName: String = "Imported Plugin"): Result<ExtensionPlugin> = withContext(Dispatchers.IO) {
        try {
            val id = "ext_" + UUID.randomUUID().toString().take(8)
            val plugin = parsePluginFromContent(id, code) ?: ExtensionPlugin(
                id = id,
                name = customName,
                version = "1.0.0",
                author = "User",
                description = "User imported plugin script",
                scriptContent = code
            )
            savePluginToFile(plugin)
            loadInstalledPlugins()
            Result.success(plugin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installPluginFromLocalUri(uri: android.net.Uri, fileName: String?): Result<ExtensionPlugin> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream"))
            val content = inputStream.bufferedReader().use { it.readText() }
            val cleanName = fileName?.substringBeforeLast(".") ?: "Imported Extension"

            val id = "ext_" + UUID.randomUUID().toString().take(8)
            val plugin = parsePluginFromContent(id, content) ?: ExtensionPlugin(
                id = id,
                name = cleanName,
                version = "1.0.0",
                author = "Local File",
                description = "Imported from local file ($cleanName)",
                scriptContent = content
            )
            savePluginToFile(plugin)
            loadInstalledPlugins()
            Result.success(plugin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val current = _installedPlugins.value
        val updated = current.map {
            if (it.id == pluginId) {
                val newPlugin = it.copy(isEnabled = !it.isEnabled)
                savePluginToFile(newPlugin)
                newPlugin
            } else it
        }
        _installedPlugins.value = updated
    }

    suspend fun deletePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val file = File(extensionsDir, "$pluginId.js")
        if (file.exists()) {
            file.delete()
        }
        loadInstalledPlugins()
    }

    suspend fun searchOnlineSongs(query: String) = withContext(Dispatchers.IO) {
        _isSearching.value = true
        val results = mutableListOf<OnlineSong>()
        val activePlugins = _installedPlugins.value.filter { it.isEnabled }

        for (plugin in activePlugins) {
            try {
                val pluginResults = executeSearchInPlugin(plugin, query)
                results.addAll(pluginResults)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _searchResults.value = results
        _isSearching.value = false
    }

    private fun executeSearchInPlugin(plugin: ExtensionPlugin, query: String): List<OnlineSong> {
        val results = mutableListOf<OnlineSong>()

        var executedSuccessfully = false
        try {
            val duktape = Duktape.create()
            try {
                duktape.evaluate(plugin.scriptContent)
                val jsCall = "search('${query.replace("'", "\\'")}')"
                val jsonResultStr = duktape.evaluate(jsCall) as? String
                if (!jsonResultStr.isNullOrEmpty()) {
                    val jsonArray = JSONArray(jsonResultStr)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        results.add(
                            OnlineSong(
                                id = item.optString("id", "${plugin.id}_$i"),
                                title = item.optString("title", "Unknown Track"),
                                artist = item.optString("artist", "Unknown Artist"),
                                album = item.optString("album", plugin.name),
                                streamUrl = item.optString("streamUrl", ""),
                                artworkUrl = item.optString("artworkUrl", ""),
                                durationMs = item.optLong("durationMs", 180000L),
                                extensionId = plugin.id,
                                extensionName = plugin.name
                            )
                        )
                    }
                    executedSuccessfully = true
                }
            } finally {
                duktape.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!executedSuccessfully && results.isEmpty()) {
            results.addAll(executeFallbackSearch(plugin, query))
        }

        return results
    }

    private fun executeFallbackSearch(plugin: ExtensionPlugin, query: String): List<OnlineSong> {
        val list = mutableListOf<OnlineSong>()
        val qLower = query.lowercase().trim()

        if (plugin.id == "sound_stream_preset" || plugin.id.contains("ncs")) {
            val sampleTracks = listOf(
                OnlineSong(
                    id = "ncs_1",
                    title = "Acoustic Sunbeams",
                    artist = "SoundHelix Band",
                    album = "Acoustic Gems",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    artworkUrl = "https://picsum.photos/seed/soundhelix1/400/400",
                    durationMs = 372000L,
                    extensionId = plugin.id,
                    extensionName = plugin.name
                ),
                OnlineSong(
                    id = "ncs_2",
                    title = "Midnight Synth Overture",
                    artist = "SoundHelix Synth",
                    album = "Electronic Dreams",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    artworkUrl = "https://picsum.photos/seed/soundhelix2/400/400",
                    durationMs = 423000L,
                    extensionId = plugin.id,
                    extensionName = plugin.name
                ),
                OnlineSong(
                    id = "ncs_3",
                    title = "Celestial Groove",
                    artist = "SoundHelix Funk",
                    album = "Neon Night",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    artworkUrl = "https://picsum.photos/seed/soundhelix3/400/400",
                    durationMs = 340000L,
                    extensionId = plugin.id,
                    extensionName = plugin.name
                ),
                OnlineSong(
                    id = "ncs_4",
                    title = "Ocean Wave Chill",
                    artist = "Ambient Flow",
                    album = "Deep Relaxation",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    artworkUrl = "https://picsum.photos/seed/soundhelix4/400/400",
                    durationMs = 302000L,
                    extensionId = plugin.id,
                    extensionName = plugin.name
                )
            )

            list.addAll(
                if (qLower.isEmpty()) sampleTracks
                else sampleTracks.filter { it.title.lowercase().contains(qLower) || it.artist.lowercase().contains(qLower) }
            )
        } else if (plugin.id == "radio_waves_preset" || plugin.id.contains("radio")) {
            val radioStations = listOf(
                OnlineSong(
                    id = "radio_1",
                    title = "Lofi Chill Beats Radio",
                    artist = "24/7 Stream",
                    album = "Global Radio",
                    streamUrl = "https://stream.zeno.fm/f3wvbbqmdg8uv",
                    artworkUrl = "https://picsum.photos/seed/lofiradio/400/400",
                    durationMs = 0L,
                    extensionId = plugin.id,
                    extensionName = plugin.name
                ),
                OnlineSong(
                    id = "radio_2",
                    title = "Synthwave Cyber Radio",
                    artist = "Nightdrive FM",
                    album = "Retro Radio",
                    streamUrl = "https://stream.zeno.fm/0r0xa792kwzuv",
                    artworkUrl = "https://picsum.photos/seed/synthradio/400/400",
                    durationMs = 0L,
                    extensionId = plugin.id,
                    extensionName = plugin.name
                ),
                OnlineSong(
                    id = "radio_3",
                    title = "Jazz Vibes Lounge",
                    artist = "Cafe FM",
                    album = "Jazz FM",
                    streamUrl = "https://stream.zeno.fm/3s8p62vsn8quv",
                    artworkUrl = "https://picsum.photos/seed/jazzradio/400/400",
                    durationMs = 0L,
                    extensionId = plugin.id,
                    extensionName = plugin.name
                )
            )

            list.addAll(
                if (qLower.isEmpty()) radioStations
                else radioStations.filter { it.title.lowercase().contains(qLower) || it.artist.lowercase().contains(qLower) }
            )
        }

        return list
    }

    private fun createDefaultNcsPlugin(): ExtensionPlugin {
        val jsScript = """
            // @name Echo Royalty-Free Streamer
            // @author Lyra Extension Hub
            // @version 1.2.0
            // @description Plugin to stream high quality open music audio tracks
            
            function search(query) {
                var q = query.toLowerCase();
                var tracks = [
                    {
                        "id": "soundhelix_1",
                        "title": "Acoustic Sunbeams",
                        "artist": "SoundHelix Band",
                        "album": "Acoustic Gems",
                        "streamUrl": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                        "artworkUrl": "https://picsum.photos/seed/soundhelix1/400/400",
                        "durationMs": 372000
                    },
                    {
                        "id": "soundhelix_2",
                        "title": "Midnight Synth Overture",
                        "artist": "SoundHelix Synth",
                        "album": "Electronic Dreams",
                        "streamUrl": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                        "artworkUrl": "https://picsum.photos/seed/soundhelix2/400/400",
                        "durationMs": 423000
                    },
                    {
                        "id": "soundhelix_3",
                        "title": "Celestial Groove",
                        "artist": "SoundHelix Funk",
                        "album": "Neon Night",
                        "streamUrl": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                        "artworkUrl": "https://picsum.photos/seed/soundhelix3/400/400",
                        "durationMs": 340000
                    }
                ];
                
                if (!q) return JSON.stringify(tracks);
                var filtered = [];
                for (var i = 0; i < tracks.length; i++) {
                    if (tracks[i].title.toLowerCase().indexOf(q) !== -1 || tracks[i].artist.toLowerCase().indexOf(q) !== -1) {
                        filtered.push(tracks[i]);
                    }
                }
                return JSON.stringify(filtered);
            }
        """.trimIndent()

        return ExtensionPlugin(
            id = "sound_stream_preset",
            name = "Echo Royalty-Free Streamer",
            version = "1.2.0",
            author = "Lyra Extension Hub",
            description = "Streams high quality open audio streams directly online",
            scriptContent = jsScript,
            isEnabled = true,
            iconUrl = "https://picsum.photos/seed/echoplugin/200/200"
        )
    }

    private fun createDefaultRadioPlugin(): ExtensionPlugin {
        val jsScript = """
            // @name World Radio Streams
            // @author Lyra Extension Hub
            // @version 1.0.0
            // @description Live radio broadcasting stations from around the world
            
            function search(query) {
                var q = query.toLowerCase();
                var stations = [
                    {
                        "id": "radio_1",
                        "title": "Lofi Chill Beats Radio",
                        "artist": "24/7 Live Stream",
                        "album": "Global Radio",
                        "streamUrl": "https://stream.zeno.fm/f3wvbbqmdg8uv",
                        "artworkUrl": "https://picsum.photos/seed/lofiradio/400/400",
                        "durationMs": 0
                    },
                    {
                        "id": "radio_2",
                        "title": "Synthwave Cyber Radio",
                        "artist": "Nightdrive FM",
                        "album": "Retro Radio",
                        "streamUrl": "https://stream.zeno.fm/0r0xa792kwzuv",
                        "artworkUrl": "https://picsum.photos/seed/synthradio/400/400",
                        "durationMs": 0
                    }
                ];
                
                if (!q) return JSON.stringify(stations);
                var filtered = [];
                for (var i = 0; i < stations.length; i++) {
                    if (stations[i].title.toLowerCase().indexOf(q) !== -1 || stations[i].artist.toLowerCase().indexOf(q) !== -1) {
                        filtered.push(stations[i]);
                    }
                }
                return JSON.stringify(filtered);
            }
        """.trimIndent()

        return ExtensionPlugin(
            id = "radio_waves_preset",
            name = "World Radio Streams",
            version = "1.0.0",
            author = "Lyra Extension Hub",
            description = "Live radio music stations (Lofi, Synthwave, Chillout)",
            scriptContent = jsScript,
            isEnabled = true,
            iconUrl = "https://picsum.photos/seed/radioplugin/200/200"
        )
    }
}
