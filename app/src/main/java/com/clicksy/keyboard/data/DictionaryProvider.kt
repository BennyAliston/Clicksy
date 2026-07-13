package com.clicksy.keyboard.data

import android.content.Context
import java.io.File

/**
 * Trie-based dictionary for fast prefix-matching autocomplete suggestions.
 * Pre-loaded with ~500 common English words, and learns custom user-typed words dynamically.
 */
object DictionaryProvider {

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var isWord = false
        var frequency = 0 // Higher = more common
    }

    private val root = TrieNode()
    private var initialized = false
    private val userWords = mutableMapOf<String, Int>()

    /**
     * Initializes the static common dictionary and loads user-learned words from local disk.
     */
    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            
            // 1. Load static common words
            commonWords.forEachIndexed { index, word ->
                insert(word.lowercase(), commonWords.size - index)
            }
            
            // 2. Load user-learned words from user_dict.txt
            try {
                val file = File(context.filesDir, "user_dict.txt")
                if (file.exists()) {
                    file.readLines().forEach { line ->
                        val parts = line.split(":")
                        if (parts.size == 2) {
                            val word = parts[0].trim().lowercase()
                            val freq = parts[1].toIntOrNull() ?: 10
                            if (word.isNotEmpty()) {
                                userWords[word] = freq
                                insert(word, freq)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            initialized = true
        }
    }

    private fun insert(word: String, frequency: Int) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isWord = true
        node.frequency = maxOf(node.frequency, frequency)
    }

    /**
     * Learns a custom word by inserting it into the Trie and persisting to storage.
     */
    fun learnWord(context: Context, word: String) {
        if (word.isBlank() || word.length < 2) return
        val normalized = word.trim().lowercase()
        // Only learn valid alphabetic words to avoid learning typos with special characters/numbers
        if (!normalized.all { it.isLetter() }) return

        initialize(context)

        synchronized(this) {
            // Static words can also be up-ranked if typed repeatedly
            val currentFreq = userWords[normalized] ?: 0
            val newFreq = currentFreq + 10 // Increment frequency rank
            userWords[normalized] = newFreq
            insert(normalized, newFreq)

            // Save user dictionary to disk
            try {
                val file = File(context.filesDir, "user_dict.txt")
                file.printWriter().use { writer ->
                    userWords.forEach { (w, f) ->
                        writer.println("$w:$f")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Returns up to [limit] autocomplete suggestions for the given [prefix].
     * Results are sorted by frequency (most common first).
     */
    fun getSuggestions(prefix: String, limit: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()

        val lowerPrefix = prefix.lowercase()
        var node = root
        for (char in lowerPrefix) {
            node = node.children[char] ?: return emptyList()
        }

        val results = mutableListOf<Pair<String, Int>>()
        collectWords(node, StringBuilder(lowerPrefix), results)

        return results
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun collectWords(
        node: TrieNode,
        current: StringBuilder,
        results: MutableList<Pair<String, Int>>
    ) {
        if (node.isWord) {
            results.add(current.toString() to node.frequency)
        }
        for ((char, child) in node.children) {
            current.append(char)
            collectWords(child, current, results)
            current.deleteCharAt(current.length - 1)
        }
    }

    // Top ~500 most common English words
    private val commonWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "great", "between", "need", "large", "often", "hand", "high", "place", "keep", "help",
        "every", "never", "start", "city", "right", "small", "night", "always", "next", "hard",
        "open", "seem", "together", "each", "begin", "while", "own", "point", "house", "world",
        "near", "build", "self", "home", "much", "both", "here", "move", "still", "end",
        "school", "head", "turn", "real", "leave", "might", "door", "set", "close", "long",
        "before", "last", "left", "few", "side", "been", "call", "part", "early", "water",
        "find", "put", "thing", "many", "play", "away", "animal", "old", "follow", "learn",
        "change", "more", "run", "off", "again", "read", "sure", "under", "going", "stop",
        "let", "thought", "important", "until", "children", "food", "kind", "country", "number", "line",
        "tell", "does", "same", "mean", "differ", "move", "right", "boy", "did", "does",
        "three", "air", "well", "play", "small", "end", "put", "home", "read", "hand",
        "port", "large", "spell", "add", "even", "land", "here", "must", "big", "high",
        "such", "follow", "act", "why", "ask", "men", "change", "went", "light", "kind",
        "off", "need", "house", "picture", "try", "us", "again", "animal", "point", "mother",
        "world", "near", "build", "self", "earth", "father", "head", "stand", "own", "page",
        "should", "country", "found", "answer", "school", "grow", "study", "still", "learn", "plant",
        "cover", "food", "sun", "four", "between", "state", "keep", "eye", "never", "last",
        "let", "thought", "city", "tree", "cross", "farm", "hard", "start", "might", "story",
        "saw", "far", "sea", "draw", "left", "late", "run", "while", "press", "close",
        "night", "real", "life", "few", "north", "open", "seem", "together", "next", "white",
        "children", "begin", "got", "walk", "example", "ease", "paper", "group", "always", "music",
        "those", "both", "mark", "often", "letter", "until", "mile", "river", "car", "feet",
        "care", "second", "book", "carry", "took", "science", "eat", "room", "friend", "began",
        "idea", "fish", "mountain", "stop", "once", "base", "hear", "horse", "cut", "sure",
        "watch", "color", "face", "wood", "main", "enough", "plain", "girl", "usual", "young",
        "ready", "above", "ever", "red", "list", "though", "feel", "talk", "bird", "soon",
        "body", "dog", "family", "direct", "pose", "leave", "song", "measure", "door", "product",
        "black", "short", "numeral", "class", "wind", "question", "happen", "complete", "ship", "area",
        "half", "rock", "order", "fire", "south", "problem", "piece", "told", "knew", "pass",
        "since", "top", "whole", "king", "space", "heard", "best", "hour", "better", "true",
        "during", "hundred", "five", "remember", "step", "early", "hold", "west", "ground", "interest",
        "reach", "fast", "verb", "sing", "listen", "six", "table", "travel", "less", "morning",
        "ten", "simple", "several", "vowel", "toward", "war", "lay", "against", "pattern", "slow",
        "center", "love", "person", "money", "serve", "appear", "road", "map", "rain", "rule",
        "govern", "pull", "cold", "notice", "voice", "unit", "power", "town", "fine", "certain",
        "fly", "fall", "lead", "cry", "dark", "machine", "note", "wait", "plan", "figure",
        "star", "box", "noun", "field", "rest", "correct", "able", "pound", "done", "beauty",
        "drive", "stood", "contain", "front", "teach", "week", "final", "gave", "green", "oh",
        "quick", "develop", "ocean", "warm", "free", "minute", "strong", "special", "mind", "behind",
        "clear", "tail", "produce", "fact", "street", "inch", "lot", "nothing", "course", "stay",
        "wheel", "full", "force", "blue", "object", "decide", "surface", "deep", "moon", "island",
        "foot", "system", "busy", "test", "record", "boat", "common", "gold", "possible", "plane",
        "age", "dry", "wonder", "laugh", "thousand", "ago", "ran", "check", "game", "shape",
        "yes", "hot", "miss", "brought", "heat", "snow", "bed", "bring", "sit", "perhaps",
        "fill", "east", "weight", "language", "among", "please", "thank", "hello", "okay", "sorry",
        "welcome", "goodbye", "maybe", "today", "tomorrow", "yesterday", "morning", "evening", "afternoon", "tonight",
        "happy", "awesome", "amazing", "beautiful", "wonderful", "perfect", "great", "nice", "cool", "funny",
        "love", "thanks", "congrats", "birthday", "party", "dinner", "lunch", "breakfast", "coffee", "meeting",
        "message", "phone", "email", "address", "name", "password", "account", "update", "download", "upload"
    )
}
