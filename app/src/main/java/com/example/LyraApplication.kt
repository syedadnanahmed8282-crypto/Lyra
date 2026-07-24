package com.example

import android.app.Application
import android.os.Build
import com.example.data.db.LyraDatabase
import com.example.data.extension.ExtensionManager
import com.example.repository.MusicRepository

class LyraApplication : Application() {

    val database: LyraDatabase by lazy {
        LyraDatabase.getDatabase(this)
    }

    val repository: MusicRepository by lazy {
        MusicRepository(
            context = this,
            favoriteDao = database.favoriteDao(),
            playlistDao = database.playlistDao()
        )
    }

    val extensionManager: ExtensionManager by lazy {
        ExtensionManager(this)
    }
}
