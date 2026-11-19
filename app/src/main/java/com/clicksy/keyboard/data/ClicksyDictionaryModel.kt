package com.clicksy.keyboard.data

/**
 * Data structures for Clicksy Keyboard's unified English + Hinglish dictionary.
 */
data class DictionaryWordEntry(
    val word: String,
    val language: String,
    val variants: List<String> = emptyList(),
    val frequency: Int,
    val shortForms: List<String> = emptyList(),
    val corrections: List<String> = emptyList(),
    val source: String,
    val license: String
)

data class DictionaryMetadata(
    val version: String = "1.0.0",
    val generatedAt: String = "",
    val totalWords: Int = 0,
    val englishWordCount: Int = 0,
    val hinglishWordCount: Int = 0,
    val totalShortcuts: Int = 0,
    val totalBigrams: Int = 0,
    val totalTrigrams: Int = 0
)

/**
 * Consumer interface for streaming dictionary parser.
 * Allows populating Trie and memory structures with zero intermediate object allocations.
 */
interface ClicksyDictionaryConsumer {
    fun onMetadata(metadata: DictionaryMetadata) {}
    fun onShortcut(shortcut: String, expansions: List<String>) {}
    fun onWord(entry: DictionaryWordEntry) {}
    fun onBigram(prev: String, next: String, frequency: Int) {}
    fun onTrigram(prevPrevPrev: String, next: String, frequency: Int) {}
    fun onProperCasing(lower: String, proper: String) {}
    fun onEmoji(keyword: String, emoji: String) {}
}
