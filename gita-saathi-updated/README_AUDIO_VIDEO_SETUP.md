# Gita Saathi — Audio & Video Setup (read this first)

## What changed in this build

**1. Removed the "singing" instruction from Gemini TTS prompts** (`GeminiService.kt`).
Gemini's TTS models are speech synthesizers, not singing/music models — they cannot
produce melody or background music, no matter how the prompt is worded. Asking it
to "sing" just made it guess at odd, unstable delivery. It now asks for a slow,
reverent *recitation* instead, which is something the model can actually do well.

**2. Real chant + ambience audio layering** (`GitaAudioEngine.kt`).
If you drop real recordings into:
- `app/src/main/assets/chants/chapter_N.mp3` — a real chanted/sung shloka per chapter
- `app/src/main/assets/audio/background_ambient.mp3` — one shared quiet ambient bed

...the app plays the real chant first, then Gemini's spoken meaning + modern
example, with the ambience looping quietly underneath both. Neither file is
required — if absent, the app just uses Gemini's spoken recitation with no
ambience, so nothing breaks while you're still sourcing recordings.

**3. Real seek bar** (`AudioPlayerBar.kt`). The player previously had no scrubber.
It now has a draggable progress bar wired to actual playback position, with
live mm:ss labels.

**4. Real AI video, via 8 reusable theme clips** (`VerseAnimationVideo.kt` +
`tools/generate_theme_videos.py`). Generating a unique multi-minute AI video for
every one of 700 verses would cost an estimated **$19,000-$94,000** at current Veo
API pricing (~$0.15-$0.75/sec, videos generated in ~8-second segments). Instead,
every verse is mapped to 1 of 8 spiritual themes (already in your code via
`getVerseVideoTheme()`), and each theme gets one real AI-generated video —
estimated one-time cost **$10-$50 total**. Run:

```bash
pip install google-genai --break-system-packages
export GEMINI_API_KEY="your key"
python tools/generate_theme_videos.py
```

Drop the resulting `theme_0.mp4` ... `theme_7.mp4` into
`app/src/main/assets/videos/`. If they're not there yet, each verse falls back to
the built-in procedural Canvas animation — the app works either way.

## What you still need to decide / do
- Source or record the 18 chapter chant files (or start with 1-2 chapters as a pilot).
- Find/license one ambient background track.
- Run the video script (or skip it — the fallback animation is fine to ship with).
- All 700 verses' text/meaning already come from your existing `GitaData.kt` — this
  build doesn't touch that data, only how it's narrated and shown.

## For future tweaks in Google AI Studio
See `AI_STUDIO_PROMPT.md` — paste it in as a single instruction so AI Studio has the
full context and doesn't reintroduce the "sing a melody" request that Gemini TTS
can't fulfill.
