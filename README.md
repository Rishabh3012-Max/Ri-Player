# Ri Player 🎬

VLC-jaisa native Android media player — Made by ऋSHABH

## Features (v1)
- Local video + audio playback (auto-scans device library)
- Network stream playback (HTTP/HTTPS/RTSP URL)
- Basic 5-band equalizer
- Teal/coral dark theme (Ri browser jaisa)
- ExoPlayer (Media3) core — mp4, mkv, webm, ogg, most common formats natively
- FFmpeg-kit dependency added for extra codec coverage (flac, avi edge cases)

## ⚠️ Important build note
`ffmpeg-kit` library (arthenica) was **archived/retired by its maintainer in 2025** —
maven se pull fail ho sakta hai. Agar build fail ho FFmpeg-kit line pe:
1. `app/build.gradle` se yeh line hata do:
   `implementation 'com.arthenica:ffmpeg-kit-full:6.0-2'`
2. ExoPlayer akela bhi 90%+ formats (mp4, mkv, webm, mp3, aac, flac, ogg) handle kar leta hai —
   sirf kuch rare/legacy codecs (real old avi/wmv variants) miss ho sakte hain.

## How to build
1. Yeh poora folder GitHub repo **Ri-player** mein push karo
2. GitHub → Actions tab → "Build Ri Player APK" workflow apne aap chalega
3. Build complete hone pe Artifacts section se `Ri-player-debug-apk` download karo
4. Phone mein install karo (Unknown sources allow karna padega)

## Roadmap (pending)
- Subtitle support (.srt loading)
- Playlist/queue
- File browser (folder-wise, not just MediaStore scan)
- Background audio playback service
- Chromecast support
