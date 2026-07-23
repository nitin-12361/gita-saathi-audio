package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.BookmarkEntity
import com.example.data.GitaData
import com.example.data.Verse

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    appLanguage: AppLanguage,
    onVerseClick: (Verse) -> Unit,
    onPlayAudio: (Verse) -> Unit,
    onRemoveBookmark: (Verse) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("empty_bookmarks_view"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "No bookmarks",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(20.dp).height(48.dp).width(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (appLanguage == AppLanguage.HINDI) "कोई पसंदीदा श्लोक सेव नहीं किया गया" else "No Favorite Verses Saved Yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (appLanguage == AppLanguage.HINDI) "श्लोक पढ़ते समय पसंदीदा बटन पर टैप करके उन्हें यहाँ सहेजें।" else "Tap the bookmark icon on any verse while reading to save it here for quick listening.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("bookmarks_lazy_column"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (appLanguage == AppLanguage.HINDI) "पसंदीदा श्लोक संग्रह (${bookmarks.size})" else "Saved Favorite Verses (${bookmarks.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(bookmarks) { b ->
            val verse = GitaData.getVerse(b.chapterId, b.verseId)
            if (verse != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVerseClick(verse) }
                        .testTag("bookmark_item_${verse.chapterId}_${verse.verseId}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                    text = "${if (appLanguage == AppLanguage.HINDI) "अध्याय" else "Chapter"} ${verse.chapterId}, ${if (appLanguage == AppLanguage.HINDI) "श्लोक" else "Verse"} ${verse.verseId}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { onRemoveBookmark(verse) },
                                    modifier = Modifier.testTag("remove_bookmark_${verse.chapterId}_${verse.verseId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkRemove,
                                        contentDescription = "Remove Bookmark",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }

                                IconButton(
                                    onClick = { onPlayAudio(verse) },
                                    modifier = Modifier.testTag("play_bookmark_audio_${verse.chapterId}_${verse.verseId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Audio",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = verse.shlokaSanskrit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (appLanguage == AppLanguage.HINDI) verse.translationHindi else verse.translationEnglish,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
