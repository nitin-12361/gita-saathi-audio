package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import android.widget.VideoView
import android.net.Uri
import com.example.data.AppLanguage
import com.example.data.Verse
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real, AI-generated video for a theme, if you've bundled one in
 * assets/videos/theme_<styleId>.mp4 (see tools/generate_theme_videos.py and
 * README_AUDIO_VIDEO_SETUP.md). There are 8 themes shared across all verses — see
 * getVerseVideoTheme() — so 8 real generated clips cover every verse in every chapter.
 * If the file isn't there yet, the procedural Canvas animation below is used instead,
 * so the app always works.
 */
private fun findThemeVideoAssetPath(context: android.content.Context, styleId: Int): String? {
    val candidates = listOf("videos/theme_$styleId.mp4", "videos/theme_$styleId.webm")
    return candidates.firstOrNull { path ->
        try {
            context.assets.open(path).close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
private fun RealThemeVideoView(
    assetPath: String,
    isPlaying: Boolean,
    isLooping: Boolean,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val videoView = remember {
        VideoView(context).apply {
            setVideoURI(Uri.parse("file:///android_asset/$assetPath"))
            setOnPreparedListener { mp ->
                mp.isLooping = isLooping
                start()
            }
        }
    }
    AndroidView(factory = { videoView }, modifier = modifier)
    LaunchedEffect(isPlaying) {
        if (isPlaying) videoView.start() else videoView.pause()
    }
    DisposableEffect(Unit) {
        onDispose { videoView.stopPlayback() }
    }
}

/**
 * Unique Animated Video Theme metadata for a verse
 */
data class VerseVideoTheme(
    val title: String,
    val titleHindi: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val styleId: Int
)

fun getVerseVideoTheme(chapterId: Int, verseId: Int): VerseVideoTheme {
    val seed = (chapterId * 31 + verseId) % 8
    return when (seed) {
        0 -> VerseVideoTheme(
            title = "Sacred Om Mandala Video",
            titleHindi = "पवित्र ॐ मण्डल एनिमेशन",
            primaryColor = Color(0xFFE67E22),
            secondaryColor = Color(0xFFFF9933),
            accentColor = Color(0xFFFFD54F),
            styleId = 0
        )
        1 -> VerseVideoTheme(
            title = "Cosmic Vishwaroopa Galaxy",
            titleHindi = "दिव्य विश्वरूप ब्रह्मांडीय प्रवाह",
            primaryColor = Color(0xFF8E24AA),
            secondaryColor = Color(0xFFAB47BC),
            accentColor = Color(0xFFFFB74D),
            styleId = 1
        )
        2 -> VerseVideoTheme(
            title = "Lotus Flame Awakening",
            titleHindi = "कमल एवं ज्योति जागृति",
            primaryColor = Color(0xFFD84315),
            secondaryColor = Color(0xFFF4511E),
            accentColor = Color(0xFFFFE082),
            styleId = 2
        )
        3 -> VerseVideoTheme(
            title = "Chakra Energy Rays",
            titleHindi = "चक्र ऊर्जा किरणें",
            primaryColor = Color(0xFF00897B),
            secondaryColor = Color(0xFF26A69A),
            accentColor = Color(0xFF80CBC4),
            styleId = 3
        )
        4 -> VerseVideoTheme(
            title = "Eternal Wheel of Karma",
            titleHindi = "अनादि कर्म चक्र प्रवाह",
            primaryColor = Color(0xFFC2185B),
            secondaryColor = Color(0xFFE91E63),
            accentColor = Color(0xFFFF80AB),
            styleId = 4
        )
        5 -> VerseVideoTheme(
            title = "Divine Golden Light Waves",
            titleHindi = "स्वर्ण ज्योति तरंगें",
            primaryColor = Color(0xFFF57F17),
            secondaryColor = Color(0xFFFBC02D),
            accentColor = Color(0xFFFFF59D),
            styleId = 5
        )
        6 -> VerseVideoTheme(
            title = "Cosmic Chariot Rays",
            titleHindi = "दिव्य रथ एवं सूर्य रश्मियाँ",
            primaryColor = Color(0xFF1565C0),
            secondaryColor = Color(0xFF1E88E5),
            accentColor = Color(0xFF90CAF9),
            styleId = 6
        )
        else -> VerseVideoTheme(
            title = "Sacred Banyan Tree Light",
            titleHindi = "अक्षय वटवृक्ष प्रकाश",
            primaryColor = Color(0xFF2E7D32),
            secondaryColor = Color(0xFF43A047),
            accentColor = Color(0xFFA5D6A7),
            styleId = 7
        )
    }
}

@Composable
fun VerseAnimationVideoPlayer(
    verse: Verse,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier,
    isAutoPlaying: Boolean = true,
    showControls: Boolean = true
) {
    val theme = remember(verse.chapterId, verse.verseId) {
        getVerseVideoTheme(verse.chapterId, verse.verseId)
    }

    var isPlaying by remember { mutableStateOf(isAutoPlaying) }
    var isLooping by remember { mutableStateOf(true) }
    var playSpeed by remember { mutableFloatStateOf(1.0f) }
    var showFullscreen by remember { mutableStateOf(false) }

    // Video progress simulation (0.0 to 1.0 in 10-second loop)
    var videoProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, playSpeed) {
        if (!isPlaying) return@LaunchedEffect
        val stepMs = 50L
        val totalMs = 10000L / playSpeed
        val delta = stepMs.toFloat() / totalMs

        while (isPlaying) {
            delay(stepMs)
            videoProgress = (videoProgress + delta) % 1.0f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "video_canvas_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (12000 / playSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2500 / playSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("verse_animation_video_${verse.chapterId}_${verse.verseId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF120D08)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            // Video Canvas Header Overlay Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1710))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Animation Video",
                        tint = theme.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (appLanguage == AppLanguage.HINDI) theme.titleHindi else theme.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = theme.primaryColor.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "Verse ${verse.chapterId}.${verse.verseId} Video",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Animated Video Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                theme.primaryColor.copy(alpha = 0.4f),
                                Color(0xFF120D08)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val realVideoPath = remember(theme.styleId) {
                    findThemeVideoAssetPath(context, theme.styleId)
                }

                if (realVideoPath != null) {
                    RealThemeVideoView(
                        assetPath = realVideoPath,
                        isPlaying = isPlaying,
                        isLooping = isLooping,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                        drawVerseAnimationScene(
                            theme = theme,
                            rotationAngle = if (isPlaying) rotationAngle else 0f,
                            pulseScale = if (isPlaying) pulseScale else 1f,
                            progress = videoProgress,
                            center = center,
                            size = size
                        )
                    }
                }

                // Sanskrit Overlay Text in Video
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = verse.shlokaSanskrit.lines().firstOrNull() ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                // Play / Pause Tap Overlay Button (if paused)
                if (!isPlaying) {
                    Surface(
                        onClick = { isPlaying = true },
                        shape = CircleShape,
                        color = theme.primaryColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Fullscreen Icon Button at top-right
                IconButton(
                    onClick = { showFullscreen = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Full Screen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Timeline Scrub Progress Bar
            LinearProgressIndicator(
                progress = { videoProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = theme.accentColor,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            // Video Controls Row
            if (showControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF18120C))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = theme.accentColor
                            )
                        }

                        IconButton(
                            onClick = {
                                videoProgress = 0f
                                isPlaying = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Restart Video",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Speed Switcher
                        Surface(
                            onClick = {
                                playSpeed = when (playSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    else -> 1.0f
                                }
                            },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "${playSpeed}x",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val secs = (videoProgress * 10).toInt()
                        Text(
                            text = "0:${if (secs < 10) "0$secs" else "$secs"} / 0:10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { isLooping = !isLooping },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "Loop Video",
                                tint = if (isLooping) theme.accentColor else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-Screen Video Modal
    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Large Animation Video Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawVerseAnimationScene(
                        theme = theme,
                        rotationAngle = if (isPlaying) rotationAngle else 0f,
                        pulseScale = if (isPlaying) pulseScale else 1f,
                        progress = videoProgress,
                        center = center,
                        size = size
                    )
                }

                // Shloka Sanskrit Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Verse ${verse.chapterId}.${verse.verseId}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = verse.shlokaSanskrit,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (appLanguage == AppLanguage.HINDI) verse.translationHindi else verse.translationEnglish,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Top Close Button
                IconButton(
                    onClick = { showFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Custom DrawScope rendering procedural animated scenes
 */
private fun DrawScope.drawVerseAnimationScene(
    theme: VerseVideoTheme,
    rotationAngle: Float,
    pulseScale: Float,
    progress: Float,
    center: Offset,
    size: Size
) {
    val minDim = minOf(size.width, size.height)
    val radius = (minDim / 3f) * pulseScale

    when (theme.styleId) {
        0 -> { // Sacred Om Mandala
            rotate(rotationAngle, center) {
                for (i in 0 until 12) {
                    val angle = (i * 30f) * (PI.toFloat() / 180f)
                    val petalCenter = Offset(
                        center.x + (radius * 0.7f) * cos(angle),
                        center.y + (radius * 0.7f) * sin(angle)
                    )
                    drawCircle(
                        color = theme.secondaryColor.copy(alpha = 0.35f),
                        radius = radius * 0.4f,
                        center = petalCenter,
                        style = Stroke(width = 3f)
                    )
                }
            }
            drawCircle(
                color = theme.accentColor.copy(alpha = 0.8f),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = theme.primaryColor.copy(alpha = 0.2f),
                radius = radius * 0.9f,
                center = center
            )
        }
        1 -> { // Cosmic Galaxy Starfield
            rotate(-rotationAngle * 0.5f, center) {
                for (arm in 0 until 4) {
                    val baseAngle = arm * 90f
                    for (step in 1..20) {
                        val dist = (step * (minDim / 45f))
                        val angleRad = (baseAngle + step * 12f) * (PI.toFloat() / 180f)
                        val starPos = Offset(
                            center.x + dist * cos(angleRad),
                            center.y + dist * sin(angleRad)
                        )
                        drawCircle(
                            color = if (step % 2 == 0) theme.accentColor else theme.secondaryColor,
                            radius = (3f + (step % 4)),
                            center = starPos
                        )
                    }
                }
            }
        }
        2 -> { // Lotus Flame Awakening
            val petalCount = 8
            val floatOffset = sin(progress * 2 * PI.toFloat()) * 15f
            val adjustedCenter = Offset(center.x, center.y + floatOffset)

            rotate(rotationAngle * 0.3f, adjustedCenter) {
                for (i in 0 until petalCount) {
                    val angle = (i * (360f / petalCount)) * (PI.toFloat() / 180f)
                    val p1 = Offset(
                        adjustedCenter.x + radius * cos(angle),
                        adjustedCenter.y + radius * sin(angle)
                    )
                    drawCircle(
                        color = theme.primaryColor.copy(alpha = 0.4f),
                        radius = radius * 0.35f,
                        center = p1,
                        style = Stroke(width = 3f)
                    )
                }
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.accentColor, Color.Transparent),
                    center = adjustedCenter,
                    radius = radius * 0.6f
                ),
                center = adjustedCenter,
                radius = radius * 0.6f
            )
        }
        3 -> { // Chakra Energy Rays
            for (ray in 0 until 16) {
                val rayAngle = (ray * 22.5f + rotationAngle) * (PI.toFloat() / 180f)
                val start = Offset(
                    center.x + (radius * 0.3f) * cos(rayAngle),
                    center.y + (radius * 0.3f) * sin(rayAngle)
                )
                val end = Offset(
                    center.x + (radius * 1.2f) * cos(rayAngle),
                    center.y + (radius * 1.2f) * sin(rayAngle)
                )
                drawLine(
                    color = if (ray % 2 == 0) theme.accentColor else theme.secondaryColor,
                    start = start,
                    end = end,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
        4 -> { // Eternal Wheel of Karma
            rotate(rotationAngle * 0.8f, center) {
                drawCircle(
                    color = theme.primaryColor,
                    radius = radius * 0.8f,
                    center = center,
                    style = Stroke(width = 6f)
                )
                for (spoke in 0 until 12) {
                    val sAngle = (spoke * 30f) * (PI.toFloat() / 180f)
                    val outer = Offset(
                        center.x + (radius * 0.8f) * cos(sAngle),
                        center.y + (radius * 0.8f) * sin(sAngle)
                    )
                    drawLine(
                        color = theme.accentColor,
                        start = center,
                        end = outer,
                        strokeWidth = 3f
                    )
                }
            }
        }
        5 -> { // Divine Golden Waves
            val wavePhase = progress * 2 * PI.toFloat()
            for (w in 1..4) {
                val waveRadius = (radius * 0.3f * w) + (sin(wavePhase + w) * 12f)
                drawCircle(
                    color = theme.accentColor.copy(alpha = 0.5f / w),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 4f)
                )
            }
        }
        6 -> { // Cosmic Chariot Rays
            rotate(-rotationAngle * 0.6f, center) {
                drawRect(
                    color = theme.secondaryColor.copy(alpha = 0.3f),
                    topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.5f),
                    size = Size(radius, radius),
                    style = Stroke(width = 4f)
                )
            }
            rotate(rotationAngle * 0.6f, center) {
                drawRect(
                    color = theme.accentColor.copy(alpha = 0.5f),
                    topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.5f),
                    size = Size(radius, radius),
                    style = Stroke(width = 3f)
                )
            }
        }
        else -> { // Sacred Banyan Tree Light
            val sparkCount = 15
            for (s in 0 until sparkCount) {
                val sparkAngle = ((s * 24f) + (rotationAngle * 0.5f)) * (PI.toFloat() / 180f)
                val sparkDist = (radius * 0.2f) + ((s * 7) % radius.toInt())
                val sparkPos = Offset(
                    center.x + sparkDist * cos(sparkAngle),
                    center.y + sparkDist * sin(sparkAngle)
                )
                drawCircle(
                    color = theme.accentColor.copy(alpha = 0.9f),
                    radius = 4f + (s % 3),
                    center = sparkPos
                )
            }
        }
    }
}
