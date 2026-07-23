package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppLanguage
import com.example.data.GitaData
import com.example.ui.GitaViewModel
import com.example.ui.Screen
import com.example.ui.components.AudioPlayerBar
import com.example.ui.components.GitaTopBar
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.ChapterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.VerseDetailDialog
import com.example.ui.components.GitaAiChatDialog
import com.example.ui.components.GeminiApiKeyDialog
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.theme.GitaSaathiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitaSaathiApp()
        }
    }
}

@Composable
fun GitaSaathiApp(viewModel: GitaViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val currentVerseList by viewModel.currentVerseList.collectAsStateWithLifecycle()
    val selectedVerse by viewModel.selectedVerse.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val activePlayingVerse by viewModel.activePlayingVerse.collectAsStateWithLifecycle()
    val voiceStyle by viewModel.voiceStyle.collectAsStateWithLifecycle()
    val audioSpeed by viewModel.audioSpeed.collectAsStateWithLifecycle()
    val narrationLanguage by viewModel.narrationLanguage.collectAsStateWithLifecycle()
    val autoContinueAudio by viewModel.autoContinueAudio.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val recentPosition by viewModel.recentPosition.collectAsStateWithLifecycle()
    val aiInsightText by viewModel.aiInsightText.collectAsStateWithLifecycle()
    val isInsightLoading by viewModel.isInsightLoading.collectAsStateWithLifecycle()

    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val aiChatMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val isAiChatLoading by viewModel.isAiChatLoading.collectAsStateWithLifecycle()
    val showAiDialog by viewModel.showAiDialog.collectAsStateWithLifecycle()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsStateWithLifecycle()
    val showVoiceSettingsDialog by viewModel.showVoiceSettingsDialog.collectAsStateWithLifecycle()
    val isAudioLoading by viewModel.isAudioLoading.collectAsStateWithLifecycle()
    val audioErrorMessage by viewModel.audioErrorMessage.collectAsStateWithLifecycle()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val playbackDurationMs by viewModel.playbackDurationMs.collectAsStateWithLifecycle()

    val shlokaOfTheDay = GitaData.getShlokaOfTheDay()
    val isShlokaOfDayBookmarked = viewModel.isVerseBookmarked(shlokaOfTheDay.verseKey)

    // Handle back button behavior for sub-screens
    BackHandler(enabled = currentScreen != Screen.HOME || selectedVerse != null || showAiDialog || showApiKeyDialog || showVoiceSettingsDialog) {
        if (showVoiceSettingsDialog) {
            viewModel.closeVoiceSettingsDialog()
        } else if (showApiKeyDialog) {
            viewModel.closeApiKeyDialog()
        } else if (showAiDialog) {
            viewModel.closeAiDialog()
        } else if (selectedVerse != null) {
            viewModel.dismissVerseDetail()
        } else {
            viewModel.navigateTo(Screen.HOME)
        }
    }

    GitaSaathiTheme(darkTheme = isDarkMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                GitaTopBar(
                    currentScreen = currentScreen,
                    appLanguage = appLanguage,
                    isDarkMode = isDarkMode,
                    onBackClick = { viewModel.navigateTo(Screen.HOME) },
                    onLanguageToggle = { viewModel.toggleAppLanguage() },
                    onDarkModeToggle = { viewModel.toggleDarkMode() },
                    onBookmarksClick = { viewModel.navigateTo(Screen.BOOKMARKS) },
                    onAiChatClick = { viewModel.openAiDialog() },
                    onApiKeyClick = { viewModel.openApiKeyDialog() },
                    onVoiceSettingsClick = { viewModel.openVoiceSettingsDialog() }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.HOME -> {
                        HomeScreen(
                            appLanguage = appLanguage,
                            searchQuery = searchQuery,
                            recentPosition = recentPosition,
                            chapters = GitaData.CHAPTERS,
                            shlokaOfTheDay = shlokaOfTheDay,
                            isBookmarked = isShlokaOfDayBookmarked,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onChapterClick = { viewModel.selectChapter(it) },
                            onVerseClick = { viewModel.selectVerse(it) },
                            onPlayVerseAudio = { viewModel.playVerseAudio(it) },
                            onToggleBookmark = { viewModel.toggleBookmark(it) },
                            onBookmarkPageClick = { viewModel.navigateTo(Screen.BOOKMARKS) },
                            onOpenAiChat = { viewModel.openAiDialog() }
                        )
                    }

                    Screen.CHAPTER_DETAIL -> {
                        val chapter = selectedChapter
                        if (chapter != null) {
                            ChapterScreen(
                                chapter = chapter,
                                verses = currentVerseList,
                                appLanguage = appLanguage,
                                activePlayingVerse = activePlayingVerse,
                                isPlayingAudio = isPlayingAudio,
                                isBookmarked = { viewModel.isVerseBookmarked(it) },
                                aiInsightText = aiInsightText,
                                isInsightLoading = isInsightLoading,
                                selectedVerseForInsight = selectedVerse,
                                onVerseClick = { viewModel.selectVerse(it) },
                                onPlayVerseAudio = { viewModel.playVerseAudio(it) },
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onFetchAiInsight = {
                                    viewModel.selectVerse(it)
                                    viewModel.fetchAiInsight(it)
                                }
                            )
                        } else {
                            viewModel.navigateTo(Screen.HOME)
                        }
                    }

                    Screen.BOOKMARKS -> {
                        BookmarksScreen(
                            bookmarks = bookmarks,
                            appLanguage = appLanguage,
                            onVerseClick = { viewModel.selectVerse(it) },
                            onPlayAudio = { viewModel.playVerseAudio(it) },
                            onRemoveBookmark = { viewModel.toggleBookmark(it) }
                        )
                    }

                    Screen.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            appLanguage = appLanguage,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onVerseClick = { viewModel.selectVerse(it) },
                            onPlayAudio = { viewModel.playVerseAudio(it) }
                        )
                    }

                    else -> {
                        viewModel.navigateTo(Screen.HOME)
                    }
                }

                // Floating Audio Player Bar at bottom
                if (activePlayingVerse != null) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        AudioPlayerBar(
                            activeVerse = activePlayingVerse,
                            isPlaying = isPlayingAudio,
                            narrationLanguage = narrationLanguage,
                            speed = audioSpeed,
                            autoContinue = autoContinueAudio,
                            appLanguage = appLanguage,
                            isLoading = isAudioLoading,
                            positionMs = playbackPositionMs,
                            durationMs = playbackDurationMs,
                            onSeek = { viewModel.seekAudio(it) },
                            onPlayPauseToggle = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.playNextVerse() },
                            onPrevClick = { viewModel.playPreviousVerse() },
                            onSpeedChange = { viewModel.setAudioSpeed(it) },
                            onNarrationLangChange = { viewModel.setNarrationLanguage(it) },
                            onAutoContinueToggle = { viewModel.toggleAutoContinue() },
                            onVerseCardClick = {
                                val verse = activePlayingVerse
                                if (verse != null) viewModel.selectVerse(verse)
                            },
                            onVoiceSettingsClick = { viewModel.openVoiceSettingsDialog() }
                        )
                    }
                }

                // Voice Settings Dialog
                if (showVoiceSettingsDialog) {
                    VoiceSettingsDialog(
                        appLanguage = appLanguage,
                        currentVoiceStyle = voiceStyle,
                        currentSpeed = audioSpeed,
                        currentNarrationLang = narrationLanguage,
                        onVoiceStyleSelected = { viewModel.setVoiceStyle(it) },
                        onSpeedSelected = { viewModel.setAudioSpeed(it) },
                        onNarrationLangSelected = { viewModel.setNarrationLanguage(it) },
                        onTestSample = { lang, style, speed -> viewModel.testVoiceSample(lang, style, speed) },
                        onDismiss = { viewModel.closeVoiceSettingsDialog() }
                    )
                }

                // Verse Detail Dialog
                if (selectedVerse != null) {
                    val verse = selectedVerse!!
                    VerseDetailDialog(
                        verse = verse,
                        appLanguage = appLanguage,
                        isPlayingAudio = isPlayingAudio && activePlayingVerse?.verseKey == verse.verseKey,
                        isBookmarked = viewModel.isVerseBookmarked(verse.verseKey),
                        aiInsightText = aiInsightText,
                        isInsightLoading = isInsightLoading,
                        onDismiss = { viewModel.dismissVerseDetail() },
                        onPlayAudio = { viewModel.playVerseAudio(it) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        onFetchAiInsight = { viewModel.fetchAiInsight(it) }
                    )
                }

                // Gita AI Interactive Chat Dialog
                if (showAiDialog) {
                    GitaAiChatDialog(
                        appLanguage = appLanguage,
                        messages = aiChatMessages,
                        isLoading = isAiChatLoading,
                        customApiKey = customApiKey,
                        onSendMessage = { viewModel.sendAiChatMessage(it) },
                        onOpenApiKeySetup = { viewModel.openApiKeyDialog() },
                        onDismiss = { viewModel.closeAiDialog() }
                    )
                }

                // Gemini API Key Settings Dialog
                if (showApiKeyDialog) {
                    GeminiApiKeyDialog(
                        appLanguage = appLanguage,
                        currentApiKey = customApiKey,
                        onSaveApiKey = { viewModel.setCustomApiKey(it) },
                        onDismiss = { viewModel.closeApiKeyDialog() }
                    )
                }

                // Audio Error Message Dialog
                if (audioErrorMessage != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.clearAudioError() },
                        title = {
                            androidx.compose.material3.Text(
                                text = if (appLanguage == AppLanguage.HINDI) "स्वर उत्पन्न त्रुटि" else "Gemini TTS Error"
                            )
                        },
                        text = {
                            androidx.compose.material3.Text(text = audioErrorMessage ?: "")
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { viewModel.clearAudioError() }) {
                                androidx.compose.material3.Text(text = if (appLanguage == AppLanguage.HINDI) "ठीक है" else "OK")
                            }
                        }
                    )
                }
            }
        }
    }
}
