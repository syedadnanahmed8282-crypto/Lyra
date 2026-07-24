package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    /**
     * Scans the device MediaStore for audio files.
     * @param minDurationMs Exclude tracks shorter than this threshold (e.g. 30,000ms = 30s)
     */
    suspend fun scanLocalAudioFiles(minDurationMs: Long = 30_000L): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )

        val selection = "${MediaStore.Audio.Media.SIZE} > 0"
        val selectionArgs = null
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Track"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)
                    val filePath = cursor.getString(dataColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val year = cursor.getInt(yearColumn)
                    val trackNumber = cursor.getInt(trackColumn)

                    // Verify file extensions: mp3, wav, flac, m4a, aac
                    val extension = filePath.substringAfterLast('.', "").lowercase()
                    if (extension in listOf("mp3", "wav", "flac", "m4a", "aac", "ogg")) {
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val albumArtUri = if (albumId > 0) ContentUris.withAppendedId(albumArtBaseUri, albumId) else null

                        val file = File(filePath)
                        val folderName = file.parentFile?.name ?: "Unknown Folder"
                        val folderPath = file.parentFile?.absolutePath ?: ""

                        songList.add(
                            Song(
                                id = id,
                                title = title,
                                artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                                album = if (album == "<unknown>") "Unknown Album" else album,
                                albumId = albumId,
                                duration = duration,
                                path = filePath,
                                contentUri = contentUri,
                                albumArtUri = albumArtUri,
                                size = size,
                                dateAdded = dateAdded,
                                folderName = folderName,
                                folderPath = folderPath,
                                year = year,
                                trackNumber = trackNumber
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Always include high quality demo synth tracks so app has working music immediately on test devices
        val demoSongs = DemoAudioGenerator.getDemoSongs(context)
        return@withContext songList + demoSongs
    }
}
