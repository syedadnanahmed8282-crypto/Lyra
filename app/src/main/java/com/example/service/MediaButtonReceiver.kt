package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
            if (event.action == KeyEvent.ACTION_DOWN) {
                val serviceIntent = Intent(context, MusicService::class.java)
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                        serviceIntent.action = MusicService.ACTION_PLAY_PAUSE
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        serviceIntent.action = MusicService.ACTION_NEXT
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        serviceIntent.action = MusicService.ACTION_PREVIOUS
                    }
                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        serviceIntent.action = MusicService.ACTION_STOP
                    }
                    else -> return
                }
                context?.startService(serviceIntent)
            }
        }
    }
}
