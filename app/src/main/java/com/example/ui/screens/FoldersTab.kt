package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.model.Folder
import com.example.model.Song
import com.example.ui.components.FolderItem
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantPurple

@Composable
fun FoldersTab(
    folders: List<Folder>,
    allSongs: List<Song>,
    onFolderClick: (Folder, List<Song>) -> Unit,
    themeColor: Color = VibrantPurple,
    modifier: Modifier = Modifier
) {
    val isMidnightDark = themeColor == Color(0xFF0A1128)
    if (folders.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = themeColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Folders Found",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isMidnightDark) Color.White else TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No music folders detected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMidnightDark) Color(0xFFA0A5B5) else TextSecondary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("folders_list"),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(folders, key = { it.path }) { folder ->
                val folderSongs = allSongs.filter { it.folderPath == folder.path }
                FolderItem(
                    folder = folder,
                    onFolderClick = { onFolderClick(folder, folderSongs) },
                    themeColor = themeColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
