package com.example.innovexia.memory.Mind.ingest

import com.example.innovexia.memory.Mind.api.EmotionType
import com.example.innovexia.memory.Mind.api.MemoryKind

/**
 * Heuristics for classifying memory kind and emotion
 */
object Heuristics {

    /**
     * Classify memory kind based on text patterns
     */
    fun classifyKind(text: String): MemoryKind {
        val lower = text.lowercase()

        return when {
            // Preferences
            lower.contains("i like") || lower.contains("i prefer") ||
            lower.contains("i love") || lower.contains("i hate") ||
            lower.contains("i enjoy") || lower.contains("my favorite") -> MemoryKind.PREFERENCE

            // Events
            lower.contains("yesterday") || lower.contains("today") ||
            lower.contains("tomorrow") || lower.contains("last week") ||
            lower.contains("went to") || lower.contains("going to") ||
            lower.matches(Regex(".*\\d{1,2}[:/]\\d{1,2}.*")) -> MemoryKind.EVENT

            // Projects
            lower.contains("working on") || lower.contains("building") ||
            lower.contains("project") || lower.contains("planning to") ||
            lower.contains("goal") -> MemoryKind.PROJECT

            // Facts (named entities, locations, people)
            lower.contains("my name is") || lower.contains("i am") ||
            lower.contains("i'm") || lower.contains("i live") ||
            lower.contains(" in ") && lower.matches(Regex(".*\\b[A-Z][a-z]+\\b.*")) -> MemoryKind.FACT

            // Knowledge/learning
            lower.contains("learned") || lower.contains("discovered") ||
            lower.contains("found out") || lower.contains("understand") -> MemoryKind.KNOWLEDGE

            // Emotions
            lower.contains("feel") || lower.contains("feeling") ||
            lower.contains("emotion") -> MemoryKind.EMOTION

            else -> MemoryKind.OTHER
        }
    }

    /**
     * Detect emotion from text
     */
    fun detectEmotion(text: String): EmotionType? {
        val lower = text.lowercase()

        return when {
            // Happy
            lower.contains("happy") || lower.contains("excited") ||
            lower.contains("great") || lower.contains("awesome") ||
            lower.contains("wonderful") || lower.contains("😊") ||
            lower.contains("😀") || lower.contains("🎉") -> EmotionType.HAPPY

            // Excited
            lower.contains("can't wait") || lower.contains("so excited") ||
            lower.contains("amazing") || lower.contains("🤩") -> EmotionType.EXCITED

            // Sad
            lower.contains("sad") || lower.contains("disappointed") ||
            lower.contains("unfortunate") || lower.contains("😢") ||
            lower.contains("😞") -> EmotionType.SAD

            // Frustrated
            lower.contains("frustrated") || lower.contains("annoying") ||
            lower.contains("difficult") || lower.contains("struggling") -> EmotionType.FRUSTRATED

            // Anxious
            lower.contains("worried") || lower.contains("nervous") ||
            lower.contains("anxious") || lower.contains("concerned") -> EmotionType.ANXIOUS

            // Curious
            lower.contains("curious") || lower.contains("wondering") ||
            lower.contains("how does") || lower.contains("why") ||
            lower.contains("what if") -> EmotionType.CURIOUS

            // Confident
            lower.contains("confident") || lower.contains("sure") ||
            lower.contains("definitely") -> EmotionType.CONFIDENT

            else -> EmotionType.NEUTRAL
        }
    }

    /**
     * Calculate importance score based on heuristics
     */
    fun calculateImportance(text: String, kind: MemoryKind, emotion: EmotionType?): Double {
        var score = 0.5 // base

        // Length factor (longer = more specific)
        val words = text.split("\\s+".toRegex()).size
        score += when {
            words > 50 -> 0.2
            words > 20 -> 0.1
            words < 5 -> -0.1
            else -> 0.0
        }

        // Kind weight
        score += when (kind) {
            MemoryKind.PREFERENCE -> 0.15
            MemoryKind.FACT -> 0.1
            MemoryKind.PROJECT -> 0.15
            MemoryKind.EVENT -> 0.05
            else -> 0.0
        }

        // Emotion weight (strong emotions are important)
        score += when (emotion) {
            EmotionType.EXCITED, EmotionType.FRUSTRATED, EmotionType.ANXIOUS -> 0.1
            EmotionType.HAPPY, EmotionType.SAD -> 0.05
            else -> 0.0
        }

        // Named entities (capitalized words)
        val capitals = Regex("\\b[A-Z][a-z]+\\b").findAll(text).count()
        score += capitals * 0.02

        // Clamp to 0.0-1.0
        return score.coerceIn(0.0, 1.0)
    }
}
