package com.example.repository

import android.content.Context
import com.example.data.db.dao.FavoriteDao
import com.example.data.db.dao.PlaylistDao
import com.example.data.db.entity.FavoriteEntity
import com.example.data.db.entity.PlaylistEntity
import com.example.data.scanner.MediaStoreScanner
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Folder
import com.example.model.Song
import com.example.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MusicRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao
) {
    private val scanner = MediaStoreScanner(context)

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())
    val rawSongs: StateFlow<List<Song>> = _rawSongs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val favoritesList: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    val playlistsList: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    /**
     * Combined reactive song list filtered by search query, marked with favorite status, and sorted.
     */
    val songsList: Flow<List<Song>> = combine(_rawSongs, _searchQuery, _sortOrder, favoritesList) { raw, query, sort, favs ->
        val favIds = favs.map { it.songId }.toSet()

        val markedSongs = raw.map { song ->
            song.copy(isFavorite = favIds.contains(song.id))
        }

        val filtered = if (query.isBlank()) {
            markedSongs
        } else {
            val q = query.trim().lowercase()
            markedSongs.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.folderName.lowercase().contains(q)
            }
        }

        when (sort) {
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOrder.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.DURATION_DESC -> filtered.sortedByDescending { it.duration }
            SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.size }
        }
    }

    /**
     * Group songs into Albums.
     */
    val albumsList: Flow<List<Album>> = songsList.map { songs ->
        songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
            val first = albumSongs.first()
            Album(
                id = albumId,
                title = first.album,
                artist = first.artist,
                songCount = albumSongs.size,
                albumArtUri = first.albumArtUri,
                demoDrawableRes = first.demoDrawableRes
            )
        }.sortedBy { it.title.lowercase() }
    }

    /**
     * Group songs into Artists.
     */
    val artistsList: Flow<List<Artist>> = songsList.map { songs ->
        songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
            val albumCount = artistSongs.map { it.albumId }.distinct().size
            Artist(
                name = artistName,
                songCount = artistSongs.size,
                albumCount = albumCount
            )
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Group songs into Folders.
     */
    val foldersList: Flow<List<Folder>> = songsList.map { songs ->
        songs.groupBy { it.folderPath }.map { (path, folderSongs) ->
            val first = folderSongs.first()
            Folder(
                name = first.folderName.ifEmpty { "Root Folder" },
                path = path,
                songCount = folderSongs.size
            )
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Scan local audio files.
     */
    suspend fun refreshLocalAudio() {
        _rawSongs.value = scanner.scanLocalAudioFiles()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    suspend fun toggleFavorite(songId: Long, currentIsFav: Boolean) {
        if (currentIsFav) {
            favoriteDao.deleteFavorite(songId)
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(songId = songId))
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
        playlistDao.clearPlaylistItems(playlistId)
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylistAndUpdateCount(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return combine(songsList, playlistDao.getSongIdsForPlaylist(playlistId)) { allSongs, ids ->
            val idSet = ids.toSet()
            allSongs.filter { idSet.contains(it.id) }
        }
    }
}
