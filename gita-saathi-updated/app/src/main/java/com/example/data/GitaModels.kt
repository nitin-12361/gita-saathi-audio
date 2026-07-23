package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage(val code: String, val displayName: String, val ttsLocale: String) {
    HINDI("hi", "हिंदी", "hi_IN"),
    ENGLISH("en", "English", "en_US")
}

data class Chapter(
    val id: Int,
    val nameHindi: String,
    val nameEnglish: String,
    val titleHindi: String,
    val titleEnglish: String,
    val summaryHindi: String,
    val summaryEnglish: String,
    val versesCount: Int
)

data class Verse(
    val chapterId: Int,
    val verseId: Int,
    val shlokaSanskrit: String,
    val transliteration: String,
    val translationHindi: String,
    val translationEnglish: String,
    val meaningHindi: String,
    val meaningEnglish: String
) {
    val verseKey: String
        get() = "c${chapterId}_v${verseId}"

    val verseReference: String
        get() = "$chapterId.$verseId"
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val verseKey: String,
    val chapterId: Int,
    val verseId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_position")
data class RecentPositionEntity(
    @PrimaryKey val id: Int = 1,
    val chapterId: Int,
    val verseId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class AudioState(
    val isPlaying: Boolean = false,
    val currentChapterId: Int = 0,
    val currentVerseId: Int = 0,
    val narrationLanguage: AppLanguage = AppLanguage.HINDI,
    val speed: Float = 1.0f,
    val autoContinue: Boolean = true
)
