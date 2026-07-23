Paste this into AI Studio when you want to keep iterating on Gita Saathi there.
It encodes the constraints that caused the last 7-8 rounds of "make it sing" to fail,
so AI Studio doesn't loop on the same impossible request.

---

This is Gita Saathi, an Android app narrating Bhagavad Gita verses. Please respect
these constraints when making changes:

1. AUDIO: Gemini's TTS models (used in GeminiService.kt / GitaAudioEngine.kt) are
   speech synthesizers only — they cannot sing a melody or generate background
   music. Do not add prompt instructions asking the model to "sing." The Sanskrit
   shloka should be requested as a slow, reverent spoken recitation instead. Real
   singing/chanting comes from bundled audio files in assets/chants/chapter_N.mp3
   (one per chapter, optional), which GitaAudioEngine already plays before the
   spoken narration if present. A shared ambience loop from
   assets/audio/background_ambient.mp3 (optional) plays quietly underneath both.

2. VIDEO: Do not attempt to generate a unique AI video per verse at runtime or in
   a live API call — Veo-style video APIs are billed per second (roughly
   $0.15-$0.75/sec) and a 2-3 minute clip per verse across 700 verses would cost
   tens of thousands of dollars. Video is handled via 8 reusable AI-generated
   theme clips (assets/videos/theme_0.mp4 ... theme_7.mp4, generated once offline
   via tools/generate_theme_videos.py), each verse deterministically mapped to one
   theme in getVerseVideoTheme() (VerseAnimationVideo.kt). If a theme file is
   missing, the app falls back to a procedural Canvas animation — keep that
   fallback intact.

3. SEEK BAR: AudioPlayerBar.kt has a real Slider bound to playback position via
   GitaViewModel's playbackPositionMs/playbackDurationMs StateFlows and
   GitaAudioEngine's onProgress callback + seekTo(). Keep this wired correctly if
   you touch playback code.

4. Keep all 700-verse content coming from GitaData.kt unchanged unless the task is
   specifically about verse content.

Now, here's what I'd like changed: <describe your next specific request here>
