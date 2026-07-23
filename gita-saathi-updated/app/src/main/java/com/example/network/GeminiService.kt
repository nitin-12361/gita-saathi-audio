package com.example.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.AppLanguage
import com.example.data.Verse
import com.example.data.VoiceStyle
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "speechConfig") val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    @Json(name = "voiceConfig") val voiceConfig: VoiceConfig
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    @Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    @Json(name = "voiceName") val voiceName: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

interface GeminiApiService {
    @POST
    suspend fun generateWithUrl(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val DEFAULT_API_KEY = "AQ.Ab8RN6K6fkizEm4PkSm5Qqv9x0nBX85BbetLoJONOsSOw1PpGg"

    private class NetworkLoggingAndErrorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            val cleanUrl = request.url.toString().replace(Regex("key=[^&]+"), "key=REDACTED")
            if (!response.isSuccessful) {
                Log.e("GeminiNetworkInterceptor", "HTTP Error status ${response.code} for URL: $cleanUrl")
            } else {
                Log.d("GeminiNetworkInterceptor", "HTTP Success status ${response.code} for URL: ${request.url.encodedPath}")
            }
            return response
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(NetworkLoggingAndErrorInterceptor())
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun formatHttpException(e: Exception, language: AppLanguage): String {
        return when (e) {
            is java.net.SocketTimeoutException -> {
                if (language == AppLanguage.HINDI)
                    "सर्वर प्रतिक्रिया में समय सीमा समाप्त हो गई। कृपया पुनः प्रयास करें।"
                else
                    "Network timeout while connecting to Gemini service. Please try again."
            }
            is java.net.UnknownHostException -> {
                if (language == AppLanguage.HINDI)
                    "इंटरनेट कनेक्शन उपलब्ध नहीं है। कृपया अपना नेटवर्क जांचें।"
                else
                    "No internet connection. Please check your network connection."
            }
            is retrofit2.HttpException -> {
                val code = e.code()
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e("GeminiClient", "API HTTP $code error body: $errorBody")

                when {
                    code == 404 -> {
                        if (language == AppLanguage.HINDI)
                            "अनुरोधित ऑडियो मॉडल या स्वर उपलब्ध नहीं है (HTTP 404)। कृपया दूसरा स्वर चुनें।"
                        else
                            "The requested audio model or voice endpoint was not found (HTTP 404). Please try a different voice."
                    }
                    code == 400 -> {
                        if (errorBody?.contains("API key", ignoreCase = true) == true) {
                            if (language == AppLanguage.HINDI) "अमान्य API कुंजी (HTTP 400)। कृपया सेटिंग्स से अपनी Gemini Key जांचें।"
                            else "Invalid API Key (HTTP 400). Please verify your Gemini API key in Settings."
                        } else {
                            if (language == AppLanguage.HINDI) "अमान्य अनुरोध (HTTP 400)। कृपया कोई अन्य स्वर शैली चुनें।"
                            else "Invalid request formatting (HTTP 400). Please try a different voice style."
                        }
                    }
                    code == 401 || code == 403 -> {
                        if (language == AppLanguage.HINDI)
                            "अमान्य या अनधिकृत API कुंजी (HTTP $code)। कृपया सेटिंग्स से अपनी Gemini Key अद्यतन करें।"
                        else
                            "Invalid or unauthorized API key (HTTP $code). Please update your API key in Settings."
                    }
                    code == 429 -> {
                        if (language == AppLanguage.HINDI)
                            "API उपयोग सीमा समाप्त हो गई है (HTTP 429)। कृपया कुछ समय बाद प्रयास करें।"
                        else
                            "API rate limit reached (HTTP 429). Please wait a few seconds and try again."
                    }
                    code >= 500 -> {
                        if (language == AppLanguage.HINDI)
                            "Gemini सर्वर में समस्या आई है (HTTP $code)। कृपया थोड़ी देर में प्रयास करें।"
                        else
                            "Gemini service temporary server error (HTTP $code). Please try again shortly."
                    }
                    else -> {
                        if (language == AppLanguage.HINDI) "सर्वर त्रुटि (HTTP $code)"
                        else "Server Error (HTTP $code)"
                    }
                }
            }
            else -> {
                e.localizedMessage ?: if (language == AppLanguage.HINDI) "अज्ञात त्रुटि हुई।" else "An unexpected error occurred."
            }
        }
    }

    private fun getEffectiveApiKey(customKey: String?): String {
        if (!customKey.isNullOrBlank()) return customKey
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (!buildKey.isNullOrBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey
        return DEFAULT_API_KEY
    }

    private suspend fun callTextWithFallback(apiKey: String, request: GenerateContentRequest): GenerateContentResponse {
        val textModels = listOf(
            "v1beta/models/gemini-2.5-flash:generateContent",
            "v1beta/models/gemini-3.5-flash:generateContent"
        )
        var lastError: Exception? = null
        for (url in textModels) {
            try {
                return service.generateWithUrl(url, apiKey, request)
            } catch (e: Exception) {
                Log.w("GeminiService", "Text model failed ($url): ${e.message}")
                lastError = e
            }
        }
        throw lastError ?: Exception("Text generation failed.")
    }

    private suspend fun callTtsWithFallback(apiKey: String, request: GenerateContentRequest): GenerateContentResponse {
        val ttsModels = listOf(
            "v1beta/models/gemini-2.0-flash:generateContent",
            "v1beta/models/gemini-2.5-flash:generateContent",
            "v1beta/models/gemini-2.0-flash-exp:generateContent"
        )
        var lastError: Exception? = null

        // Attempt with speechConfig
        for (url in ttsModels) {
            try {
                val response = service.generateWithUrl(url, apiKey, request)
                val inlineData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData
                if (inlineData != null && inlineData.data.isNotBlank()) {
                    return response
                } else {
                    Log.w("GeminiService", "Audio response was empty for $url")
                    lastError = Exception("Audio response was empty for $url")
                }
            } catch (e: Exception) {
                Log.w("GeminiService", "TTS model failed ($url): ${e.message}")
                lastError = e
            }
        }

        // Attempt without speechConfig if initial request had speechConfig
        if (request.generationConfig?.speechConfig != null) {
            val fallbackRequest = request.copy(
                generationConfig = request.generationConfig.copy(speechConfig = null)
            )
            for (url in ttsModels) {
                try {
                    val response = service.generateWithUrl(url, apiKey, fallbackRequest)
                    val inlineData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData
                    if (inlineData != null && inlineData.data.isNotBlank()) {
                        return response
                    } else {
                        Log.w("GeminiService", "Fallback audio response was empty for $url")
                        lastError = Exception("Fallback audio response was empty for $url")
                    }
                } catch (e: Exception) {
                    Log.w("GeminiService", "TTS fallback model failed ($url): ${e.message}")
                    lastError = e
                }
            }
        }

        throw lastError ?: Exception("TTS generation failed on all models.")
    }

    suspend fun getVerseInsight(verse: Verse, language: AppLanguage, customApiKey: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key is missing."))
        }

        val prompt = if (language == AppLanguage.HINDI) {
            "आप श्रीमद्भगवद्गीता के एक महान विद्वान और मार्गदर्शक हैं। कृपया अध्याय ${verse.chapterId}, श्लोक ${verse.verseId} (${verse.shlokaSanskrit}) का सरल, शांत और जीवनोपयोगी अर्थ समझाइए। यह समझाइए कि आज के आधुनिक जीवन में इस श्लोक को कैसे अपनाएं। उत्तर को 3-4 संक्षिप्त, सुंदर और प्रेरणादायक अनुच्छेदों में रखें।"
        } else {
            "You are a compassionate scholar and spiritual guide of the Bhagavad Gita. Please explain Chapter ${verse.chapterId}, Verse ${verse.verseId} (${verse.transliteration}). Provide a profound, calming, and practical explanation on how one can apply this wisdom to modern daily life. Keep the response in 3-4 inspiring paragraphs."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are Gita Saathi, a calm, spiritual AI companion providing peaceful and clear wisdom from the Bhagavad Gita.")))
        )

        try {
            val response = callTextWithFallback(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception(if (language == AppLanguage.HINDI) "कोई उत्तर प्राप्त नहीं हुआ।" else "No insight text returned."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatHttpException(e, language)))
        }
    }

    suspend fun askGitaAi(userQuery: String, language: AppLanguage, customApiKey: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception(if (language == AppLanguage.HINDI) "API कुंजी की आवश्यकता है।" else "API key is missing."))
        }

