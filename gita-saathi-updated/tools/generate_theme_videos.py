#!/usr/bin/env python3
"""
generate_theme_videos.py
-------------------------
Generates the 8 real, AI-generated theme videos used across all 700 Gita verses
(see app/src/main/assets/videos/README.md for how they're mapped to verses).

WHY 8 VIDEOS AND NOT 700:
Google's Veo video API is billed per second of output (roughly $0.15-$0.75/sec
depending on model tier, as of mid-2026 — check current pricing at
https://ai.google.dev/gemini-api/docs/pricing before running this). A unique
2-3 minute video for each of 700 verses would cost an estimated $19,000-$94,000.
Generating 8 short (~8 second) thematic loops once, and reusing them across all
verses that share a theme, costs roughly $10-$50 total instead.

WHAT THIS SCRIPT DOES:
1. Calls the Gemini API's video generation (Veo) endpoint once per theme.
2. Polls the long-running operation until each video is ready.
3. Downloads and saves each clip as app/src/main/assets/videos/theme_N.mp4.

BEFORE YOU RUN THIS:
- pip install google-genai
- export GEMINI_API_KEY="your key here" (the same key you already added to the app)
- Check current Veo model names/pricing — they change. As of writing, common
  options are "veo-3.1-generate-preview" and "veo-3.1-fast-generate-preview"
  (fast = cheaper, slightly lower fidelity). Update MODEL_NAME below if needed.
- This WILL charge your Google Cloud/API billing account. Estimated total for
  8 clips at ~8 seconds each on the Fast tier: roughly $10-$20. Double-check the
  current per-second rate for your chosen model before running.

Run once. Re-run only if you want to regenerate a theme (delete its old file first
or it will just overwrite it).
"""

import os
import sys
import time

try:
    from google import genai
except ImportError:
    sys.exit("Missing dependency. Run: pip install google-genai --break-system-packages")

API_KEY = os.environ.get("GEMINI_API_KEY")
if not API_KEY:
    sys.exit("Set GEMINI_API_KEY in your environment first.")

# Cheaper/faster tier by default — swap to "veo-3.1-generate-preview" for higher
# fidelity at a higher per-second cost. Verify current model IDs at
# https://ai.google.dev/gemini-api/docs/video before running.
MODEL_NAME = "veo-3.1-fast-generate-preview"

OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..", "app", "src", "main", "assets", "videos"
)

# One prompt per theme, matching getVerseVideoTheme() in VerseAnimationVideo.kt.
# Kept abstract/symbolic (mandalas, light, energy) — no depictions of deities or
# real people, which keeps content generation policy-safe and devotionally
# tasteful for all 18 chapters.
THEME_PROMPTS = {
    0: "A slow, seamless-looping cinematic animation of a golden Om symbol at the "
       "center of a rotating twelve-petal mandala, warm orange and amber light, "
       "soft particle glow, serene devotional atmosphere, no text, no people.",
    1: "A slow cosmic zoom through a purple and violet galaxy spiral, glowing stars "
       "drifting outward from a bright center, tranquil and vast, seamless loop, "
       "no text, no people.",
    2: "A lotus flower slowly blooming with a warm flame glowing at its center, "
       "gentle floating motion, deep orange and gold tones, soft bokeh light, "
       "seamless loop, no text, no people.",
    3: "Teal and turquoise light rays radiating outward from a glowing center point "
       "in a slow pulsing rhythm, energy-field aesthetic, calm meditative pace, "
       "seamless loop, no text, no people.",
    4: "A large ornate wheel with twelve spokes slowly rotating, glowing pink and "
       "magenta light along its rim, symbolic and abstract, seamless loop, no "
       "text, no people.",
    5: "Concentric golden light waves slowly rippling outward across a dark "
       "background, warm yellow glow, tranquil and hypnotic, seamless loop, no "
       "text, no people.",
    6: "Two overlapping glowing squares slowly counter-rotating, deep blue light "
       "rays like sunbeams behind them, calm cosmic atmosphere, seamless loop, no "
       "text, no people.",
    7: "Warm green sparks of light drifting upward and outward beneath a canopy of "
       "glowing leaves, tranquil forest-light atmosphere, seamless loop, no text, "
       "no people.",
}

DURATION_SECONDS = 8  # Veo's per-call max as of writing; loop it in-app via isLooping.


def generate_theme(client, theme_id: int, prompt: str):
    out_path = os.path.join(OUTPUT_DIR, f"theme_{theme_id}.mp4")
    print(f"\n[theme {theme_id}] starting generation...")

    operation = client.models.generate_videos(
        model=MODEL_NAME,
        prompt=prompt,
    )

    while not operation.done:
        print(f"[theme {theme_id}] waiting for render...")
        time.sleep(10)
        operation = client.operations.get(operation)

    video = operation.response.generated_videos[0]
    client.files.download(file=video.video)
    video.video.save(out_path)
    print(f"[theme {theme_id}] saved -> {out_path}")


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    client = genai.Client(api_key=API_KEY)

    print(f"Generating {len(THEME_PROMPTS)} theme videos with model '{MODEL_NAME}'.")
    print("This will incur real API charges — see the header comment for estimates.")
    confirm = input("Type 'yes' to continue: ").strip().lower()
    if confirm != "yes":
        print("Cancelled.")
        return

    for theme_id, prompt in THEME_PROMPTS.items():
        out_path = os.path.join(OUTPUT_DIR, f"theme_{theme_id}.mp4")
        if os.path.exists(out_path):
            print(f"[theme {theme_id}] already exists, skipping (delete file to regenerate).")
            continue
        try:
            generate_theme(client, theme_id, prompt)
        except Exception as e:
            print(f"[theme {theme_id}] FAILED: {e}")

    print("\nDone. Rebuild/reinstall the app so the new assets are picked up.")


if __name__ == "__main__":
    main()
