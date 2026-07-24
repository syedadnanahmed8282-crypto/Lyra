package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.ui.components.formatDuration
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.VibrantPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    sleepTimerMsLeft: Long?,
    allSongs: List<Song> = emptyList(),
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onScanForSongs: () -> Unit = {},
    onSelectSong: (Song) -> Unit = {},
    activeThemeColor: Color = VibrantPurple,
    onThemeChange: (Color) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSong = song ?: allSongs.firstOrNull()
    if (activeSong == null) return
    val currentDisplaySong = activeSong

    val isMidnightDark = activeThemeColor == Color(0xFF0A1128)
    val lightSectionBg = if (isMidnightDark) Color.Black else Color(0xFFEFF2F8)
    val cardBgColor = if (isMidnightDark) Color.Black else PureWhite
    val buttonBgColor = if (isMidnightDark) Color.Black else PureWhite
    val buttonIconTint = if (isMidnightDark) Color.White else activeThemeColor
    val textColorPrimary = if (isMidnightDark) Color.White else TextDarkPrimary
    val textColorSecondary = if (isMidnightDark) Color(0xFFA0A5B5) else TextDarkSecondary
    val tileBgColor = if (isMidnightDark) Color(0xFF0A1128) else Color(0xFFF6F8FC)
    val tileAccentBg = if (isMidnightDark) Color(0xFF142142) else Color(0xFFECE6FE)

    var showAlbumSongsPopup by remember { mutableStateOf(false) }
    var showSongDetailsPopup by remember { mutableStateOf(false) }
    var showSettingsPopup by remember { mutableStateOf(false) }
    var showThemePopup by remember { mutableStateOf(false) }

    // Dynamic state synced with song and library
    var isFavoriteLocal by remember(currentDisplaySong.id, currentDisplaySong.isFavorite, allSongs) {
        mutableStateOf(allSongs.find { it.id == currentDisplaySong.id }?.isFavorite ?: currentDisplaySong.isFavorite)
    }
    var isCheckedLocal by remember(currentDisplaySong.id) { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(lightSectionBg)
            .testTag("now_playing_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ================= TOP CONTAINER =================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.65f)
                    .clip(RoundedCornerShape(bottomStart = 42.dp, bottomEnd = 42.dp))
                    .background(activeThemeColor)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Top Header Bar with 3D Solid White Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Top Left ⚙️ Settings Gear Button
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        ) {
                            IconButton(
                                onClick = { showSettingsPopup = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("open_settings_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = activeThemeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Top Right White Hamburger Menu Button (≡)
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        ) {
                            IconButton(
                                onClick = { onDismiss() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("open_song_info_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Main Library Menu",
                                    tint = activeThemeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // NOW PLAYING Text (Positioned directly above the center circle)
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            fontSize = 15.sp
                        ),
                        color = PureWhite,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 2. Vinyl Album Artwork flanked by Heart (♥) and Checkmark (✓)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Left Heart ♥ Action Button (Fixed & Working)
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        ) {
                            IconButton(onClick = {
                                val newFav = !isFavoriteLocal
                                isFavoriteLocal = newFav
                                onToggleFavorite(currentDisplaySong.copy(isFavorite = newFav))
                            }) {
                                Icon(
                                    imageVector = if (isFavoriteLocal) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavoriteLocal) Color(0xFFFF4081) else activeThemeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Center Vinyl Album Disc (Resized for clean vertical hierarchy)
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer translucent ring
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0x28FFFFFF))
                                    .border(1.5.dp, Color(0x4DFFFFFF), CircleShape)
                            )

                            // Inner rotating vinyl disc
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.86f)
                                    .clip(CircleShape)
                                    .background(PureWhite)
                                    .padding(4.dp)
                                    .rotate(if (isPlaying) rotationAngle else 0f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(PureWhite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentDisplaySong.demoDrawableRes != null) {
                                        Image(
                                            painter = painterResource(id = currentDisplaySong.demoDrawableRes),
                                            contentDescription = "Album Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (currentDisplaySong.albumArtUri != null) {
                                        AsyncImage(
                                            model = currentDisplaySong.albumArtUri,
                                            contentDescription = "Album Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = activeThemeColor,
                                            modifier = Modifier.size(52.dp)
                                        )
                                    }

                                    // Center Spindle Hole
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(TextDarkPrimary)
                                    )
                                }
                            }
                        }

                        // Right Checkmark ✓ Action Button (Fixed to toggle checked state)
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        ) {
                            IconButton(onClick = { isCheckedLocal = !isCheckedLocal }) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Check",
                                    tint = if (isCheckedLocal) Color(0xFF4CAF50) else activeThemeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 3. Song Title & Artist Info (Lifted Higher Up above bottom seam)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 54.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Text(
                            text = currentDisplaySong.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp
                            ),
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${currentDisplaySong.artist} • ${currentDisplaySong.album}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xD0FFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ================= BOTTOM LIGHT BLUE-WHITE CONTAINER =================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .background(lightSectionBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Clearance for overlapping 3D control buttons
                    Spacer(modifier = Modifier.height(36.dp))

                    // 4. Progress Bar & Timestamps (Elevated Higher) + Album Songs Popup Button
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(currentPositionMs),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Color(0xFF8895AD)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDuration(durationMs),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFF8895AD)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Album Songs Popup Button beside timer
                                Surface(
                                    shape = CircleShape,
                                    color = PureWhite,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { showAlbumSongsPopup = true }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = "Album Tracks",
                                            tint = activeThemeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Custom Progress Slider with 3D Spherical Thumb (White circle + inner purple dot)
                        Slider(
                            value = currentPositionMs.coerceAtMost(durationMs).toFloat(),
                            onValueChange = { onSeek(it.toLong()) },
                            valueRange = 0f..durationMs.coerceAtLeast(1000L).toFloat(),
                            colors = SliderDefaults.colors(
                                activeTrackColor = activeThemeColor,
                                inactiveTrackColor = Color(0xFFD3D9E8)
                            ),
                            thumb = {
                                Surface(
                                    shape = CircleShape,
                                    color = PureWhite,
                                    shadowElevation = 6.dp,
                                    border = BorderStroke(1.5.dp, Color(0x337047EB)),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(activeThemeColor)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("song_seek_bar")
                        )
                    }

                    // 5. Tool Icons Row (Shuffle, Repeat, Equalizer, Sleep Timer) BELOW Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) activeThemeColor else TextDarkMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = onToggleRepeat) {
                            Icon(
                                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode != RepeatMode.OFF) activeThemeColor else TextDarkMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = onOpenEqualizer) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Equalizer",
                                tint = activeThemeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = onOpenSleepTimer) {
                            BadgedBox(
                                badge = {
                                    if (sleepTimerMsLeft != null) {
                                        Badge(containerColor = activeThemeColor)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerMsLeft != null) activeThemeColor else TextDarkMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // ================= OVERLAPPING 3D CONTROL BUTTONS (⏪ ⏸️/▶️ ⏩) =================
                // 40% overlaps top purple section, 60% rests on light section
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-38).dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 3D Spherical Previous Button (⏪)
                    Surface(
                        shape = CircleShape,
                        color = PureWhite,
                        shadowElevation = 10.dp,
                        border = BorderStroke(1.5.dp, Color(0x337047EB)),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    ) {
                        IconButton(
                            onClick = onPreviousClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Previous",
                                tint = activeThemeColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 3D Spherical Play / Pause Prominent FAB (⏸️ / ▶️)
                    Surface(
                        shape = CircleShape,
                        color = PureWhite,
                        shadowElevation = 16.dp,
                        border = BorderStroke(2.dp, Color(0x447047EB)),
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .testTag("now_playing_fab")
                    ) {
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = activeThemeColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // 3D Spherical Next Button (⏩)
                    Surface(
                        shape = CircleShape,
                        color = PureWhite,
                        shadowElevation = 10.dp,
                        border = BorderStroke(1.5.dp, Color(0x337047EB)),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    ) {
                        IconButton(
                            onClick = onNextClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Next",
                                tint = activeThemeColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // ================= POPUP 1: Album Songs List =================
        if (showAlbumSongsPopup) {
            val albumSongs = remember(currentDisplaySong, allSongs) {
                val matched = allSongs.filter { it.album.equals(currentDisplaySong.album, ignoreCase = true) }
                if (matched.isNotEmpty()) matched else allSongs
            }

            Dialog(onDismissRequest = { showAlbumSongsPopup = false }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = activeThemeColor,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentDisplaySong.album,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextDarkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${albumSongs.size} Songs in Album",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextDarkSecondary
                                    )
                                }
                            }

                            IconButton(onClick = { showAlbumSongsPopup = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextDarkSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            items(albumSongs) { item ->
                                val isCurrent = item.id == currentDisplaySong.id
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCurrent) activeThemeColor.copy(alpha = 0.15f) else Color(0xFFF6F8FC),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            onSelectSong(item)
                                            showAlbumSongsPopup = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isCurrent) activeThemeColor else Color(0xFFE2E7F4)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Playing",
                                                    tint = PureWhite,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = activeThemeColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isCurrent) activeThemeColor else TextDarkPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextDarkSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = formatDuration(item.duration),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextDarkSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= POPUP 2: Hamburger Menu (≡) Song & Artist Details =================
        if (showSongDetailsPopup) {
            Dialog(onDismissRequest = { showSongDetailsPopup = false }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Track Information",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextDarkPrimary
                            )
                            IconButton(onClick = { showSongDetailsPopup = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextDarkSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mini Cover
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(activeThemeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentDisplaySong.demoDrawableRes != null) {
                                Image(
                                    painter = painterResource(id = currentDisplaySong.demoDrawableRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = activeThemeColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentDisplaySong.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextDarkPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = currentDisplaySong.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = activeThemeColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Info rows
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF6F8FC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Album", style = MaterialTheme.typography.bodySmall, color = TextDarkSecondary)
                                    Text(currentDisplaySong.album, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextDarkPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Duration", style = MaterialTheme.typography.bodySmall, color = TextDarkSecondary)
                                    Text(formatDuration(currentDisplaySong.duration), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextDarkPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Format", style = MaterialTheme.typography.bodySmall, color = TextDarkSecondary)
                                    Text("MP3 • 320 kbps", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextDarkPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFECE6FE),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val newFav = !isFavoriteLocal
                                        isFavoriteLocal = newFav
                                        onToggleFavorite(currentDisplaySong.copy(isFavorite = newFav))
                                        showSongDetailsPopup = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isFavoriteLocal) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = activeThemeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFavoriteLocal) "Favorited" else "Favorite",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = activeThemeColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFECE6FE),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        showSongDetailsPopup = false
                                        onOpenEqualizer()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = null,
                                        tint = activeThemeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Equalizer",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = activeThemeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= SETTINGS & FEATURES DIALOG (⚙️) =================
        if (showSettingsPopup) {
            val context = LocalContext.current
            Dialog(onDismissRequest = { showSettingsPopup = false }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = activeThemeColor.copy(alpha = 0.12f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = activeThemeColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Settings & Features",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextDarkPrimary
                                )
                            }

                            IconButton(onClick = { showSettingsPopup = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextDarkSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 1. Scan for Song
                        SettingsFeatureTile(
                            icon = Icons.Default.Sync,
                            title = "Scan for Song",
                            subtitle = "Rescan storage & update audio library",
                            themeColor = activeThemeColor,
                            onClick = {
                                showSettingsPopup = false
                                onScanForSongs()
                                Toast.makeText(context, "Scanning storage for audio files...", Toast.LENGTH_SHORT).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Audio Effect (Equalizer)
                        SettingsFeatureTile(
                            icon = Icons.Default.Equalizer,
                            title = "Audio Effect",
                            subtitle = "5-band equalizer, bass boost & sound presets",
                            themeColor = activeThemeColor,
                            onClick = {
                                showSettingsPopup = false
                                onOpenEqualizer()
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Sleep Mode (Sleep Timer)
                        SettingsFeatureTile(
                            icon = Icons.Default.Timer,
                            title = "Sleep Mode",
                            subtitle = if (sleepTimerMsLeft != null) "Active timer: ${formatDuration(sleepTimerMsLeft)}" else "Auto stop music playback timer",
                            themeColor = activeThemeColor,
                            onClick = {
                                showSettingsPopup = false
                                onOpenSleepTimer()
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. Themes
                        SettingsFeatureTile(
                            icon = Icons.Default.Palette,
                            title = "Themes",
                            subtitle = "Customize player color theme",
                            themeColor = activeThemeColor,
                            onClick = {
                                showSettingsPopup = false
                                showThemePopup = true
                            }
                        )
                    }
                }
            }
        }

        // ================= THEME CHOOSER DIALOG =================
        if (showThemePopup) {
            val themeOptions = listOf(
                ThemeOption("Vibrant Purple", VibrantPurple, "Royal Purple Theme"),
                ThemeOption("Midnight Dark", Color(0xFF0A1128), "Deep Dark Royal Blue"),
                ThemeOption("Ocean Cyan", Color(0xFF00838F), "Refreshing Sea Cyan"),
                ThemeOption("Sunset Rose", Color(0xFFC2185B), "Warm Crimson Red"),
                ThemeOption("Emerald Green", Color(0xFF2E7D32), "Nature Forest Green")
            )

            Dialog(onDismissRequest = { showThemePopup = false }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Choose Theme Color",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextDarkPrimary
                            )
                            IconButton(onClick = { showThemePopup = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextDarkSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        themeOptions.forEach { option ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (activeThemeColor == option.color) option.color.copy(alpha = 0.15f) else Color(0xFFF6F8FC),
                                border = if (activeThemeColor == option.color) BorderStroke(1.5.dp, option.color) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onThemeChange(option.color)
                                        showThemePopup = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(option.color)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextDarkPrimary
                                        )
                                        Text(
                                            text = option.desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextDarkSecondary
                                        )
                                    }
                                    if (activeThemeColor == option.color) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = option.color,
                                            modifier = Modifier.size(20.dp)
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
}

@Composable
private fun SettingsFeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    themeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF6F8FC),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDarkPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDarkSecondary
                )
            }
        }
    }
}

private data class ThemeOption(val name: String, val color: Color, val desc: String)
