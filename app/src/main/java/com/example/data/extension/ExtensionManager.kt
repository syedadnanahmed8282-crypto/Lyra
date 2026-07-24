package com.example.data.extension

import android.content.Context
import com.squareup.duktape.Duktape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

interface JsHttpBridge {
    fun httpGet(url: String, headersJson: String?): String
    fun httpPost(url: String, body: String, headersJson: String?): String
}

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
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val extensionsDir = File(context.filesDir, "extensions")
    private val legacyPluginsDir = File(context.filesDir, "plugins")

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

        val files = mutableListOf<File>()
        if (extensionsDir.exists()) {
            extensionsDir.listFiles { _, name -> name.endsWith(".js") || name.endsWith(".eapk") || name.endsWith(".json") }?.let { files.addAll(it) }
        }
        if (legacyPluginsDir.exists()) {
            legacyPluginsDir.listFiles { _, name -> name.endsWith(".js") || name.endsWith(".eapk") || name.endsWith(".json") }?.let { files.addAll(it) }
        }

        if (files.isEmpty()) {
            val defaultPlugin1 = createDefaultNcsPlugin()
            val defaultPlugin2 = createDefaultRadioPlugin()
            savePluginToFile(defaultPlugin1)
            savePluginToFile(defaultPlugin2)
            pluginsList.add(defaultPlugin1)
            pluginsList.add(defaultPlugin2)
        } else {
            for (file in files) {
                try {
                    val bytes = file.readBytes()
                    val plugin = parsePluginFromBytes(file.nameWithoutExtension, bytes, file.nameWithoutExtension)
                    if (plugin != null) {
                        pluginsList.add(plugin)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        _installedPlugins.value = pluginsList.distinctBy { it.id }
    }

    private fun savePluginToFile(plugin: ExtensionPlugin) {
        val file = File(extensionsDir, "${plugin.id}.json")
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

    fun parsePluginFromBytes(id: String, bytes: ByteArray, defaultName: String): ExtensionPlugin? {
        if (bytes.size >= 4 && bytes[0] == 'P'.toByte() && bytes[1] == 'K'.toByte() && bytes[2] == 3.toByte() && bytes[3] == 4.toByte()) {
            try {
                var manifestContent: String? = null
                val scripts = mutableMapOf<String, String>()

                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val entryName = entry.name.lowercase()
                            val out = ByteArrayOutputStream()
                            zis.copyTo(out)
                            val text = out.toString("UTF-8")
                            if (entryName.endsWith("manifest.json") || entryName == "manifest.json") {
                                manifestContent = text
                            } else if (entryName.endsWith(".js")) {
                                scripts[entry.name] = text
                            }
                        }
                        entry = zis.nextEntry
                    }
                }

                var pluginName = defaultName
                var version = "1.0.0"
                var author = "Community"
                var description = "EAPK Extension Package"
                var mainFile = "index.js"

                if (manifestContent != null) {
                    val json = JSONObject(manifestContent)
                    pluginName = json.optString("name", defaultName)
                    version = json.optString("version", "1.0.0")
                    author = json.optString("author", "Community")
                    description = json.optString("description", description)
                    mainFile = json.optString("main", "index.js")
                }

                val mainScriptContent = scripts[mainFile]
                    ?: scripts.entries.find { it.key.endsWith(mainFile) }?.value
                    ?: scripts.values.firstOrNull()

                if (!mainScriptContent.isNullOrBlank()) {
                    return ExtensionPlugin(
                        id = id,
                        name = pluginName,
                        version = version,
                        author = author,
                        description = description,
                        scriptContent = mainScriptContent,
                        isEnabled = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val textContent = bytes.toString(Charsets.UTF_8)
        return parsePluginFromContent(id, textContent, defaultName)
    }

    fun parsePluginFromContent(id: String, content: String, defaultName: String = "Custom Extension"): ExtensionPlugin? {
        return try {
            val trimmed = content.trim()
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                ExtensionPlugin(
                    id = json.optString("id", id),
                    name = json.optString("name", defaultName),
                    version = json.optString("version", "1.0.0"),
                    author = json.optString("author", "Community"),
                    description = json.optString("description", "Online audio plugin extension"),
                    scriptContent = json.optString("script", content),
                    isEnabled = json.optBoolean("isEnabled", true),
                    iconUrl = json.optString("iconUrl", null),
                    sourceUrl = json.optString("sourceUrl", null)
                )
            } else {
                var name = defaultName
                var author = "Community"
                var version = "1.0.0"
                var description = "Custom JS Music Extension"

                val lines = content.lines().take(20)
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
            var cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }

            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code} (${response.message})"))
            }

            val bytes = response.body?.bytes() ?: throw Exception("Empty response body from server")
            val id = "ext_" + UUID.randomUUID().toString().take(8)
            val defaultName = cleanUrl.substringAfterLast("/").substringBefore("?").ifEmpty { "Downloaded Plugin" }

            val plugin = parsePluginFromBytes(id, bytes, defaultName)
                ?: throw Exception("Failed to parse extension code or package")
            val pluginWithSource = plugin.copy(sourceUrl = cleanUrl)

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
            val plugin = parsePluginFromContent(id, code, customName) ?: ExtensionPlugin(
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
            val bytes = inputStream.use { it.readBytes() }
            val cleanName = fileName?.substringBeforeLast(".") ?: "Imported Extension"

            val id = "ext_" + UUID.randomUUID().toString().take(8)
            val plugin = parsePluginFromBytes(id, bytes, cleanName)
                ?: throw Exception("Failed to parse local extension package")

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
        val jsonFile = File(extensionsDir, "$pluginId.json")
        val jsFile = File(extensionsDir, "$pluginId.js")
        if (jsonFile.exists()) jsonFile.delete()
        if (jsFile.exists()) jsFile.delete()
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

    private fun createJsBridge(): JsHttpBridge {
        return object : JsHttpBridge {
            override fun httpGet(url: String, headersJson: String?): String {
                return try {
                    val reqBuilder = Request.Builder().url(url)
                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    if (!headersJson.isNullOrEmpty() && headersJson != "{}") {
                        val json = JSONObject(headersJson)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            reqBuilder.header(key, json.getString(key))
                        }
                    }
                    okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        resp.body?.string() ?: ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }

            override fun httpPost(url: String, body: String, headersJson: String?): String {
                return try {
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val reqBody = body.toRequestBody(mediaType)
                    val reqBuilder = Request.Builder().url(url).post(reqBody)
                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    if (!headersJson.isNullOrEmpty() && headersJson != "{}") {
                        val json = JSONObject(headersJson)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            reqBuilder.header(key, json.getString(key))
                        }
                    }
                    okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        resp.body?.string() ?: ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }
        }
    }

    private fun getJsPolyfill(): String {
        return """
            var http = {
                get: function(url, headers) {
                    var hStr = headers ? (typeof headers === 'string' ? headers : JSON.stringify(headers)) : "{}";
                    var res = JsHttpBridge.httpGet(url, hStr);
                    return {
                        status: 200,
                        body: res,
                        text: function() { return res; },
                        json: function() { try { return JSON.parse(res); } catch(e) { return {}; } }
                    };
                },
                post: function(url, body, headers) {
                    var hStr = headers ? (typeof headers === 'string' ? headers : JSON.stringify(headers)) : "{}";
                    var bStr = typeof body === 'string' ? body : JSON.stringify(body || {});
                    var res = JsHttpBridge.httpPost(url, bStr, hStr);
                    return {
                        status: 200,
                        body: res,
                        text: function() { return res; },
                        json: function() { try { return JSON.parse(res); } catch(e) { return {}; } }
                    };
                }
            };

            function httpGet(url, headers) {
                var hStr = headers ? (typeof headers === 'string' ? headers : JSON.stringify(headers)) : "{}";
                return JsHttpBridge.httpGet(url, hStr);
            }

            function fetch(url, options) {
                options = options || {};
                var method = (options.method || 'GET').toUpperCase();
                var headers = options.headers || {};
                if (method === 'POST') {
                    return http.post(url, options.body || '', headers);
                } else {
                    return http.get(url, headers);
                }
            }
        """.trimIndent()
    }

    private fun executeSearchInPlugin(plugin: ExtensionPlugin, query: String): List<OnlineSong> {
        val results = mutableListOf<OnlineSong>()

        var executedSuccessfully = false
        try {
            val duktape = Duktape.create()
            try {
                duktape.set("JsHttpBridge", JsHttpBridge::class.java, createJsBridge())
                duktape.evaluate(getJsPolyfill())
                duktape.evaluate(plugin.scriptContent)

                val escapedQuery = query.replace("\\", "\\\\").replace("'", "\\'")
                val jsCall = "search('$escapedQuery')"
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
                                streamUrl = item.optString("streamUrl", item.optString("url", "")),
                                artworkUrl = item.optString("artworkUrl", item.optString("cover", "")),
                                durationMs = item.optLong("durationMs", item.optLong("duration", 180000L)),
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

    suspend fun resolveStreamUrl(song: OnlineSong): String = withContext(Dispatchers.IO) {
        if (song.streamUrl.startsWith("http://") || song.streamUrl.startsWith("https://")) {
            return@withContext song.streamUrl
        }
        val plugin = _installedPlugins.value.find { it.id == song.extensionId }
            ?: return@withContext song.streamUrl

        try {
            val duktape = Duktape.create()
            try {
                duktape.set("JsHttpBridge", JsHttpBridge::class.java, createJsBridge())
                duktape.evaluate(getJsPolyfill())
                duktape.evaluate(plugin.scriptContent)
                val jsCall = "getStreamUrl('${song.id}')"
                val resolved = duktape.evaluate(jsCall) as? String
                if (!resolved.isNullOrEmpty() && (resolved.startsWith("http://") || resolved.startsWith("https://"))) {
                    return@withContext resolved
                }
            } finally {
                duktape.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext song.streamUrl
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

