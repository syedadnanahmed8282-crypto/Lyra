package com.example.data.scanner

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.model.Song
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object DemoAudioGenerator {

    /**
     * Generates PCM WAV files with harmonic synth chords and melodies.
     */
    fun getDemoSongs(context: Context): List<Song> {
        val cacheDir = File(context.cacheDir, "demo_tracks")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val track1 = generateSynthWav(
            file = File(cacheDir, "starlight_echoes.wav"),
            title = "Starlight Echoes",
            artist = "Lyra Synthwave",
            album = "Cosmic Waves",
            baseFreq = 220.0, // A3
            chordNotes = listOf(220.0, 277.18, 329.63, 440.0), // A major 7
            durationSeconds = 45,
            drawableRes = R.drawable.album_starlight_1784810933623
        )

        val track2 = generateSynthWav(
            file = File(cacheDir, "neon_horizon.wav"),
            title = "Neon Horizon",
            artist = "Cyber Pulse",
            album = "Future Sunset",
            baseFreq = 174.61, // F3
            chordNotes = listOf(174.61, 220.0, 261.63, 349.23), // F major
            durationSeconds = 50,
            drawableRes = R.drawable.album_horizon_1784810946330
        )

        val track3 = generateSynthWav(
            file = File(cacheDir, "midnight_groove.wav"),
            title = "Midnight Groove",
            artist = "Lofi Luna",
            album = "Chillhop Nights",
            baseFreq = 146.83, // D3
            chordNotes = listOf(146.83, 174.61, 220.0, 261.63), // D minor 7
            durationSeconds = 60,
            drawableRes = R.drawable.album_midnight_1784810958964
        )

        return listOf(track1, track2, track3)
    }

    private fun generateSynthWav(
        file: File,
        title: String,
        artist: String,
        album: String,
        baseFreq: Double,
        chordNotes: List<Double>,
        durationSeconds: Int,
        drawableRes: Int
    ): Song {
        if (!file.exists() || file.length() < 1000) {
            try {
                val sampleRate = 22050
                val totalSamples = sampleRate * durationSeconds
                val pcmData = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    
                    // Melody arpeggio: switch note every 0.25 seconds
                    val noteIndex = ((t / 0.25).toInt()) % chordNotes.size
                    val freq = chordNotes[noteIndex]
                    
                    // Bass tone
                    val bass = sin(2.0 * Math.PI * (baseFreq / 2.0) * t) * 0.3
                    
                    // Lead synth with sub-harmonics
                    val lead = sin(2.0 * Math.PI * freq * t) * 0.4
                    val harmonic = sin(2.0 * Math.PI * (freq * 2.0) * t) * 0.15
                    
                    // Envelope (gentle fade in and out)
                    val envelope = when {
                        t < 1.0 -> t
                        t > durationSeconds - 2.0 -> (durationSeconds - t) / 2.0
                        else -> 1.0
                    }.coerceIn(0.0, 1.0)

                    val sampleValue = ((lead + bass + harmonic) * envelope * 24000).toInt()
                    pcmData[i] = sampleValue.coerceIn(-32768, 32767).toShort()
                }

                writeWavHeaderAndData(file, pcmData, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val fileUri = Uri.fromFile(file)
        return Song(
            id = file.hashCode().toLong(),
            title = title,
            artist = artist,
            album = album,
            albumId = album.hashCode().toLong(),
            duration = durationSeconds * 1000L,
            path = file.absolutePath,
            contentUri = fileUri,
            albumArtUri = null,
            size = file.length(),
            dateAdded = System.currentTimeMillis() / 1000,
            folderName = "Demo Audio",
            folderPath = file.parent ?: "/demo",
            year = 2026,
            trackNumber = 1,
            isFavorite = false,
            isDemo = true,
            demoDrawableRes = drawableRes
        )
    }

    private fun writeWavHeaderAndData(file: File, pcmData: ShortArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val dataSize = pcmData.size * 2
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size for PCM
            header.putShort(1.toShort()) // AudioFormat 1 = PCM
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort((numChannels * bitsPerSample / 8).toShort()) // BlockAlign
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(dataSize)

            out.write(header.array())

            val dataBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                dataBuffer.putShort(sample)
            }
            out.write(dataBuffer.array())
        }
    }
}
