package com.binnaclellc.grokbotvoice.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binnaclellc.grokbotvoice.data.AudioPlayer
import com.binnaclellc.grokbotvoice.data.AudioRecorder
import com.binnaclellc.grokbotvoice.data.SettingsStore
import com.binnaclellc.grokbotvoice.data.VoiceApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class TranscriptLine(
    val id: String,
    val fromYou: Boolean,
    val text: String,
    val audioFile: File? = null,
)

class VoiceViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsStore(app)
    private val api = VoiceApi(settings)
    private val recorder = AudioRecorder()
    private val player = AudioPlayer()

    var status by mutableStateOf("Idle")
        private set
    var busy by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var showSettings by mutableStateOf(false)
    var serverUrl by mutableStateOf(settings.serverUrl)
    var deviceToken by mutableStateOf(settings.deviceToken)
    val lines = mutableStateListOf<TranscriptLine>()

    private var pollJob: Job? = null
    private var clipFile: File? = null

    fun saveSettings() {
        settings.serverUrl = serverUrl
        settings.deviceToken = deviceToken
        serverUrl = settings.serverUrl
        deviceToken = settings.deviceToken
        showSettings = false
        status = "Idle"
    }

    fun startRecording() {
        if (busy || recording) return
        if (settings.deviceToken.isBlank()) {
            status = "Set the device token in Settings."
            showSettings = true
            return
        }
        val file = File(getApplication<Application>().cacheDir, "utterance-${System.currentTimeMillis()}.m4a")
        try {
            recorder.start(file)
            clipFile = file
            recording = true
            status = "Recording"
        } catch (e: Exception) {
            status = e.message ?: "Could not start the microphone."
            recording = false
        }
    }

    fun stopRecordingAndSend() {
        if (!recording) return
        recording = false
        val file = recorder.stop()
        if (file == null || !file.exists() || file.length() < 64) {
            status = "Recording was too short."
            return
        }
        busy = true
        status = "Uploading"
        viewModelScope.launch {
            try {
                val turn = withContext(Dispatchers.IO) { api.postUtterance(file) }
                val id = turn.id.orEmpty()
                if (turn.text.isNotBlank()) {
                    lines.add(TranscriptLine(id.ifBlank { "in-${System.currentTimeMillis()}" }, true, turn.text))
                }
                status = "Waiting for Grok Bot"
                waitForReply(id)
            } catch (e: Exception) {
                status = e.message ?: "Upload failed"
                busy = false
            } finally {
                file.delete()
            }
        }
    }

    fun replay(line: TranscriptLine) {
        val file = line.audioFile ?: return
        status = "Speaking"
        player.play(file) {
            if (!busy) status = "Idle"
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        player.stop()
        recorder.cancel()
        super.onCleared()
    }

    private fun waitForReply(afterId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val deadline = System.currentTimeMillis() + 120_000
            var cursor = afterId
            while (isActive && System.currentTimeMillis() < deadline) {
                delay(1_500)
                val page = runCatching {
                    withContext(Dispatchers.IO) { api.getTurns(cursor.ifBlank { null }) }
                }.getOrElse {
                    status = it.message ?: "Poll failed"
                    busy = false
                    return@launch
                }
                for (turn in page.turns) {
                    if (!turn.id.isNullOrBlank()) cursor = turn.id
                    if (turn.direction == "out" && turn.text.isNotBlank() && turn.status == "spoken") {
                        val audio = if (!turn.id.isNullOrBlank() && !turn.audioUrl.isNullOrBlank()) {
                            runCatching {
                                val dest = File(getApplication<Application>().cacheDir, "reply-${turn.id}.mp3")
                                withContext(Dispatchers.IO) { api.downloadAudio(turn.id, dest) }
                            }.getOrNull()
                        } else null
                        val line = TranscriptLine(turn.id ?: "out-${System.currentTimeMillis()}", false, turn.text, audio)
                        lines.add(line)
                        if (audio != null) {
                            status = "Speaking"
                            player.play(audio) {
                                status = "Idle"
                                busy = false
                            }
                        } else {
                            status = "Idle"
                            busy = false
                        }
                        return@launch
                    }
                }
            }
            status = "Grok Bot did not pick up — check the webhook routine."
            busy = false
        }
    }
}
