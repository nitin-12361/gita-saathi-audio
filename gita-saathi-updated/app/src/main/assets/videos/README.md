# Real AI-generated theme videos (8 files cover all 700 verses)

Drop generated clips here named exactly:

    theme_0.mp4   theme_1.mp4   theme_2.mp4   theme_3.mp4
    theme_4.mp4   theme_5.mp4   theme_6.mp4   theme_7.mp4

Every verse is deterministically assigned one of these 8 themes (see
`getVerseVideoTheme()` in VerseAnimationVideo.kt — the mapping is
`(chapterId * 31 + verseId) % 8`), so 8 real videos give every single verse in
every chapter a real AI-generated video background, without generating 700
separate multi-minute clips (which would cost ~$19,000–$94,000 — see
tools/generate_theme_videos.py for the real per-second pricing and why).

Run `tools/generate_theme_videos.py` once to generate these 8 with Veo using your
existing Gemini API key (estimated one-time cost: roughly $10–$50 total for all 8).
If a theme_N.mp4 file isn't present, the app automatically falls back to the
built-in procedural animation for that theme — nothing breaks.

The 8 themes, in order (0-7):
0. Sacred Om Mandala
1. Cosmic Vishwaroopa Galaxy
2. Lotus Flame Awakening
3. Chakra Energy Rays
4. Eternal Wheel of Karma
5. Divine Golden Light Waves
6. Cosmic Chariot Rays
7. Sacred Banyan Tree Light
