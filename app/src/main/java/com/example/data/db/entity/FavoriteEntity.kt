package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)
