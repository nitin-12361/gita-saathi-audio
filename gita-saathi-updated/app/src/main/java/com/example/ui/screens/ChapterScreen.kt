package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.Chapter
import com.example.data.Verse
import com.example.ui.components.VerseAnimationVideoPlayer
import androidx.compose.material.icons.filled.Videocam

@Composable
fun ChapterScreen(
    chapter: Chapter,
    verses: List<Verse>,
    appLanguage: AppLanguage,
    activePlayingVerse: Verse?,
    isPlayingAudio: Boolean,
    isBookmarked: (String) -> Boolean,
    aiInsightText: String?,
    isInsightLoading: Boolean,
    selectedVerseForInsight: Verse?,
    onVerseClick: (Verse) -> Unit,
    onPlayVerseAudio: (Verse) -> Unit,
    onToggleBookmark: (Verse) -> Unit,
    onFetchAiInsight: (Verse) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("chapter_screen_lazy_column"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chapter Header & Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chapter_header_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${if (appLanguage == AppLanguage.HINDI) "अध्याय" else "Chapter"} ${chapter.id}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (appLanguage == AppLanguage.HINDI) chapter.nameHindi else chapter.nameEnglish,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (appLanguage == AppLanguage.HINDI) chapter.summaryHindi else chapter.summaryEnglish,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Verses List
        items(verses) { verse ->
            val isActivePlaying = activePlayingVerse?.verseKey == verse.verseKey && isPlayingAudio
            val bookmarked = isBookmarked(verse.verseKey)
            val isInsightTarget = selectedVerseForInsight?.verseKey == verse.verseKey

            VerseItemCard(
                verse = verse,
                appLanguage = appLanguage,
                isActivePlaying = isActivePlaying,
                isBookmarked = bookmarked,
                aiInsightText = if (isInsightTarget) aiInsightText else null,
                isInsightLoading = isInsightTarget && isInsightLoading,
                onClick = { onVerseClick(verse) },
                onPlayAudio = { onPlayVerseAudio(verse) },
                onToggleBookmark = { onToggleBookmark(verse) },
                onFetchAiInsight = { onFetchAiInsight(verse) }
            )
        }
    }
}

@Composable
fun VerseItemCard(
    verse: Verse,
    appLanguage: AppLanguage,
    isActivePlaying: Boolean,
    isBookmarked: Boolean,
    aiInsightText: String?,
    isInsightLoading: Boolean,
    onClick: () -> Unit,
    onPlayAudio: () -> Unit,
    onToggleBookmark: () -> Unit,
    onFetchAiInsight: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAnimationVideo by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("verse_card_${verse.chapterId}_${verse.verseId}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActivePlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Verse Header Row (Reference, Play button, Bookmark button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${if (appLanguage == AppLanguage.HINDI) "श्लोक" else "Verse"} ${verse.chapterId}.${verse.verseId}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showAnimationVideo = !showAnimationVideo },
                        modifier = Modifier.testTag("toggle_video_${verse.chapterId}_${verse.verseId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Animation Video",
                            tint = if (showAnimationVideo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("bookmark_verse_${verse.chapterId}_${verse.verseId}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        onClick = onPlayAudio,
                        shape = CircleShape,
                        color = if (isActivePlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("play_verse_${verse.chapterId}_${verse.verseId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isActivePlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                contentDescription = "Play Audio",
                                tint = if (isActivePlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isActivePlaying) "Playing" else (if (appLanguage == AppLanguage.HINDI) "सुनें" else "Listen"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActivePlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Video Canvas (Toggleable per Verse)
            AnimatedVisibility(visible = showAnimationVideo) {
                VerseAnimationVideoPlayer(
                    verse = verse,
                    appLanguage = appLanguage,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Sanskrit Devanagari Text
            Text(
                text = verse.shlokaSanskrit,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // English Transliteration
            Text(
                text = verse.transliteration,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Translation based on app language preference
            Text(
                text = if (appLanguage == AppLanguage.HINDI) verse.translationHindi else verse.translationEnglish,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expand Word Meaning & AI Insights Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (appLanguage == AppLanguage.HINDI) "शब्दार्थ एवं अर्थ विस्तार" else "Word Meanings & Details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = if (appLanguage == AppLanguage.HINDI) verse.meaningHindi else verse.meaningEnglish,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gemini AI Explanation Button
                    Surface(
                        onClick = onFetchAiInsight,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_insight_button_${verse.chapterId}_${verse.verseId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Insight",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (appLanguage == AppLanguage.HINDI) "AI से गहराई से समझें (Gemini Insight)" else "Explain in detail with AI (Gemini)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isInsightLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == AppLanguage.HINDI) "AI चिंतन प्राप्त हो रहा है..." else "Generating AI Reflection...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (aiInsightText != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = aiInsightText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
