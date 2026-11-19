package com.clicksy.keyboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiDataTest {

    @Test
    fun `search with blank query returns empty list`() {
        assertTrue(EmojiData.search("").isEmpty())
        assertTrue(EmojiData.search("   ").isEmpty())
    }

    @Test
    fun `search with exact keyword returns matches`() {
        val smileResults = EmojiData.search("smile")
        assertTrue(smileResults.isNotEmpty())
        assertTrue(smileResults.contains("😊") || smileResults.contains("😄") || smileResults.contains("😀"))

        val fireResults = EmojiData.search("fire")
        assertTrue(fireResults.isNotEmpty())
        assertTrue(fireResults.contains("🔥"))

        val heartResults = EmojiData.search("heart")
        assertTrue(heartResults.isNotEmpty())
        assertTrue(heartResults.contains("❤️"))
    }

    @Test
    fun `search is case insensitive and trims whitespace`() {
        val lower = EmojiData.search("pizza")
        val upper = EmojiData.search("  PIZZA  ")
        assertEquals(lower, upper)
        assertTrue(upper.contains("🍕"))
    }

    @Test
    fun `search returns prefix matches`() {
        val prefixResults = EmojiData.search("happ")
        assertTrue(prefixResults.isNotEmpty())
        assertTrue(prefixResults.contains("😀") || prefixResults.contains("😃") || prefixResults.contains("😄"))
    }

    @Test
    fun `search returns results without duplicates and caps at 60`() {
        val results = EmojiData.search("food")
        assertTrue(results.isNotEmpty())
        assertEquals(results.size, results.distinct().size)
        assertTrue(results.size <= 60)
    }

    @Test
    fun `search matches category names as fallback`() {
        val travelResults = EmojiData.search("travel")
        assertTrue(travelResults.isNotEmpty())
        // Should contain emojis from the Travel category
        val travelCat = EmojiData.categories.first { it.name == "Travel" }
        assertTrue(travelResults.any { it in travelCat.emojis })
    }

    @Test
    fun `popular search tags are non-empty and well-formed`() {
        assertTrue(EmojiData.popularSearchTags.isNotEmpty())
        for (tag in EmojiData.popularSearchTags) {
            assertFalse(tag.label.isBlank())
            assertFalse(tag.emoji.isBlank())
            val results = EmojiData.search(tag.label)
            assertTrue("Expected results for tag ${tag.label}", results.isNotEmpty())
        }
    }
}