        val systemPrompt = if (language == AppLanguage.HINDI) {
            "आप 'गीता साथी' हैं - श्रीमद्भगवद्गीता के कालातीत ज्ञान पर आधारित एक करुणामय एवं ज्ञानी AI मार्गदर्शक। प्रयोगकर्ता के प्रश्नों का उत्तर केवल और केवल भगवद्गीता के श्लोकों, सिद्धांतों (कर्मयोग, ज्ञानयोग, भक्तियोग, मन-नियंत्रण) और श्री कृष्ण के उपदेशों के संदर्भ में दें। उत्तर सरल, प्रेरणादायक और व्यावहारिक जीवनोपयोगी रखें।"
        } else {
            "You are 'Gita Saathi' - a compassionate and wise AI spiritual guide rooted in the timeless teachings of the Bhagavad Gita. Answer user questions directly by drawing from the Bhagavad Gita's verses, philosophy (Karma Yoga, Bhakti Yoga, Jnana Yoga, mind control), and Lord Krishna's guidance. Provide practical, peaceful, and inspiring advice for daily life."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userQuery)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = callTextWithFallback(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception(if (language == AppLanguage.HINDI) "कोई प्रतिक्रिया प्राप्त नहीं हुई।" else "No response received."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatHttpException(e, language)))
        }
    }

    suspend fun getOrFetchTtsAudio(
        context: Context,
        verse: Verse,
        language: AppLanguage,
        voiceStyle: VoiceStyle,
        customApiKey: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val cacheKey = "gemini_tts_v8_${verse.verseKey}_${language.name}_${voiceStyle.id}.wav"
        val cacheFile = File(context.cacheDir, cacheKey)

        if (cacheFile.exists() && cacheFile.length() > 0) {
            return@withContext Result.success(cacheFile)
        }

        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception(if (language == AppLanguage.HINDI) "स्वर वाचन के लिए Gemini API कुंजी की आवश्यकता है।" else "Gemini API key is required for Gemini Text-to-Speech narration."))
        }

        val ttsPrompt = buildTtsPrompt(verse, language)
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = ttsPrompt)))),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = SpeechConfig(
                    voiceConfig = VoiceConfig(
                        prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceStyle.geminiVoiceName)
                    )
                )
            )
        )

        try {
            val response = callTtsWithFallback(apiKey, request)
            val inlineData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData

            if (inlineData != null && inlineData.data.isNotBlank()) {
                val rawBytes = Base64.decode(inlineData.data, Base64.DEFAULT)
                val playableBytes = convertToPlayableAudio(rawBytes, inlineData.mimeType)
                cacheFile.writeBytes(playableBytes)
                Result.success(cacheFile)
            } else {
                Result.failure(Exception(if (language == AppLanguage.HINDI) "खाली ऑडियो प्राप्त हुआ।" else "Gemini TTS returned empty audio stream."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatHttpException(e, language)))
        }
    }

    suspend fun getOrFetchSampleTtsAudio(
        context: Context,
        language: AppLanguage,
        voiceStyle: VoiceStyle,
        customApiKey: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val cacheKey = "gemini_sample_tts_v8_${language.name}_${voiceStyle.id}.wav"
        val cacheFile = File(context.cacheDir, cacheKey)

        if (cacheFile.exists() && cacheFile.length() > 0) {
            return@withContext Result.success(cacheFile)
        }

        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception(if (language == AppLanguage.HINDI) "API कुंजी आवश्यक है।" else "Gemini API key is required."))
        }

        val samplePrompt = if (language == AppLanguage.HINDI) {
            "You are a warm, authentic, highly realistic human guide with natural vocal inflections and expressive emotional depth. Perform in 3 clear steps:\n\n1. RECITE SANSKRIT SHLOKA SLOWLY: Recite this sacred Sanskrit verse slowly and reverently, deep resonant tone, pausing at every danda, the way a temple priest recites (not singing — a slow devotional recitation):\n[slow devotional recitation] ॐ नमो भगवते वासुदेवाय॥ [pause]\n[slow devotional recitation] कर्मण्येवाधिकारस्ते मा फलेषु कदाचन। मा कर्मफलहेतुर्भूर्मा ते सङ्गोऽस्त्वकर्मणि॥ [pause]\n\n2. MEANING IN HINGLISH: Transition into a warm, realistic human speaking voice and explain in natural Hinglish:\n[warm human voice] Is pavitra shloka ka matlab hai ki aap apne pure dedication aur effort se kaam karein, par outcome ki anxiety na lein. [pause]\n\n3. NEW-ERA MODERN EXAMPLE: Provide a practical 2-minute modern real-life example in Hinglish:\n[empathetic human voice] Aaj ke digital age mein, jab hum career goals, exams, ya corporate projects par kaam karte hain, to hamara focus aksar results par hota hai. Is verse ka lesson yeh hai ki aap apne process aur daily effort par 100% attention dein. Stress automatic khatam hoga aur success real mental peace ke saath milegi."
        } else {
            "You are a warm, authentic, highly realistic human guide with natural vocal inflections and expressive emotional depth. Perform in 3 clear steps:\n\n1. RECITE SANSKRIT SHLOKA SLOWLY: Recite this sacred Sanskrit verse slowly and reverently, deep resonant tone, pausing at every danda, the way a temple priest recites (not singing — a slow devotional recitation):\n[slow devotional recitation] Om Namo Bhagavate Vasudevaya. [pause] Karman-yeva-adhikaraste, maa phaleshu kadachana. [pause]\n\n2. MEANING IN ENGLISH: Transition into a warm, realistic human speaking voice and explain in friendly, empathetic English:\n[warm human voice] The sacred meaning of this verse is to dedicate yourself to action without being paralyzed by anxiety over results. [pause]\n\n3. NEW-ERA MODERN EXAMPLE: Provide a detailed practical modern real-world example in natural English:\n[empathetic human voice] In today's fast-paced digital era, whether you are managing work deadlines, exams, or personal projects, worrying about the future causes unnecessary burnout. By anchoring your mind in the present action and giving your absolute best effort, you experience clarity, resilience, and true inner peace."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = samplePrompt)))),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = SpeechConfig(
                    voiceConfig = VoiceConfig(
                        prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceStyle.geminiVoiceName)
                    )
                )
            )
        )

        try {
            val response = callTtsWithFallback(apiKey, request)
            val inlineData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData

            if (inlineData != null && inlineData.data.isNotBlank()) {
                val rawBytes = Base64.decode(inlineData.data, Base64.DEFAULT)
                val playableBytes = convertToPlayableAudio(rawBytes, inlineData.mimeType)
                cacheFile.writeBytes(playableBytes)
                Result.success(cacheFile)
            } else {
                Result.failure(Exception(if (language == AppLanguage.HINDI) "नमूना ऑडियो खाली प्राप्त हुआ।" else "Gemini TTS sample returned empty audio."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatHttpException(e, language)))
        }
    }

    private fun convertToPlayableAudio(audioBytes: ByteArray, mimeType: String?): ByteArray {
        if (audioBytes.isEmpty()) return audioBytes

        val isWav = audioBytes.size >= 4 &&
                audioBytes[0] == 'R'.code.toByte() &&
                audioBytes[1] == 'I'.code.toByte() &&
                audioBytes[2] == 'F'.code.toByte() &&
                audioBytes[3] == 'F'.code.toByte()

        val isMp3 = (audioBytes.size >= 3 &&
                audioBytes[0] == 'I'.code.toByte() &&
                audioBytes[1] == 'D'.code.toByte() &&
                audioBytes[2] == '3'.code.toByte()) ||
                (audioBytes.size >= 2 &&
                (audioBytes[0].toInt() and 0xFF) == 0xFF &&
                (audioBytes[1].toInt() and 0xE0) == 0xE0)

        val isOgg = audioBytes.size >= 4 &&
                audioBytes[0] == 'O'.code.toByte() &&
                audioBytes[1] == 'g'.code.toByte() &&
                audioBytes[2] == 'g'.code.toByte() &&
                audioBytes[3] == 'S'.code.toByte()

        if (isWav || isMp3 || isOgg) {
            return audioBytes
        }

        var sampleRate = 24000
        if (!mimeType.isNullOrBlank()) {
            val rateRegex = Regex("rate=(\\d+)")
            val match = rateRegex.find(mimeType)
            if (match != null) {
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let {
                    sampleRate = it
                }
            }
        }

        return createWavHeader(audioBytes, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)
    }

    private fun createWavHeader(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        val wavData = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavData, 0, 44)
        System.arraycopy(pcmData, 0, wavData, 44, pcmData.size)
        return wavData
    }

    private fun buildTtsPrompt(verse: Verse, language: AppLanguage): String {
        val cleanShloka = verse.shlokaSanskrit
            .replace("॥", " [pause] ")
            .replace("।", " [pause] ")

        // NOTE: Gemini TTS is a speech model, not a singing/music model — it cannot
        // actually sing a melody or add background music. We ask it to *recite* the
        // shloka slowly with devotional cadence instead. The real chanted/sung audio
        // (if you have one) is layered in by GitaAudioEngine from a local file before
        // this narration plays — see assets/chants/ and README_AUDIO_SETUP.md.
        val instructionAndShloka = """
            |You are a warm, highly realistic, empathetic human spiritual guide speaking in a natural, conversational voice. 
            |Your vocal delivery must sound like an authentic human guide speaking directly from the heart, with natural cadence, soft breath micro-pauses, and expressive emotional warmth.
            |
            |Execute the narration in these 3 seamless steps:
            |
            |STEP 1: RECITE THE SANSKRIT SHLOKA SLOWLY AND DEVOTIONALLY
            |Recite this sacred Bhagavad Gita verse (Chapter ${verse.chapterId}, Verse ${verse.verseId}) slowly, clearly, and reverently, the way a temple priest recites — unhurried pace, deep resonant tone, a distinct pause at every danda:
            |[slow devotional recitation] $cleanShloka [pause]
        """.trimMargin()

        return if (language == AppLanguage.HINDI) {
            """
            |$instructionAndShloka
            |
            |STEP 2: EXPLAIN THE MEANING IN NATURAL HINGLISH
            |After singing the shloka, seamlessly transition into a warm, realistic human speaking voice. Speak in natural, everyday Hinglish (Hindi mixed with popular English words) so it feels like a real human friend and guide speaking to the listener:
            |[warm human voice]
            |Ab is pavitra shloka ka matlab aur arth samjhte hain.
            |${verse.translationHindi}
            |${verse.meaningHindi} [pause]
            |
            |STEP 3: NEW-ERA MODERN REAL-WORLD EXAMPLE (~2 MINUTES COMPREHENSIVE EXPLANATION IN HINGLISH)
            |Now, give a detailed, engaging real-life modern example in conversational Hinglish that connects this verse directly to modern life today (such as corporate stress, career anxiety, startup hurdles, exam pressure, social media distractions, or emotional balance):
            |[empathetic human voice]
            |Is shloka ka aaj ke new-era modern lifestyle mein ek bahut gehra practical application hai. 
            |Aaj kal ki fast-paced life mein, chahe hum office ke high-pressure projects handle kar rahe hon, competitive exams ki tayyari kar rahe hon, ya apne startup aur career goals ke peeche bhaag rahe hon—hamara mind hamesha future results aur outcome ke baare mein overthink karne lagta hai. 
            |Is verse ka sabse bada lesson yahi hai ki aap apna pure dedication, energy, aur 100% effort bina kisi outcome-stress ke present action par lagayein. 
            |Jab aap result ki tension chhod kar apne process par focus karte hain, to aapka mental burnout, anxiety, aur fear automatic khatam ho jata hai. 
            |Chaho aap student ho, software engineer ho, ya business owner—is timeless wisdom ko apni daily life mein apply karke aap apne mind ko calm, sharp, aur unstoppable bana sakte hain.
            """.trimMargin()
        } else {
            """
            |$instructionAndShloka
            |
            |STEP 2: EXPLAIN THE MEANING IN WARM CONVERSATIONAL ENGLISH
            |After singing the shloka, seamlessly transition into a warm, realistic human speaking voice. Speak in warm, clear, conversational English as an empathetic mentor:
            |[warm human voice]
            |Now, let us reflect on the sacred meaning of this verse.
            |${verse.translationEnglish}
            |${verse.meaningEnglish} [pause]
            |
            |STEP 3: NEW-ERA MODERN REAL-WORLD EXAMPLE (~2 MINUTES COMPREHENSIVE EXPLANATION IN ENGLISH)
            |Now, give a detailed, engaging real-life modern example in realistic human English connecting this verse directly to today's era (such as career ambitions, workplace stress, tech distractions, personal growth, and emotional resilience):
            |[empathetic human voice]
            |In today's modern digital era, this verse provides a crucial masterclass for our daily lives. 
            |Whether you are navigating demanding work deadlines, preparing for critical exams, or building a startup, we often become overwhelmed by anxiety about future results and expectations. 
            |This verse teaches us the powerful art of present-moment focus—devoting all your energy, skill, and heart into the effort itself without letting fear of failure paralyze your mind. 
            |When you shift your perspective from result-obsession to mastery of the current task, your stress dissipates, your performance peaks, and you cultivate deep, unbreakable inner peace. 
            |Applying this wisdom in your daily routine builds resilience, emotional balance, and true clarity in every modern endeavor.
            """.trimMargin()
        }
    }
}

