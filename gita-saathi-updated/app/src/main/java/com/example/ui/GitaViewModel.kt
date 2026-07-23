package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GitaAudioEngine
import com.example.data.AppLanguage
import com.example.data.BookmarkEntity
import com.example.data.Chapter
import com.example.data.GitaData
import com.example.data.GitaDatabase
import com.example.data.RecentPositionEntity
import com.example.data.Verse
import com.example.data.VoiceStyle
import com.example.network.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    CHAPTER_DETAIL,
    VERSE_DETAIL,
    BOOKMARKS,
    SEARCH
}

class GitaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GitaDatabase.getDatabase(application)
    private val bookmarkDao = db.bookmarkDao()

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPosition: StateFlow<RecentPositionEntity?> = bookmarkDao.getRecentPosition()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.HINDI)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedChapter = MutableStateFlow<Chapter?>(null)
    val selectedChapter: StateFlow<Chapter?> = _selectedChapter.asStateFlow()

    private val _currentVerseList = MutableStateFlow<List<Verse>>(emptyList())
    val currentVerseList: StateFlow<List<Verse>> = _currentVerseList.asStateFlow()

    private val _selectedVerse = MutableStateFlow<Verse?>(null)
    val selectedVerse: StateFlow<Verse?> = _selectedVerse.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Verse>>(emptyList())
    val searchResults: StateFlow<List<Verse>> = _searchResults.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _activePlayingVerse = MutableStateFlow<Verse?>(null)
    val activePlayingVerse: StateFlow<Verse?> = _activePlayingVerse.asStateFlow()

    // SharedPreferences for persistent settings
    private val prefs = application.getSharedPreferences("gita_saathi_prefs", android.content.Context.MODE_PRIVATE)

    private val _voiceStyle = MutableStateFlow(VoiceStyle.fromId(prefs.getString("gita_voice_style", VoiceStyle.DEVOTIONAL.id)))
    val voiceStyle: StateFlow<VoiceStyle> = _voiceStyle.asStateFlow()

    private val _audioSpeed = MutableStateFlow(prefs.getFloat("gita_audio_speed", 1.0f))
    val audioSpeed: StateFlow<Float> = _audioSpeed.asStateFlow()

    private val _narrationLanguage = MutableStateFlow(
        if (prefs.getString("gita_narration_lang", "HINDI") == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.HINDI
    )
    val narrationLanguage: StateFlow<AppLanguage> = _narrationLanguage.asStateFlow()

    private val _autoContinueAudio = MutableStateFlow(true)
    val autoContinueAudio: StateFlow<Boolean> = _autoContinueAudio.asStateFlow()

    private val _aiInsightText = MutableStateFlow<String?>(null)
    val aiInsightText: StateFlow<String?> = _aiInsightText.asStateFlow()

    private val _isInsightLoading = MutableStateFlow(false)
    val isInsightLoading: StateFlow<Boolean> = _isInsightLoading.asStateFlow()

    // Gemini Custom API Key State stored in SharedPreferences
    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // Interactive Ask Gita AI Chat State
    data class AiChatMessage(val sender: String, val text: String, val timestamp: Long = System.currentTimeMillis())
    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAiChatLoading = MutableStateFlow(false)
    val isAiChatLoading: StateFlow<Boolean> = _isAiChatLoading.asStateFlow()

    private val _showAiDialog = MutableStateFlow(false)
    val showAiDialog: StateFlow<Boolean> = _showAiDialog.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private val _showVoiceSettingsDialog = MutableStateFlow(false)
    val showVoiceSettingsDialog: StateFlow<Boolean> = _showVoiceSettingsDialog.asStateFlow()

    private val _isAudioLoading = MutableStateFlow(false)
    val isAudioLoading: StateFlow<Boolean> = _isAudioLoading.asStateFlow()

    private val _audioErrorMessage = MutableStateFlow<String?>(null)
    val audioErrorMessage: StateFlow<String?> = _audioErrorMessage.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0)
    val playbackPositionMs: StateFlow<Int> = _playbackPositionMs.asStateFlow()

    private val _playbackDurationMs = MutableStateFlow(0)
    val playbackDurationMs: StateFlow<Int> = _playbackDurationMs.asStateFlow()

    private var audioEngine: GitaAudioEngine? = null

    init {
        audioEngine = GitaAudioEngine(
            context = application,
            onPlaybackCompleted = { onAudioCompleted() },
            onLoadingStateChanged = { loading -> _isAudioLoading.value = loading },
            onAudioError = { errorMsg ->
                _isPlayingAudio.value = false
                _isAudioLoading.value = false
                _audioErrorMessage.value = errorMsg
            },
            onProgress = { positionMs, durationMs ->
                _playbackPositionMs.value = positionMs
                _playbackDurationMs.value = durationMs
            }
        )
    }

    fun seekAudio(positionMs: Int) {
        audioEngine?.seekTo(positionMs)
        _playbackPositionMs.value = positionMs
    }

    fun clearAudioError() {
        _audioErrorMessage.value = null
    }

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        _customApiKey.value = trimmed
        prefs.edit().putString("custom_gemini_key", trimmed).apply()
    }

    fun openAiDialog() {
        _showAiDialog.value = true
    }

    fun closeAiDialog() {
        _showAiDialog.value = false
    }

    fun openApiKeyDialog() {
        _showApiKeyDialog.value = true
    }

    fun closeApiKeyDialog() {
        _showApiKeyDialog.value = false
    }

    fun sendAiChatMessage(userQuery: String) {
        if (userQuery.isBlank()) return
        val userMsg = AiChatMessage("user", userQuery)
        _aiChatMessages.value = _aiChatMessages.value + userMsg
        _isAiChatLoading.value = true

        viewModelScope.launch {
            val keyToUse = _customApiKey.value.ifBlank { null }
            val result = GeminiClient.askGitaAi(userQuery, _appLanguage.value, keyToUse)
            _isAiChatLoading.value = false

            val replyText = result.getOrElse { err ->
                if (_appLanguage.value == AppLanguage.HINDI) {
                    "क्षमा करें, AI उत्तर प्राप्त करने में त्रुटि हुई: ${err.localizedMessage ?: "कृपया पुनः प्रयास करें"}"
                } else {
                    "Sorry, unable to get AI response: ${err.localizedMessage ?: "Please try again"}"
                }
            }
            _aiChatMessages.value = _aiChatMessages.value + AiChatMessage("gita_ai", replyText)
        }
    }

    fun toggleAppLanguage() {
        val next = if (_appLanguage.value == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
        _appLanguage.value = next
        _narrationLanguage.value = next
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        _narrationLanguage.value = language
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectChapter(chapterId: Int) {
        val chapter = GitaData.getChapter(chapterId)
        _selectedChapter.value = chapter
        if (chapter != null) {
            _currentVerseList.value = GitaData.getVersesForChapter(chapterId)
            _currentScreen.value = Screen.CHAPTER_DETAIL
        }
    }

    fun selectVerse(verse: Verse?) {
        _selectedVerse.value = verse
        _aiInsightText.value = null
        if (verse != null) {
            saveRecentPosition(verse.chapterId, verse.verseId)
        }
    }

    fun dismissVerseDetail() {
        _selectedVerse.value = null
        _aiInsightText.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        } else {
            _searchResults.value = GitaData.searchVerses(query)
            if (_currentScreen.value != Screen.SEARCH) {
                _currentScreen.value = Screen.SEARCH
            }
        }
    }

    fun toggleBookmark(verse: Verse) {
        viewModelScope.launch {
            val key = verse.verseKey
            val isBookmarkedNow = bookmarks.value.any { it.verseKey == key }
            if (isBookmarkedNow) {
                bookmarkDao.deleteBookmark(key)
            } else {
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        verseKey = key,
                        chapterId = verse.chapterId,
                        verseId = verse.verseId
                    )
                )
            }
        }
    }

    fun isVerseBookmarked(verseKey: String): Boolean {
        return bookmarks.value.any { it.verseKey == verseKey }
    }

    fun playVerseAudio(verse: Verse) {
        _activePlayingVerse.value = verse
        _isPlayingAudio.value = true
        _audioErrorMessage.value = null
        _playbackPositionMs.value = 0
        _playbackDurationMs.value = 0
        audioEngine?.playVerse(
            verse = verse,
            language = _narrationLanguage.value,
            speed = _audioSpeed.value,
            voiceStyle = _voiceStyle.value,
            customApiKey = _customApiKey.value.ifBlank { null }
        )
        saveRecentPosition(verse.chapterId, verse.verseId)
    }

    fun togglePlayPause() {
        if (_isPlayingAudio.value) {
            audioEngine?.pauseOrStop()
            _isPlayingAudio.value = false
        } else {
            val verse = _activePlayingVerse.value ?: _selectedVerse.value ?: GitaData.getShlokaOfTheDay()
            playVerseAudio(verse)
        }
    }

    fun setAudioSpeed(speed: Float) {
        _audioSpeed.value = speed
        audioEngine?.setSpeed(speed)
        prefs.edit().putFloat("gita_audio_speed", speed).apply()
    }

    fun setNarrationLanguage(language: AppLanguage) {
        _narrationLanguage.value = language
        prefs.edit().putString("gita_narration_lang", language.name).apply()
        val verse = _activePlayingVerse.value
        if (verse != null && _isPlayingAudio.value) {
            playVerseAudio(verse)
        }
    }

    fun setVoiceStyle(style: VoiceStyle) {
        _voiceStyle.value = style
        prefs.edit().putString("gita_voice_style", style.id).apply()
        audioEngine?.setVoiceStyle(style)
        val verse = _activePlayingVerse.value
        if (verse != null && _isPlayingAudio.value) {
            playVerseAudio(verse)
        }
    }

    fun openVoiceSettingsDialog() {
        _showVoiceSettingsDialog.value = true
    }

    fun closeVoiceSettingsDialog() {
        _showVoiceSettingsDialog.value = false
    }

    fun testVoiceSample(language: AppLanguage = _narrationLanguage.value, style: VoiceStyle = _voiceStyle.value, speed: Float = _audioSpeed.value) {
        _audioErrorMessage.value = null
        audioEngine?.testVoiceSample(language, style, speed, _customApiKey.value.ifBlank { null })
    }

    fun toggleAutoContinue() {
        _autoContinueAudio.value = !_autoContinueAudio.value
    }

    fun playNextVerse() {
        val current = _activePlayingVerse.value ?: return
        val currentChapterVerses = GitaData.getVersesForChapter(current.chapterId)
        val currentIndex = currentChapterVerses.indexOfFirst { it.verseId == current.verseId }

        if (currentIndex != -1 && currentIndex < currentChapterVerses.size - 1) {
            val nextVerse = currentChapterVerses[currentIndex + 1]
            playVerseAudio(nextVerse)
        } else if (current.chapterId < 18) {
            val nextChapterVerses = GitaData.getVersesForChapter(current.chapterId + 1)
            if (nextChapterVerses.isNotEmpty()) {
                playVerseAudio(nextChapterVerses.first())
            }
        }
    }

    fun playPreviousVerse() {
        val current = _activePlayingVerse.value ?: return
        val currentChapterVerses = GitaData.getVersesForChapter(current.chapterId)
        val currentIndex = currentChapterVerses.indexOfFirst { it.verseId == current.verseId }

        if (currentIndex > 0) {
            val prevVerse = currentChapterVerses[currentIndex - 1]
            playVerseAudio(prevVerse)
        } else if (current.chapterId > 1) {
            val prevChapterVerses = GitaData.getVersesForChapter(current.chapterId - 1)
            if (prevChapterVerses.isNotEmpty()) {
                playVerseAudio(prevChapterVerses.last())
            }
        }
    }

    private fun onAudioCompleted() {
        viewModelScope.launch {
            if (_autoContinueAudio.value && _isPlayingAudio.value) {
                playNextVerse()
            } else {
                _isPlayingAudio.value = false
            }
        }
    }

    fun fetchAiInsight(verse: Verse) {
        viewModelScope.launch {
            _isInsightLoading.value = true
            _aiInsightText.value = null
            val keyToUse = _customApiKey.value.ifBlank { null }
            val result = GeminiClient.getVerseInsight(verse, _appLanguage.value, keyToUse)
            _isInsightLoading.value = false
            _aiInsightText.value = result.getOrElse { err ->
                if (_appLanguage.value == AppLanguage.HINDI) {
                    "इस श्लोक से हमें यह दिव्य संदेश मिलता है कि कर्तव्य मार्ग पर निष्काम भाव से निरंतर आगे बढ़ते रहें।"
                } else {
                    "This divine verse guides us to perform our duties with devotion and focus without attachment to results."
                }
            }
        }
    }

    private fun saveRecentPosition(chapterId: Int, verseId: Int) {
        viewModelScope.launch {
            bookmarkDao.saveRecentPosition(
                RecentPositionEntity(
                    id = 1,
                    chapterId = chapterId,
                    verseId = verseId
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine?.shutdown()
    }
}
