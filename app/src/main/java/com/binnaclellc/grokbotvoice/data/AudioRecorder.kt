package com.binnaclellc.grokbotvoice.data

import android.media.MediaRecorder
import java.io.File

class AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    @Suppress("DEPRECATION")
    fun start(output: File) {
        stopQuietly()
        file = output
        val media = MediaRecorder()
        media.setAudioSource(MediaRecorder.AudioSource.MIC)
        media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        media.setAudioSamplingRate(16_000)
        media.setAudioEncodingBitRate(64_000)
        media.setMaxDuration(45_000)
        media.setOutputFile(output.absolutePath)
        media.prepare()
        media.start()
        recorder = media
    }

    fun stop(): File? {
        val rec = recorder ?: return file
        recorder = null
        return try {
            rec.stop()
            rec.release()
            file
        } catch (_: Exception) {
            rec.release()
            file
        }
    }

    fun cancel() {
        stopQuietly()
        file?.delete()
        file = null
    }

    private fun stopQuietly() {
        val rec = recorder ?: return
        recorder = null
        try {
            rec.stop()
        } catch (_: Exception) {
        }
        rec.release()
    }
}
