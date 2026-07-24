package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.VibrantPurple

@Composable
fun SongItem(
    song: Song,
    isPlayingCurrent: Boolean,
    isPlaying: Boolean = true,
    themeColor: Color = VibrantPurple,
    onSongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayPauseToggle: () -> Unit = onSongClick,
    onAddToPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMidnightDark = themeColor == Color(0xFF0A1128)
    val inactiveTitleColor = if (isMidnightDark) Color.White else TextDarkPrimary
    val inactiveSubtitleColor = if (isMidnightDark) Color(0xFFA0A5B5) else TextDarkSecondary
    val buttonBgColor = if (isMidnightDark) Color.Black else PureWhite

    if (isPlayingCurrent) {
        // Active Playing Song Top Card (Dynamic Theme Color Container)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = themeColor,
            shadowElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable { onSongClick() }
                .testTag("song_item_${song.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left 3D Circular White Button with Pause / Play Icon
                Surface(
                    shape = CircleShape,
                    color = buttonBgColor,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                ) {
                    IconButton(
                        onClick = { onPlayPauseToggle() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Center Song Info (White Text)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = Color(0xDDFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right 3D Circular White Button with Heart Icon
                Surface(
                    shape = CircleShape,
                    color = buttonBgColor,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("favorite_button_${song.id}")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color(0xFFFF4081) else themeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Inactive / Regular Song Item (Soft 3D Circular Play & Heart Button)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onSongClick() }
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .testTag("song_item_${song.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left 3D Circular White Button with Play Icon
            Surface(
                shape = CircleShape,
                color = buttonBgColor,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            ) {
                IconButton(
                    onClick = onSongClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = themeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center Song Title & Subtitle Info (Dark Text)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = inactiveTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = inactiveSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right 3D Circular White Button with Heart Icon
            Surface(
                shape = CircleShape,
                color = buttonBgColor,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("favorite_button_${song.id}")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) Color(0xFFFF4081) else themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
