# Pulse Music EQ (Android Music Player + Studio DSP Equalizer)

An Android music player built with **Kotlin**, **Jetpack Compose**, **Media3 ExoPlayer**, and **Android Studio-grade DSP (`DynamicsProcessing` 10-band EQ + Multi-Band Compressor + Master Limiter + 3D Surround)** tuned for punchy, crystal-clear **TikTok-style** sound.

## Features
- **Full Storage Folder Browser:** Scans device storage via `MediaStore` and groups all audio files by folder (`Download`, `WhatsApp Audio`, `Music`, `Recordings`, etc.) with track count, duration, Play All, and Shuffle.
- **TikTok Punch Mastering Engine (`AudioDspEngine`):**
  - Default **TikTok Punch** sound profile (sub-bass punch, mid clarity, crisp vocal presence, multi-band compression, and zero-clip peak limiter).
  - **10-Band Equalizer** (`31Hz` to `16kHz`, `-12dB` to `+12dB`) + presets (`TikTok Punch`, `Bass Extreme`, `Studio Vocal`, `Club / EDM`, `Warm Chill`, `Flat`, `Custom`).
  - **Deep Bass Punch**, **Loudness Gain**, and **3D Stereo Surround**.
- **Background Playback & Lock Screen Controls:** Powered by Media3 `MediaSessionService`.

## Build `.apk` via GitHub Actions
Push `d:\Software\Task\MusicPlayer` to a GitHub repository and download **`Pulse-Music-EQ-Release-APK`** from **Actions → Artifacts**.
