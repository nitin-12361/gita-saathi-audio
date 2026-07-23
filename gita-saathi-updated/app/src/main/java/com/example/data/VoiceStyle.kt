package com.example.data

enum class VoiceStyle(
    val id: String,
    val titleEnglish: String,
    val titleHindi: String,
    val descriptionEnglish: String,
    val descriptionHindi: String,
    val pitch: Float,
    val defaultSpeed: Float,
    val geminiVoiceName: String,
    val pauseDandaMs: Int,
    val pauseCommaMs: Int,
    val pauseSectionMs: Int
) {
    DEVOTIONAL(
        id = "devotional",
        titleEnglish = "Devotional Tone",
        titleHindi = "भक्ति स्वर",
        descriptionEnglish = "Deep, meditative & resonant voice with sacred pauses",
        descriptionHindi = "गंभीर, ध्यानमग्न और भक्तिमय वाणी",
        pitch = 0.85f,
        defaultSpeed = 1.0f,
        geminiVoiceName = "Fenrir",
        pauseDandaMs = 900,
        pauseCommaMs = 450,
        pauseSectionMs = 1200
    ),
    CALM(
        id = "calm",
        titleEnglish = "Calm Narrator",
        titleHindi = "शांत वाचक",
        descriptionEnglish = "Warm, soothing & balanced narrative cadence",
        descriptionHindi = "मधुर, शांत और संतुलित कथा प्रवाह",
        pitch = 0.95f,
        defaultSpeed = 1.0f,
        geminiVoiceName = "Kore",
        pauseDandaMs = 650,
        pauseCommaMs = 300,
        pauseSectionMs = 900
    ),
    STORYTELLER(
        id = "storyteller",
        titleEnglish = "Simple Storyteller",
        titleHindi = "सरल कथावाचक",
        descriptionEnglish = "Clear, expressive & easy to follow storytelling voice",
        descriptionHindi = "स्पष्ट, सरस और सरल व्याख्यात्मक स्वर",
        pitch = 1.02f,
        defaultSpeed = 1.0f,
        geminiVoiceName = "Puck",
        pauseDandaMs = 500,
        pauseCommaMs = 250,
        pauseSectionMs = 700
    );

    companion object {
        fun fromId(id: String?): VoiceStyle {
            if (id == null) return DEVOTIONAL
            return entries.find { it.id == id } ?: DEVOTIONAL
        }
    }
}
