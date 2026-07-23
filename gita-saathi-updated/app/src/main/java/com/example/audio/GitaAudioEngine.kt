package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.data.AppLanguage
import com.example.data.Verse
import com.example.data.VoiceStyle
import com.example.network.GeminiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GitaAudioEngine(
    private val context: Context,
    private val onPlaybackCompleted: () -> Unit,
    private val onLoadingStateChanged: (Boolean) -> Unit = {},
    private val onAudioError: (String) -> Unit = {},
    // Reports (positionMs, durationMs) roughly every 200ms while something is playing,
    // so the UI can drive a real, draggable seek bar.
    private val onProgress: (Int, Int) -> Unit = { _, _ -> }
) {

    private var mediaPlayer: MediaPlayer? = null
    // Low-volume looping spiritual ambience track that plays alongside narration,
    // if the user has dropped one in assets/audio/background_ambient.mp3.
    private var ambiencePlayer: MediaPlayer? = null
    private var ttsEngine: android.speech.tts.TextToSpeech? = null
    private var isTtsInitialized = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var audioJob: Job? = null
    private var progressJob: Job? = null

    private var currentVerse: Verse? = null
    private var currentLanguage: AppLanguage = AppLanguage.HINDI
    private var currentSpeed: Float = 1.0f
    private var currentVoiceStyle: VoiceStyle = VoiceStyle.DEVOTIONAL

    /**
     * Looks for a real, human-recorded/chanted audio file for this verse's chapter in
     * assets/chants/. Per-chapter (not per-verse) keeps this realistic to actually source:
     * assets/chants/chapter_1.mp3 ... chapter_18.mp3. Falls back to null (skip) if absent,
     * so the app works fine before you've added any recordings.
     */
    private fun findChantAssetPath(chapterId: Int): String? {
        val candidates = listOf(
            "chants/chapter_$chapterId.mp3",
            "chants/chapter_$chapterId.wav"
        )
        return candidates.firstOrNull { path ->
            try {
                context.assets.open(path).close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun findAmbienceAssetPath(): String? {
        val candidates = listOf("audio/background_ambient.mp3", "audio/background_ambient.wav")
        return candidates.firstOrNull { path ->
            try {
                context.assets.open(path).close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun startAmbience(speed: Float) {
        stopAmbience()
        val path = findAmbienceAssetPath() ?: return
        try {
            val afd = context.assets.openFd(path)
            ambiencePlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setVolume(0.18f, 0.18f) // quiet bed under narration, not competing with it
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try { playbackParams = PlaybackParams().setSpeed(speed) } catch (_: Exception) {}
                }
                start()
            }
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "No ambience track playing: ${e.message}")
        }
    }

    private fun stopAmbience() {
        try {
            ambiencePlayer?.release()
        } catch (_: Exception) {}
        ambiencePlayer = null
    }

    private fun startProgressReporting() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (true) {
                val player = mediaPlayer
                if (player != null) {
                    try {
                        if (player.isPlaying) {
                            onProgress(player.currentPosition, player.duration.coerceAtLeast(0))
                        }
                    } catch (_: Exception) {}
                }
                kotlinx.coroutines.delay(200)
            }
        }
    }

    init {
        try {
            ttsEngine = android.speech.tts.TextToSpeech(context.applicationContext) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                }
            }
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "Failed to initialize Android TextToSpeech", e)
        }
    }

    fun playVerse(
        verse: Verse,
        language: AppLanguage = currentLanguage,
        speed: Float = currentSpeed,
        voiceStyle: VoiceStyle = currentVoiceStyle,
        customApiKey: String? = null
    ) {
        currentVerse = verse
        currentLanguage = language
        currentSpeed = speed
        currentVoiceStyle = voiceStyle

        stopAudio()
        onLoadingStateChanged(true)

        audioJob?.cancel()
        audioJob = coroutineScope.launch {
            val result = GeminiClient.getOrFetchTtsAudio(
                context = context,
                verse = verse,
                language = language,
                voiceStyle = voiceStyle,
                customApiKey = customApiKey
            )

            onLoadingStateChanged(false)

            result.fold(
                onSuccess = { audioFile ->
                    startAmbience(speed)
                    val chantPath = findChantAssetPath(verse.chapterId)
                    if (chantPath != null) {
                        // Real recorded/chanted shloka first, then Gemini's spoken meaning + example
                        playAssetThenFile(chantPath, audioFile, speed)
                    } else {
                        playAudioFile(audioFile, speed)
                    }
                },
                onFailure = { error ->
                    Log.e("GitaAudioEngine", "Gemini TTS audio error: ${error.message}. Using System TTS fallback.")
                    speakFallback(verse, language, speed, error.localizedMessage)
                }
            )
        }
    }

    /** Plays a bundled asset (e.g. a real chant recording) to completion, then the given file. */
    private fun playAssetThenFile(assetPath: String, followUpFile: java.io.File, speed: Float) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val afd = context.assets.openFd(assetPath)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try { playbackParams = PlaybackParams().setSpeed(speed) } catch (_: Exception) {}
                }
                setOnCompletionListener {
                    playAudioFile(followUpFile, speed)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("GitaAudioEngine", "Chant asset error: $what, $extra")
                    playAudioFile(followUpFile, speed)
                    true
                }
                start()
            }
            startProgressReporting()
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "Falling back, couldn't play chant asset", e)
            playAudioFile(followUpFile, speed)
        }
    }

    private fun speakFallback(verse: Verse, language: AppLanguage, speed: Float, originalErrorMsg: String? = null) {
        if (!isTtsInitialized || ttsEngine == null) {
            onAudioError(originalErrorMsg ?: "Failed to generate audio.")
            return
        }

        try {
            val locale = if (language == AppLanguage.HINDI) java.util.Locale("hi", "IN") else java.util.Locale.US
            ttsEngine?.language = locale
            ttsEngine?.setSpeechRate(speed)

            val textToSpeak = if (language == AppLanguage.HINDI) {
                "${verse.shlokaSanskrit}। अर्थ: ${verse.translationHindi}। ${verse.meaningHindi}"
            } else {
                "${verse.transliteration}. Meaning: ${verse.translationEnglish}. ${verse.meaningEnglish}"
            }

            ttsEngine?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    coroutineScope.launch(Dispatchers.Main) {
                        onPlaybackCompleted()
                    }
                }
                override fun onError(utteranceId: String?) {
                    coroutineScope.launch(Dispatchers.Main) {
                        onAudioError(originalErrorMsg ?: "Text-to-speech error.")
                    }
                }
            })

            val params = android.os.Bundle()
            ttsEngine?.speak(textToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "GITA_TTS_${verse.verseKey}")
        } catch (e: Exception) {
            Log.e("GitaAudioEngine", "System TTS fallback error", e)
            onAudioError(originalErrorMsg ?: e.localizedMessage ?: "Audio error.")
        }
    }

    fun testVoiceSample(
        language: AppLanguage,
        style: VoiceStyle,
        speed: Float,
        customApiKey: String? = null
    ) {
        stopAudio()
        onLoadingStateChanged(true)

        audioJob?.cancel()
        audioJob = coroutineScope.launch {
            val result = GeminiClient.getOrFetchSampleTtsAudio(
                context = context,
                language = language,
                voiceStyle = style,
                customApiKey = customApiKey
            )

            onLoadingStateChanged(false)

            result.fold(
                onSuccess = { audioFile ->
                    playAudioFile(audioFile, speed)
                },
                onFailure = { error ->
                    Log.e("GitaAudioEngine", "Sample TTS error", error)
                    onAudioError(error.localizedMessage ?: "Failed to generate voice sample.")
                }
            )
        }
    }

    private fun playAudioFile(file: java.io.File, speed: Float) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        playbackParams = PlaybackParams().setSpeed(speed)
                    } catch (e: Exception) {
                        Log.w("GitaAudioEngine", "Could not set playback speed", e)
                    }
                }
                setOnCompletionListener {
                    stopAmbience()
                    onPlaybackCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("GitaAudioEngine", "MediaPlayer error: $what, $extra")
                    try { file.delete() } catch (_: Exception) {}
                    stopAmbience()
                    onAudioError("Audio playback error ($what)")
                    true
                }
                start()
            }
            startProgressReporting()
        } catch (e: Exception) {
            Log.e("GitaAudioEngine", "Error playing audio file", e)
            try { file.delete() } catch (_: Exception) {}
            onAudioError("Failed to play audio file: ${e.localizedMessage}")
        }
    }

    /** Seeks the current narration/chant to an absolute position in milliseconds. */
    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.let { player ->
                val safePos = positionMs.coerceIn(0, player.duration.coerceAtLeast(0))
                player.seekTo(safePos)
                onProgress(safePos, player.duration.coerceAtLeast(0))
            }
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "Seek failed", e)
        }
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        mediaPlayer?.let { player ->
            if (player.isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    Log.w("GitaAudioEngine", "Could not set playback speed", e)
                }
            }
        }
    }

    fun setVoiceStyle(style: VoiceStyle) {
        currentVoiceStyle = style
        val verse = currentVerse
        if (verse != null && isSpeaking()) {
            playVerse(verse, currentLanguage, currentSpeed, style)
        }
    }

    fun pauseOrStop() {
        stopAudio()
    }

    private fun stopAudio() {
        audioJob?.cancel()
        progressJob?.cancel()
        stopAmbience()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
            }
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "Error stopping player", e)
        }
        try {
            if (ttsEngine?.isSpeaking == true) {
                ttsEngine?.stop()
            }
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "Error stopping TTS", e)
        }
    }

    fun isSpeaking(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true || ttsEngine?.isSpeaking == true
        } catch (e: Exception) {
            false
        }
    }

    fun shutdown() {
        audioJob?.cancel()
        progressJob?.cancel()
        stopAmbience()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "Error shutting down player", e)
        }
        try {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
            ttsEngine = null
        } catch (e: Exception) {
            Log.w("GitaAudioEngine", "Error shutting down TTS engine", e)
        }
    }
}


