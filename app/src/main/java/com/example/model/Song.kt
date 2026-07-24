package com.example.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long, // in milliseconds
    val path: String,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val size: Long = 0, // in bytes
    val dateAdded: Long = 0, // epoch seconds
    val folderName: String = "",
    val folderPath: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false,
    val demoDrawableRes: Int? = null
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: Uri? = null,
    val demoDrawableRes: Int? = null
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

data class Folder(
    val name: String,
    val path: String,
    val songCount: Int
)

enum class SortOrder(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    DATE_ADDED_DESC("Date Added (Newest)"),
    DURATION_DESC("Duration (Longest)"),
    SIZE_DESC("File Size (Largest)")
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
