package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LyraApplication
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Folder
import com.example.model.Song
import com.example.model.SortOrder
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.VibrantPurple
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as LyraApplication).repository
    val extensionManager = (application as LyraApplication).extensionManager

    val installedPlugins = extensionManager.installedPlugins
    val onlineSearchResults = extensionManager.searchResults
    val isSearchingOnline = extensionManager.isSearching

    private val _themeColor = MutableStateFlow(VibrantPurple)
    val themeColor: StateFlow<Color> = _themeColor.asStateFlow()

    fun setThemeColor(color: Color) {
        _themeColor.value = color
    }

    val songs: StateFlow<List<Song>> = repository.songsList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val albums: StateFlow<List<Album>> = repository.albumsList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val artists: StateFlow<List<Artist>> = repository.artistsList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val folders: StateFlow<List<Folder>> = repository.foldersList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists = repository.playlistsList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchQuery: StateFlow<String> = repository.searchQuery
    val sortOrder: StateFlow<SortOrder> = repository.sortOrder

    private val _selectedTab = MutableStateFlow(0) // 0: Songs, 1: Albums, 2: Artists, 3: Playlists, 4: Folders
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    init {
        refreshMusicLibrary()
    }

    fun setPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
        if (granted) {
            refreshMusicLibrary()
        }
    }

    fun refreshMusicLibrary() {
        viewModelScope.launch {
            repository.refreshLocalAudio()
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        repository.setSearchQuery(query)
    }

    fun setSortOrder(order: SortOrder) {
        repository.setSortOrder(order)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    fun openAddToPlaylistDialog(song: Song) {
        _songToAddToPlaylist.value = song
    }

    fun closeAddToPlaylistDialog() {
        _songToAddToPlaylist.value = null
    }

    fun openCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = true
    }

    fun closeCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = false
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim())
            closeCreatePlaylistDialog()
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song.id)
            closeAddToPlaylistDialog()
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun searchOnlineSongs(query: String) {
        viewModelScope.launch {
            extensionManager.searchOnlineSongs(query)
        }
    }

    fun installPluginFromUrl(url: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = extensionManager.installPluginFromUrl(url)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Installation failed")
            }
        }
    }

    fun installPluginFromCode(code: String, name: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = extensionManager.installPluginFromCode(code, name)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to parse code")
            }
        }
    }

    fun installPluginFromLocalUri(uri: android.net.Uri, fileName: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = extensionManager.installPluginFromLocalUri(uri, fileName)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to read local file")
            }
        }
    }

    fun togglePlugin(pluginId: String) {
        viewModelScope.launch {
            extensionManager.togglePlugin(pluginId)
        }
    }

    fun deletePlugin(pluginId: String) {
        viewModelScope.launch {
            extensionManager.deletePlugin(pluginId)
        }
    }
}
