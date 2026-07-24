package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var musicService: MusicService? = null
    private var isBound = false

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _sleepTimerMsLeft = MutableStateFlow<Long?>(null)
    val sleepTimerMsLeft: StateFlow<Long?> = _sleepTimerMsLeft.asStateFlow()

    private val _equalizerInfo = MutableStateFlow(MusicService.EqualizerInfo())
    val equalizerInfo: StateFlow<MusicService.EqualizerInfo> = _equalizerInfo.asStateFlow()

    // Dialog Visibilities
    private val _isNowPlayingExpanded = MutableStateFlow(true)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _showEqualizerDialog = MutableStateFlow(false)
    val showEqualizerDialog: StateFlow<Boolean> = _showEqualizerDialog.asStateFlow()

    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            _isServiceConnected.value = true
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
            _isServiceConnected.value = false
        }
    }

    init {
        bindMusicService()
    }

    private fun bindMusicService() {
        val intent = Intent(getApplication(), MusicService::class.java)
        getApplication<Application>().startService(intent)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeServiceState() {
        val service = musicService ?: return
        viewModelScope.launch {
            launch { service.currentSong.collect { _currentSong.value = it } }
            launch { service.isPlaying.collect { _isPlaying.value = it } }
            launch { service.isLoading.collect { _isLoading.value = it } }
            launch { service.currentPosition.collect { _currentPosition.value = it } }
            launch { service.duration.collect { _duration.value = it } }
            launch { service.repeatMode.collect { _repeatMode.value = it } }
            launch { service.isShuffle.collect { _isShuffle.value = it } }
            launch { service.sleepTimerMsLeft.collect { _sleepTimerMsLeft.value = it } }
            launch { service.equalizerInfo.collect { _equalizerInfo.value = it } }
        }
    }

    fun playSongList(songs: List<Song>, startIndex: Int) {
        musicService?.setPlaylist(songs, startIndex, true)
    }

    fun togglePlayPause() {
        musicService?.togglePlayPause()
    }

    fun playNext() {
        musicService?.playNext()
    }

    fun playPrevious() {
        musicService?.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        musicService?.seekTo(positionMs)
    }

    fun toggleRepeatMode() {
        musicService?.toggleRepeatMode()
    }

    fun toggleShuffle() {
        musicService?.toggleShuffle()
    }

    fun setSleepTimer(minutes: Int, endOfTrack: Boolean = false) {
        musicService?.setSleepTimer(minutes, endOfTrack)
    }

    fun cancelSleepTimer() {
        musicService?.cancelSleepTimer()
    }

    fun setEqualizerPreset(presetIndex: Short) {
        musicService?.setEqualizerPreset(presetIndex)
    }

    fun setEqualizerBandLevel(band: Short, level: Short) {
        musicService?.setEqualizerBandLevel(band, level)
    }

    fun setBassBoost(strength: Short) {
        musicService?.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Short) {
        musicService?.setVirtualizer(strength)
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun setShowEqualizerDialog(show: Boolean) {
        _showEqualizerDialog.value = show
    }

    fun setShowSleepTimerDialog(show: Boolean) {
        _showSleepTimerDialog.value = show
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}
