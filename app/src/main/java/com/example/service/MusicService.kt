package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    companion object {
        const val CHANNEL_ID = "lyra_music_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_PLAY_PAUSE = "com.example.lyra.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.lyra.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.lyra.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.lyra.ACTION_STOP"
    }

    private val binder = MusicBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Playback State
    private var currentPlaylist = listOf<Song>()
    private var currentIndex = -1
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    // Sleep Timer State
    private var sleepTimer: CountDownTimer? = null
    private val _sleepTimerMsLeft = MutableStateFlow<Long?>(null)
    val sleepTimerMsLeft: StateFlow<Long?> = _sleepTimerMsLeft.asStateFlow()
    private var pauseOnTrackEnd = false

    // Equalizer State Data
    data class EqualizerInfo(
        val isEnabled: Boolean = false,
        val numberOfBands: Short = 0,
        val minLevel: Short = -1500,
        val maxLevel: Short = 1500,
        val centerFreqs: List<Int> = emptyList(), // in Hz
        val bandLevels: List<Short> = emptyList(), // in mB
        val presets: List<String> = emptyList(),
        val currentPreset: Short = -1,
        val bassStrength: Short = 0,
        val virtualizerStrength: Short = 0
    )

    private val _equalizerInfo = MutableStateFlow(EqualizerInfo())
    val equalizerInfo: StateFlow<EqualizerInfo> = _equalizerInfo.asStateFlow()

    // Position progress tracking job
    private var positionUpdateJob: Job? = null

    private val attributionContext: Context by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            createAttributionContext("media_playback")
        } else {
            this
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = attributionContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupMediaSession()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> togglePlayPause()
                ACTION_NEXT -> playNext()
                ACTION_PREVIOUS -> playPrevious()
                ACTION_STOP -> stopPlayback()
            }
        }
        return START_NOT_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(attributionContext, "LyraMusicService").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { playNext() }
                override fun onSkipToPrevious() { playPrevious() }
                override fun onSeekTo(pos: Long) { seekTo(pos) }
                override fun onStop() { stopPlayback() }
            })
            isActive = true
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int, autoPlay: Boolean = true) {
        currentPlaylist = songs
        currentIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        if (currentPlaylist.isNotEmpty()) {
            playSong(currentPlaylist[currentIndex], autoPlay)
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        _currentSong.value = song
        _currentPosition.value = 0L
        _duration.value = song.duration

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                val uriString = song.contentUri.toString()
                if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    val headers = HashMap<String, String>()
                    headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    headers["Accept"] = "*/*"
                    headers["Connection"] = "keep-alive"
                    setDataSource(applicationContext, song.contentUri, headers)
                } else {
                    setDataSource(applicationContext, song.contentUri)
                }
                setOnPreparedListener(this@MusicService)
                setOnCompletionListener(this@MusicService)
                setOnErrorListener(this@MusicService)
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        initEqualizer(mp?.audioSessionId ?: 0)
        _duration.value = mp?.duration?.toLong() ?: _currentSong.value?.duration ?: 0L

        if (requestAudioFocus()) {
            mp?.start()
            _isPlaying.value = true
            startPositionUpdates()
            updateMediaSessionState(PlaybackState.STATE_PLAYING)
            updateNotification()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopPositionUpdates()
            updateMediaSessionState(PlaybackState.STATE_PAUSED)
            updateNotification()
        } else {
            if (requestAudioFocus()) {
                player.start()
                _isPlaying.value = true
                startPositionUpdates()
                updateMediaSessionState(PlaybackState.STATE_PLAYING)
                updateNotification()
            }
        }
    }

    fun playNext() {
        if (currentPlaylist.isEmpty()) return
        if (_repeatMode.value == RepeatMode.ONE && mediaPlayer?.isPlaying == true) {
            seekTo(0)
            return
        }

        if (_isShuffle.value) {
            currentIndex = (0 until currentPlaylist.size).random()
        } else {
            currentIndex = (currentIndex + 1) % currentPlaylist.size
        }
        playSong(currentPlaylist[currentIndex])
    }

    fun playPrevious() {
        if (currentPlaylist.isEmpty()) return
        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            seekTo(0)
            return
        }
        if (_isShuffle.value) {
            currentIndex = (0 until currentPlaylist.size).random()
        } else {
            currentIndex = if (currentIndex - 1 < 0) currentPlaylist.size - 1 else currentIndex - 1
        }
        playSong(currentPlaylist[currentIndex])
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _currentPosition.value = positionMs
        updateMediaSessionState(if (_isPlaying.value) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    override fun onCompletion(mp: MediaPlayer?) {
        stopPositionUpdates()
        _isPlaying.value = false
        if (pauseOnTrackEnd) {
            pauseOnTrackEnd = false
            _sleepTimerMsLeft.value = null
            updateNotification()
            return
        }

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mp?.start()
                _isPlaying.value = true
                startPositionUpdates()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (currentIndex < currentPlaylist.size - 1) {
                    playNext()
                } else {
                    updateMediaSessionState(PlaybackState.STATE_PAUSED)
                    updateNotification()
                }
            }
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        _isPlaying.value = false
        stopPositionUpdates()
        return true
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = serviceScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition.toLong()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // Audio Focus Handling
    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }.build()
            am.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus({ focusChange ->
                handleAudioFocusChange(focusChange)
            }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (_isPlaying.value) togglePlayPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_isPlaying.value) togglePlayPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                if (!_isPlaying.value) togglePlayPause()
            }
        }
    }

    // Sleep Timer
    fun setSleepTimer(minutes: Int, endOfTrack: Boolean = false) {
        sleepTimer?.cancel()
        pauseOnTrackEnd = endOfTrack

        if (endOfTrack) {
            _sleepTimerMsLeft.value = -1L
            return
        }

        if (minutes <= 0) {
            _sleepTimerMsLeft.value = null
            return
        }

        val totalMs = minutes * 60 * 1000L
        _sleepTimerMsLeft.value = totalMs

        sleepTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerMsLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _sleepTimerMsLeft.value = null
                if (_isPlaying.value) {
                    togglePlayPause()
                }
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        pauseOnTrackEnd = false
        _sleepTimerMsLeft.value = null
    }

    // Equalizer Integration
    private fun initEqualizer(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }

            try {
                bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
            } catch (e: Exception) { e.printStackTrace() }

            try {
                virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
            } catch (e: Exception) { e.printStackTrace() }

            updateEqualizerInfo()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateEqualizerInfo() {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands
        val range = eq.bandLevelRange
        val centerFreqs = (0 until numBands).map { eq.getCenterFreq(it.toShort()) / 1000 }
        val levels = (0 until numBands).map { eq.getBandLevel(it.toShort()) }
        val presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }

        _equalizerInfo.value = EqualizerInfo(
            isEnabled = eq.enabled,
            numberOfBands = numBands,
            minLevel = range[0],
            maxLevel = range[1],
            centerFreqs = centerFreqs,
            bandLevels = levels,
            presets = presets,
            currentPreset = eq.currentPreset,
            bassStrength = bassBoost?.roundedStrength ?: 0,
            virtualizerStrength = virtualizer?.roundedStrength ?: 0
        )
    }

    fun setEqualizerPreset(presetIndex: Short) {
        equalizer?.usePreset(presetIndex)
        updateEqualizerInfo()
    }

    fun setEqualizerBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
        updateEqualizerInfo()
    }

    fun setBassBoost(strength: Short) {
        bassBoost?.setStrength(strength)
        updateEqualizerInfo()
    }

    fun setVirtualizer(strength: Short) {
        virtualizer?.setStrength(strength)
        updateEqualizerInfo()
    }

    // Notification Controls
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lyra Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lyra Offline Music Player media controls"
                setShowBadge(false)
            }
            val manager = attributionContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val song = _currentSong.value ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
        val prevIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS }

        val pPlayPause = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pNext = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pPrev = PendingIntent.getService(this, 3, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val albumBitmap = getAlbumArtBitmap(song)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText("${song.artist} • ${song.album}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(albumBitmap)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(_isPlaying.value)
            .addAction(android.R.drawable.ic_media_previous, "Previous", pPrev)
            .addAction(
                if (_isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (_isPlaying.value) "Pause" else "Play",
                pPlayPause
            )
            .addAction(android.R.drawable.ic_media_next, "Next", pNext)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getAlbumArtBitmap(song: Song): Bitmap {
        if (song.demoDrawableRes != null) {
            try {
                return BitmapFactory.decodeResource(resources, song.demoDrawableRes)
            } catch (e: Exception) { e.printStackTrace() }
        }

        if (song.albumArtUri != null) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(song.albumArtUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) return bitmap
            } catch (e: Exception) { e.printStackTrace() }
        }

        val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFF2A1245.toInt())
        return bmp
    }

    private fun updateMediaSessionState(state: Int) {
        val song = _currentSong.value
        val playState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO
            )
            .setState(state, _currentPosition.value, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playState)

        if (song != null) {
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, song.duration)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, getAlbumArtBitmap(song))
                .build()
            mediaSession?.setMetadata(metadata)
        }
    }

    fun stopPlayback() {
        stopPositionUpdates()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        sleepTimer?.cancel()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        mediaSession?.release()
    }
}
