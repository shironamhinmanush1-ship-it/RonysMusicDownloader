# Rony's Music Downloader

Android Studio project using Android framework APIs only; no third-party Android libraries are required.

## Implemented
- Built-in Suno WebView browser
- Clipboard paste
- Best-effort detection of direct HTTPS MP3/WAV request URLs
- Direct HTTPS MP3/WAV downloads using Android DownloadManager
- Download notification and Music/RonysMusicDownloader destination
- Runtime storage permission for Android 6–9
- URL validation, WebView back navigation and state restoration

## Build
- JDK 17
- Android SDK Platform 35 and Build Tools 35
- Android Gradle Plugin 8.7.3 / Gradle 8.9

Open this folder in Android Studio, sync Gradle, then choose Build > Build Bundle(s) / APK(s) > Build APK(s).
The debug APK output is app/build/outputs/apk/debug/app-debug.apk.

## Known technical limits
A Suno webpage URL is not a direct audio-file URL. This app accepts direct HTTPS URLs whose path ends in .mp3 or .wav. Blob/HLS/DASH streams are not handled. WebView network URL detection is best-effort and some hosts may require authentication headers or expiring session URLs that Android DownloadManager cannot reuse. Device runtime testing remains required.
