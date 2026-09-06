package com.clicksy.keyboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DictionaryProviderTest {

    @Before
    fun setUp() {
        DictionaryProvider.initialize(null)
    }

    @Test
    fun testContractionExpansions() {
        val dontSuggestions = DictionaryProvider.getSuggestions("dont")
        assertTrue("Suggestions for 'dont' should contain 'don't'", dontSuggestions.any { it.equals("don't", ignoreCase = true) })

        val cantSuggestions = DictionaryProvider.getSuggestions("cant")
        assertTrue("Suggestions for 'cant' should contain 'can't'", cantSuggestions.any { it.equals("can't", ignoreCase = true) })

        val imSuggestions = DictionaryProvider.getSuggestions("im")
        assertTrue("Suggestions for 'im' should contain 'I'm'", imSuggestions.any { it.equals("I'm", ignoreCase = true) })

        val youreSuggestions = DictionaryProvider.getSuggestions("youre")
        assertTrue("Suggestions for 'youre' should contain 'you're'", youreSuggestions.any { it.equals("you're", ignoreCase = true) })

        val theyreSuggestions = DictionaryProvider.getSuggestions("theyre")
        assertTrue("Suggestions for 'theyre' should contain 'they're'", theyreSuggestions.any { it.equals("they're", ignoreCase = true) })

        val didntSuggestions = DictionaryProvider.getSuggestions("didnt")
        assertTrue("Suggestions for 'didnt' should contain 'didn't'", didntSuggestions.any { it.equals("didn't", ignoreCase = true) })

        val illSuggestions = DictionaryProvider.getSuggestions("ill")
        assertTrue("Suggestions for 'ill' should contain 'I'll' or 'ill'", illSuggestions.isNotEmpty())
    }

    @Test
    fun testCommonTypoCorrections() {
        val tehCorrection = DictionaryProvider.getAutoCorrection("teh")
        assertEquals("the", tehCorrection)

        val becuseCorrection = DictionaryProvider.getAutoCorrection("becuse")
        assertEquals("because", becuseCorrection)

        val mornignCorrection = DictionaryProvider.getAutoCorrection("mornign")
        assertEquals("morning", mornignCorrection)

        val thabksCorrection = DictionaryProvider.getAutoCorrection("thabks")
        assertEquals("thanks", thabksCorrection)

        val almistCorrection = DictionaryProvider.getAutoCorrection("almist")
        assertEquals("almost", almistCorrection)

        val woukdCorrection = DictionaryProvider.getAutoCorrection("woukd")
        assertEquals("would", woukdCorrection)

        val iCorrection = DictionaryProvider.getAutoCorrection("i")
        assertEquals("I", iCorrection)
    }

    @Test
    fun testSlangAndAbbreviationExpansions() {
        val idkSuggestions = DictionaryProvider.getSuggestions("idk")
        assertTrue("Suggestions for 'idk' should contain 'I don't know'", idkSuggestions.any { it.equals("I don't know", ignoreCase = true) })

        val tbhSuggestions = DictionaryProvider.getSuggestions("tbh")
        assertTrue("Suggestions for 'tbh' should contain 'to be honest'", tbhSuggestions.any { it.equals("to be honest", ignoreCase = true) })

        val btwSuggestions = DictionaryProvider.getSuggestions("btw")
        assertTrue("Suggestions for 'btw' should contain 'by the way'", btwSuggestions.any { it.equals("by the way", ignoreCase = true) })
    }

    @Test
    fun testNextWordBigramPredictions() {
        val goodPredictions = DictionaryProvider.getNextWordPredictions("good")
        assertTrue("Predictions for 'good' should contain 'morning' or 'night'",
            goodPredictions.contains("morning") || goodPredictions.contains("night"))

        val thankPredictions = DictionaryProvider.getNextWordPredictions("thank")
        assertTrue("Predictions for 'thank' should contain 'you'", thankPredictions.contains("you"))

        val howPredictions = DictionaryProvider.getNextWordPredictions("how")
        assertTrue("Predictions for 'how' should contain 'are' or 'is'",
            howPredictions.contains("are") || howPredictions.contains("is"))
    }

    @Test
    fun testContextAwarePrefixCompletion() {
        val morningSuggestions = DictionaryProvider.getSuggestions("m", prevWord = "good")
        assertEquals("morning", morningSuggestions.firstOrNull()?.lowercase())

        val areSuggestions = DictionaryProvider.getSuggestions("a", prevWord = "how")
        assertEquals("are", areSuggestions.firstOrNull()?.lowercase())

        val youSuggestions = DictionaryProvider.getSuggestions("y", prevWord = "thank")
        assertEquals("you", youSuggestions.firstOrNull()?.lowercase())
    }

    @Test
    fun testSpatialProximityDistance() {
        // Adjacent keys on QWERTY
        val distAdj = KeyboardProximity.getSubstitutionCost('o', 'p')
        assertTrue("Substitution cost between adjacent keys 'o' and 'p' should be low (<= 0.5)", distAdj <= 0.5f)

        // Far keys
        val distFar = KeyboardProximity.getSubstitutionCost('q', 'm')
        assertEquals(1.0f, distFar, 0.01f)

        // Typo weighted distance
        val distTypo = KeyboardProximity.calculateWeightedDistance("thabks", "thanks")
        assertTrue("Weighted distance for adjacent typo 'thabks' -> 'thanks' should be <= 1.0", distTypo <= 1.0f)
    }

    @Test
    fun testInlineEmojiSuggestions() {
        val fireSuggestions = DictionaryProvider.getSuggestions("fire")
        assertTrue("Suggestions for 'fire' should contain fire emoji 🔥", fireSuggestions.contains("🔥"))

        val loveSuggestions = DictionaryProvider.getSuggestions("love")
        assertTrue("Suggestions for 'love' should contain heart emoji ❤️", loveSuggestions.contains("❤️"))
    }

    @Test
    fun testCasingPreservation() {
        val upper = DictionaryProvider.applyCasing("HEL", "hello")
        assertEquals("HELLO", upper)

        val title = DictionaryProvider.applyCasing("Hel", "hello")
        assertEquals("Hello", title)

        val lower = DictionaryProvider.applyCasing("hel", "hello")
        assertEquals("hello", lower)

        val pronoun = DictionaryProvider.applyCasing("im", "i'm")
        assertEquals("I'm", pronoun)
    }
}
