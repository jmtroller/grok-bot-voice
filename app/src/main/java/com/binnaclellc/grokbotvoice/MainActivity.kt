package com.binnaclellc.grokbotvoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.binnaclellc.grokbotvoice.ui.SettingsScreen
import com.binnaclellc.grokbotvoice.ui.TalkScreen
import com.binnaclellc.grokbotvoice.ui.VoiceViewModel
import com.binnaclellc.grokbotvoice.ui.theme.GrokBotVoiceTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VoiceViewModel by viewModels()

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, getString(R.string.mic_permission), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrokBotVoiceTheme {
                if (viewModel.showSettings || viewModel.deviceToken.isBlank()) {
                    SettingsScreen(vm = viewModel)
                } else {
                    TalkScreen(
                        vm = viewModel,
                        onNeedMic = { ensureMic() },
                    )
                }
            }
        }
    }

    private fun ensureMic(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return true
        micPermission.launch(Manifest.permission.RECORD_AUDIO)
        return false
    }
}
