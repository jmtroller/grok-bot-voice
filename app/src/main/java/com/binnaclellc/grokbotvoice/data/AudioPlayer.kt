package com.binnaclellc.grokbotvoice.data

import android.media.MediaPlayer
import java.io.File

class AudioPlayer {
    private var player: MediaPlayer? = null

    fun play(file: File, onComplete: () -> Unit = {}) {
        stop()
        val media = MediaPlayer()
        media.setDataSource(file.absolutePath)
        media.setOnCompletionListener {
            onComplete()
            stop()
        }
        media.setOnErrorListener { _, _, _ ->
            onComplete()
            stop()
            true
        }
        media.prepare()
        media.start()
        player = media
    }

    fun stop() {
        val p = player ?: return
        player = null
        try {
            p.stop()
        } catch (_: Exception) {
        }
        p.release()
    }
}
