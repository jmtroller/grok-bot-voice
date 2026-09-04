# Grok Bot Voice

Hold-to-talk Android app for the Binnacle Grok Bot. The phone is mic and speaker. Grok Bot stays the brain via MCP.

**GitHub:** https://github.com/jmtroller/grok-bot-voice  
**Path:** `~/apps/grok-bot-voice`  
**Package:** `com.binnaclellc.grokbotvoice`  
**Not Capacitor.** Native Kotlin / Jetpack Compose, same family as Greenway.

Keep the app open while waiting for a reply (v1 has no FCM).

## Open in Android Studio

1. Install Android Studio with SDK 35.
2. **File → Open** → this folder.
3. Gradle sync → Run on a device (API 26+). Microphone works best on a real phone.

```bash
cd ~/apps/grok-bot-voice
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## First run

On the server that serves `mcp.binnaclellc.com`:

```bash
php artisan admin Podcasts createVoiceDeviceToken
```

That creates `mcp_users` row `voice@binnaclellc.com` (`mcp_service=voice`) and prints a **5–9 digit PIN** (`api_token`). Not the Grok Bot connector token.

In the app: **Settings**

- Server URL: `https://mcp.binnaclellc.com`
- Device token: paste the PIN

There is **no xAI API key** on the phone.

Then create the Grok Bot **webhook routine** on desktop (see `web/Laravel13/docs/binnacle-voice.md`) so the Bot wakes when you talk.

## Use

1. Hold the amber mic button, talk, release.
2. Your transcript appears after STT.
3. Status goes to **Waiting for Grok Bot**.
4. When the Bot calls `voice_speak`, the phone plays the MP3.
5. Tap a Bot line to replay.

If it sits on Waiting for two minutes: the webhook routine did not fire. Check `php artisan admin Podcasts voiceDoorbell` and the Bot’s run history.
