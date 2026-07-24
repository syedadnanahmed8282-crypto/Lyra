package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SortMenu
import com.example.ui.theme.DarkPurpleSurface
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassBorderHighlight
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPurpleBg
import com.example.ui.theme.SunnyYellowBg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantPurple
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PlayerViewModel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel
) {
    val selectedTab by mainViewModel.selectedTab.collectAsState()
    val songs by mainViewModel.songs.collectAsState()
    val albums by mainViewModel.albums.collectAsState()
    val artists by mainViewModel.artists.collectAsState()
    val playlists by mainViewModel.playlists.collectAsState()
    val folders by mainViewModel.folders.collectAsState()

    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val sortOrder by mainViewModel.sortOrder.collectAsState()
    val themeColor by mainViewModel.themeColor.collectAsState()
    val selectedExtensionMode by mainViewModel.selectedExtensionMode.collectAsState()

    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isLoading by playerViewModel.isLoading.collectAsState()
    val currentPositionMs by playerViewModel.currentPosition.collectAsState()
    val durationMs by playerViewModel.duration.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val isShuffle by playerViewModel.isShuffle.collectAsState()
    val sleepTimerMsLeft by playerViewModel.sleepTimerMsLeft.collectAsState()
    val equalizerInfo by playerViewModel.equalizerInfo.collectAsState()

    val isNowPlayingExpanded by playerViewModel.isNowPlayingExpanded.collectAsState()
    val showEqualizerDialog by playerViewModel.showEqualizerDialog.collectAsState()
    val showSleepTimerDialog by playerViewModel.showSleepTimerDialog.collectAsState()

    val songToAddToPlaylist by mainViewModel.songToAddToPlaylist.collectAsState()
    val showCreatePlaylistDialog by mainViewModel.showCreatePlaylistDialog.collectAsState()

    val plugins by mainViewModel.installedPlugins.collectAsState()
    val searchResults by mainViewModel.onlineSearchResults.collectAsState()
    val isSearchingOnline by mainViewModel.isSearchingOnline.collectAsState()

    val tabs = listOf("Songs", "Albums", "Artists", "Playlists", "Folders", "Extensions")

    val isMidnightDark = themeColor == Color(0xFF0A1128)
    val screenBgColor = if (isMidnightDark) Color.Black else SoftPurpleBg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar: App Title & Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 8.dp)
                )

                Text(
                    text = "Lyra",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = if (isMidnightDark) Color.White else TextDarkPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                SortMenu(
                    currentSort = sortOrder,
                    onSortSelected = { mainViewModel.setSortOrder(it) }
                )
            }

            val searchContainerColor = if (isMidnightDark) Color.Black else PureWhite
            val searchTextColor = if (isMidnightDark) Color.White else TextDarkPrimary
            val searchPlaceholderColor = if (isMidnightDark) Color(0xFFA0A5B5) else TextDarkSecondary

            // Search Bar Input (Clean White Pill or Black Pill in Dark Theme)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { mainViewModel.setSearchQuery(it) },
                placeholder = { Text("Search songs, artists, albums...", color = searchPlaceholderColor, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = themeColor) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { mainViewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = if (isMidnightDark) Color.White else TextDarkSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = searchContainerColor,
                    unfocusedContainerColor = searchContainerColor,
                    focusedTextColor = searchTextColor,
                    unfocusedTextColor = searchTextColor,
                    focusedIndicatorColor = themeColor,
                    unfocusedIndicatorColor = if (isMidnightDark) Color(0xFF0A1128) else Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_bar")
            )

            // Extension Mode Selection Chip Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedExtensionMode == "ALL",
                        onClick = { mainViewModel.setSelectedExtensionMode("ALL") },
                        label = { Text("🌐 All Sources", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColor,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedExtensionMode == "LOCAL",
                        onClick = { mainViewModel.setSelectedExtensionMode("LOCAL") },
                        label = { Text("📱 Local Only", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColor,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
                plugins.filter { it.isEnabled }.forEach { plugin ->
                    item {
                        FilterChip(
                            selected = selectedExtensionMode == plugin.id,
                            onClick = { mainViewModel.setSelectedExtensionMode(plugin.id) },
                            label = { Text("✨ ${plugin.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColor,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scrollable Category Tabs Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = screenBgColor,
                contentColor = if (isMidnightDark) Color.White else TextDarkPrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = themeColor,
                            height = 3.dp
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { mainViewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = if (selectedTab == index) (if (isMidnightDark) Color.White else themeColor) else (if (isMidnightDark) Color(0xFFA0A5B5) else TextDarkSecondary)
                            )
                        },
                        modifier = Modifier.testTag("tab_$title")
                    )
                }
            }

            // Main Tab Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> SongsTab(
                        songs = songs,
                        currentPlayingSong = currentSong,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        themeColor = themeColor,
                        onSongClick = { list, index -> playerViewModel.playSongList(list, index) },
                        onPlayPauseToggle = { playerViewModel.togglePlayPause() },
                        onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                        onAddToPlaylist = { mainViewModel.openAddToPlaylistDialog(it) }
                    )
                    1 -> AlbumsTab(
                        albums = albums,
                        allSongs = songs,
                        themeColor = themeColor,
                        onAlbumClick = { _, albumSongs ->
                            if (albumSongs.isNotEmpty()) playerViewModel.playSongList(albumSongs, 0)
                        }
                    )
                    2 -> ArtistsTab(
                        artists = artists,
                        allSongs = songs,
                        themeColor = themeColor,
                        onArtistClick = { _, artistSongs ->
                            if (artistSongs.isNotEmpty()) playerViewModel.playSongList(artistSongs, 0)
                        }
                    )
                    3 -> PlaylistsTab(
                        playlists = playlists,
                        allSongs = songs,
                        themeColor = themeColor,
                        onCreatePlaylistClick = { mainViewModel.openCreatePlaylistDialog() },
                        onPlaylistClick = { playlistName, list ->
                            if (playlistName == "Favorites" && list.isNotEmpty()) {
                                playerViewModel.playSongList(list, 0)
                            }
                        },
                        onDeletePlaylist = { mainViewModel.deletePlaylist(it) }
                    )
                    4 -> FoldersTab(
                        folders = folders,
                        allSongs = songs,
                        themeColor = themeColor,
                        onFolderClick = { _, folderSongs ->
                            if (folderSongs.isNotEmpty()) playerViewModel.playSongList(folderSongs, 0)
                        }
                    )
                    5 -> ExtensionsTab(
                        plugins = plugins,
                        searchResults = searchResults,
                        isSearching = isSearchingOnline,
                        themeColor = themeColor,
                        onSearchQuery = { query -> mainViewModel.searchOnlineSongs(query) },
                        onInstallFromUrl = { url, callback -> mainViewModel.installPluginFromUrl(url, callback) },
                        onInstallFromCode = { code, name, callback -> mainViewModel.installPluginFromCode(code, name, callback) },
                        onInstallFromLocalUri = { uri, fileName, callback -> mainViewModel.installPluginFromLocalUri(uri, fileName, callback) },
                        onTogglePlugin = { pluginId -> mainViewModel.togglePlugin(pluginId) },
                        onDeletePlugin = { pluginId -> mainViewModel.deletePlugin(pluginId) },
                        onPlayOnlineSong = { song -> playerViewModel.playSongList(listOf(song), 0) }
                    )
                }
            }
        }

        // Floating Mini-Player at bottom
        if (currentSong != null) {
            MiniPlayerBar(
                song = currentSong,
                isPlaying = isPlaying,
                isLoading = isLoading,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                themeColor = themeColor,
                onBarClick = { playerViewModel.setNowPlayingExpanded(true) },
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onNextClick = { playerViewModel.playNext() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }

        // Fullscreen Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingSheet(
                song = currentSong,
                isPlaying = isPlaying,
                isLoading = isLoading,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                sleepTimerMsLeft = sleepTimerMsLeft,
                allSongs = songs,
                activeThemeColor = themeColor,
                onThemeChange = { mainViewModel.setThemeColor(it) },
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onNextClick = { playerViewModel.playNext() },
                onPreviousClick = { playerViewModel.playPrevious() },
                onSeek = { playerViewModel.seekTo(it) },
                onToggleRepeat = { playerViewModel.toggleRepeatMode() },
                onToggleShuffle = { playerViewModel.toggleShuffle() },
                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                onOpenEqualizer = { playerViewModel.setShowEqualizerDialog(true) },
                onOpenSleepTimer = { playerViewModel.setShowSleepTimerDialog(true) },
                onScanForSongs = { mainViewModel.refreshMusicLibrary() },
                onSelectSong = { targetSong ->
                    val index = songs.indexOfFirst { it.id == targetSong.id }
                    if (index >= 0) {
                        playerViewModel.playSongList(songs, index)
                    }
                },
                onDismiss = { playerViewModel.setNowPlayingExpanded(false) }
            )
        }

        // Equalizer Dialog
        if (showEqualizerDialog) {
            EqualizerDialog(
                info = equalizerInfo,
                onPresetSelected = { playerViewModel.setEqualizerPreset(it) },
                onBandLevelChanged = { band, level -> playerViewModel.setEqualizerBandLevel(band, level) },
                onBassBoostChanged = { playerViewModel.setBassBoost(it) },
                onVirtualizerChanged = { playerViewModel.setVirtualizer(it) },
                onDismiss = { playerViewModel.setShowEqualizerDialog(false) }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                activeTimerMs = sleepTimerMsLeft,
                onSetTimer = { mins, endTrack -> playerViewModel.setSleepTimer(mins, endTrack) },
                onCancelTimer = { playerViewModel.cancelSleepTimer() },
                onDismiss = { playerViewModel.setShowSleepTimerDialog(false) }
            )
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onConfirm = { mainViewModel.createPlaylist(it) },
                onDismiss = { mainViewModel.closeCreatePlaylistDialog() }
            )
        }

        // Add to Playlist Dialog
        if (songToAddToPlaylist != null) {
            AddToPlaylistDialog(
                song = songToAddToPlaylist!!,
                playlists = playlists,
                onSelectPlaylist = { mainViewModel.addSongToPlaylist(it.id, songToAddToPlaylist!!) },
                onCreateNewPlaylist = {
                    mainViewModel.closeAddToPlaylistDialog()
                    mainViewModel.openCreatePlaylistDialog()
                },
                onDismiss = { mainViewModel.closeAddToPlaylistDialog() }
            )
        }
    }
}
