package com.clicksy.keyboard.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

/**
 * Intelligent & Comprehensive English + Hinglish Prediction & Auto-Correction Engine for Clicksy.
 *
 * Capabilities:
 * - Unified English + Hinglish vocabulary (36,000+ entries) loaded asynchronously from clicksy_dictionary.json
 * - Prefix-based Trie completion with sub-millisecond lookup
 * - Hinglish-aware variant recognition & code-switching support (e.g. "accha" / "achha" / "acha", "nahi" / "nahin" / "nhi")
 * - Smart Contraction & SMS/Chat Abbreviation Expansions ("gm" -> "good morning", "idk" -> "I don't know", "kr" -> "kar", "nhi" -> "nahi")
 * - Contextual Trigram & Bigram Language Model for next-word prediction
 * - Spatial QWERTY Proximity & Damerau-Levenshtein typo correction
 * - Repeated character run normalization ("helllo" -> "hello", "bhaii" -> "bhai")
 * - Cultural proper casing for cities, entities, names, and acronyms ("delhi" -> "Delhi", "github" -> "GitHub")
 * - Contextual inline emoji keywords ("chai" -> ☕, "coffee" -> ☕, "bhai" -> 🫂)
 * - User-personalized on-device vocabulary learning (Unigrams, Bigrams, Trigrams) with privacy-safe storage
 * - Whitelisting and undo mechanisms when autocorrection is reverted
 * - User controls to toggle Hinglish, toggle learning, and clear learned data
 */
object DictionaryProvider {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var isWord = false
        var frequency = 0
        var word: String? = null
        val topWords = mutableListOf<Pair<String, Int>>()
    }

    private val root = TrieNode()

    @Volatile
    private var initialized = false

    // Control toggles
    @Volatile
    var isLearningEnabled: Boolean = true

    @Volatile
    var isHinglishEnabled: Boolean = true

    @Volatile
    var isNextWordPredictionEnabled: Boolean = true

    // Fast lookup sets & maps
    private val allDictionaryWords = mutableSetOf<String>()
    private val wordsByLength = Array(25) { mutableListOf<String>() }
    private val wordFrequencies = mutableMapOf<String, Int>()

    // Variant mapping: variant/shortform -> canonical word (e.g., "nhi" -> "nahi", "acha" -> "accha", "kr" -> "kar")
    private val variantToCanonicalMap = mutableMapOf<String, String>()

    // Canonical to variants mapping: canonical -> list of accepted variants
    private val canonicalToVariantsMap = mutableMapOf<String, MutableList<String>>()

    // Shortcut expansions: shortcut -> list of expansions
    private val shortcutsMap = mutableMapOf<String, MutableList<String>>()

    // Direct common typo corrections
    private val commonTypoMap = mutableMapOf<String, String>()

    // Inline contextual emoji recommendations
    private val emojiMap = mutableMapOf<String, String>()

    // Proper capitalization for acronyms, entities, platforms, names
    private val properCasingMap = mutableMapOf<String, String>()

    // Language models: bigram and trigram frequency maps
    private val bigramMap = mutableMapOf<String, MutableMap<String, Int>>()
    private val trigramMap = mutableMapOf<String, MutableMap<String, Int>>()

    // User-learned vocabulary (on-device only)
    private val userWords = mutableMapOf<String, Int>()
    private var cachedTopUserWords: List<String>? = null

    // Whitelist for words explicitly kept or restored by user via backspace undo
    private val userWhitelistedWords = mutableSetOf<String>()

    // Conversational starters
    private val defaultStarters = listOf(
        "Hi", "Hello", "Please", "Thank", "Hope", "Good", "Dear", "I", "We",
        "Kya", "Kaise", "Haan", "Bhai", "Suno", "Theek", "Kal", "Aaj", "Chalo"
    )

    /**
     * Initializes unified dictionary from clicksy_dictionary.json, pre-built n-grams & user learned vocabulary.
     */
    fun initialize(context: Context? = null) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            // 1. Pre-seed core English & Hinglish curated baseline datasets
            EnglishDictionaryData.loadWordFrequencies { word, freq ->
                val lower = word.lowercase()
                insert(lower, freq)
                allDictionaryWords.add(lower)
                wordFrequencies[lower] = max(wordFrequencies[lower] ?: 0, freq)
                if (lower.length < 25 && lower !in wordsByLength[lower.length]) {
                    wordsByLength[lower.length].add(lower)
                }
            }

            HinglishDictionaryData.wordsWithFrequencies.forEach { (word, freq) ->
                val lower = word.lowercase()
                insert(lower, freq)
                allDictionaryWords.add(lower)
                wordFrequencies[lower] = max(wordFrequencies[lower] ?: 0, freq)
                if (lower.length < 25 && lower !in wordsByLength[lower.length]) {
                    wordsByLength[lower.length].add(lower)
                }
            }

            // Pre-seed core mappings
            EnglishDictionaryData.properCasingMap.forEach { (k, v) -> properCasingMap[k.lowercase()] = v }
            HinglishDictionaryData.properCasingMap.forEach { (k, v) -> properCasingMap[k.lowercase()] = v }
            EnglishDictionaryData.commonTypoMap.forEach { (k, v) -> commonTypoMap[k.lowercase()] = v }
            HinglishDictionaryData.hinglishCommonTypoMap.forEach { (k, v) -> commonTypoMap[k.lowercase()] = v }
            EnglishDictionaryData.inlineEmojiMap.forEach { (k, v) -> emojiMap[k.lowercase()] = v }
            HinglishDictionaryData.hinglishInlineEmojiMap.forEach { (k, v) -> emojiMap[k.lowercase()] = v }

            EnglishDictionaryData.abbreviationExpansions.forEach { (k, v) ->
                val list = shortcutsMap.getOrPut(k.lowercase()) { mutableListOf() }
                v.forEach { exp -> if (exp !in list) list.add(exp) }
            }
            HinglishDictionaryData.hinglishAbbreviationExpansions.forEach { (k, v) ->
                val list = shortcutsMap.getOrPut(k.lowercase()) { mutableListOf() }
                v.forEach { exp -> if (exp !in list) list.add(exp) }
            }

            EnglishDictionaryData.defaultBigrams.forEach { (prev, map) ->
                val bMap = bigramMap.getOrPut(prev.lowercase()) { mutableMapOf() }
                map.forEach { (next, freq) -> bMap[next.lowercase()] = max(bMap[next.lowercase()] ?: 0, freq) }
            }
            HinglishDictionaryData.hinglishBigrams.forEach { (prev, map) ->
                val bMap = bigramMap.getOrPut(prev.lowercase()) { mutableMapOf() }
                map.forEach { (next, freq) -> bMap[next.lowercase()] = max(bMap[next.lowercase()] ?: 0, freq) }
            }

            EnglishDictionaryData.defaultTrigrams.forEach { (prev, map) ->
                val tMap = trigramMap.getOrPut(prev.lowercase()) { mutableMapOf() }
                map.forEach { (next, freq) -> tMap[next.lowercase()] = max(tMap[next.lowercase()] ?: 0, freq) }
            }
            HinglishDictionaryData.hinglishTrigrams.forEach { (prev, map) ->
                val tMap = trigramMap.getOrPut(prev.lowercase()) { mutableMapOf() }
                map.forEach { (next, freq) -> tMap[next.lowercase()] = max(tMap[next.lowercase()] ?: 0, freq) }
            }

            // Register proper casing entities in Trie & lookup set
            properCasingMap.keys.forEach { lower ->
                allDictionaryWords.add(lower)
                insert(lower, 20000)
                wordFrequencies[lower] = max(wordFrequencies[lower] ?: 0, 20000)
                if (lower.length < 25 && lower !in wordsByLength[lower.length]) {
                    wordsByLength[lower.length].add(lower)
                }
            }

            // 2. Load unified 36,000+ words dictionary (clicksy_dictionary.json)
            ClicksyDictionaryRepository.loadDictionary(context, object : ClicksyDictionaryConsumer {
                override fun onWord(entry: DictionaryWordEntry) {
                    val lower = entry.word.lowercase()
                    insert(lower, entry.frequency)
                    allDictionaryWords.add(lower)
                    wordFrequencies[lower] = max(wordFrequencies[lower] ?: 0, entry.frequency)
                    if (lower.length < 25 && lower !in wordsByLength[lower.length]) {
                        wordsByLength[lower.length].add(lower)
                    }

                    // Register variants
                    if (entry.variants.isNotEmpty()) {
                        val list = canonicalToVariantsMap.getOrPut(lower) { mutableListOf() }
                        entry.variants.forEach { v ->
                            val vLower = v.lowercase()
                            if (vLower !in list) list.add(vLower)
                            variantToCanonicalMap[vLower] = lower
                            allDictionaryWords.add(vLower)
                            wordFrequencies[vLower] = max(wordFrequencies[vLower] ?: 0, entry.frequency - 500)
                            if (vLower.length < 25 && vLower !in wordsByLength[vLower.length]) {
                                wordsByLength[vLower.length].add(vLower)
                            }
                            insert(vLower, entry.frequency - 500)
                        }
                    }

                    // Register corrections
                    entry.corrections.forEach { corr ->
                        val corrLower = corr.lowercase()
                        commonTypoMap[corrLower] = lower
                    }
                }

                override fun onShortcut(shortcut: String, expansions: List<String>) {
                    val list = shortcutsMap.getOrPut(shortcut.lowercase()) { mutableListOf() }
                    expansions.forEach { exp ->
                        if (exp !in list) list.add(exp)
                    }
                }

                override fun onBigram(prev: String, next: String, frequency: Int) {
                    val map = bigramMap.getOrPut(prev.lowercase()) { mutableMapOf() }
                    map[next.lowercase()] = max(map[next.lowercase()] ?: 0, frequency)
                }

                override fun onTrigram(prevPrevPrev: String, next: String, frequency: Int) {
                    val map = trigramMap.getOrPut(prevPrevPrev.lowercase()) { mutableMapOf() }
                    map[next.lowercase()] = max(map[next.lowercase()] ?: 0, frequency)
                }

                override fun onProperCasing(lower: String, proper: String) {
                    properCasingMap[lower.lowercase()] = proper
                    allDictionaryWords.add(lower.lowercase())
                    insert(lower.lowercase(), 20000)
                    wordFrequencies[lower.lowercase()] = max(wordFrequencies[lower.lowercase()] ?: 0, 20000)
                    if (lower.length < 25 && lower.lowercase() !in wordsByLength[lower.length]) {
                        wordsByLength[lower.length].add(lower.lowercase())
                    }
                }

                override fun onEmoji(keyword: String, emoji: String) {
                    emojiMap[keyword.lowercase()] = emoji
                }
            })

            // Ensure baseline common typos and contractions are always intact
            EnglishDictionaryData.commonTypoMap.forEach { (k, v) -> commonTypoMap[k.lowercase()] = v }
            HinglishDictionaryData.hinglishCommonTypoMap.forEach { (k, v) -> commonTypoMap[k.lowercase()] = v }

            // Core candidate shortcuts required by prompt
            val coreShortcuts = mapOf(
                "gm" to listOf("good morning", "Good morning ☕"),
                "gn" to listOf("good night", "Good night 😴"),
                "idk" to listOf("I don't know"),
                "brb" to listOf("be right back"),
                "nhi" to listOf("nahi"),
                "kr" to listOf("kar"),
                "mtlb" to listOf("matlab"),
                "hbd" to listOf("happy birthday", "Happy Birthday! 🎂"),
                "tc" to listOf("take care", "Take Care! 🫂"),
                "tysm" to listOf("thank you so much", "Thank you so much! 🙏"),
                "tbh" to listOf("to be honest"),
                "asap" to listOf("as soon as possible", "ASAP"),
                "fyi" to listOf("for your information", "FYI"),
                "lmk" to listOf("let me know"),
                "wrt" to listOf("with respect to"),
                "eod" to listOf("end of day"),
                "m" to listOf("main"),
                "b" to listOf("bhi", "be"),
                "k" to listOf("kya", "ok"),
                "r" to listOf("are"),
                "u" to listOf("you"),
                "ur" to listOf("your")
            )
            coreShortcuts.forEach { (k, list) ->
                val existing = shortcutsMap.getOrPut(k) { mutableListOf() }
                list.forEach { exp -> if (exp !in existing) existing.add(exp) }
            }

            // Key Hinglish variants
            val keyVariants = mapOf(
                "accha" to listOf("achha", "acha"),
                "nahi" to listOf("nahin", "nhi", "nh"),
                "bohot" to listOf("bahut", "bhot", "boht"),
                "theek" to listOf("thik", "thk", "thek"),
                "karna" to listOf("krna"),
                "karo" to listOf("kro"),
                "raha" to listOf("rha"),
                "rahi" to listOf("rhi"),
                "rahe" to listOf("rhe"),
                "batao" to listOf("btao"),
                "mujhe" to listOf("mko", "mjhe"),
                "tujhe" to listOf("tko", "tjhe"),
                "aapko" to listOf("apko"),
                "main" to listOf("mai", "m")
            )
            keyVariants.forEach { (canon, vars) ->
                val list = canonicalToVariantsMap.getOrPut(canon) { mutableListOf() }
                vars.forEach { v ->
                    if (v !in list) list.add(v)
                    variantToCanonicalMap[v] = canon
                    allDictionaryWords.add(v)
                    allDictionaryWords.add(canon)
                    insert(v, 22000)
                    insert(canon, 24000)
                }
            }

            // Sort words in each length bucket by frequency descending for optimized candidate search
            for (len in 1 until wordsByLength.size) {
                wordsByLength[len].sortByDescending { wordFrequencies[it] ?: 0 }
            }

            // 3. Load user-learned words from local disk (privacy-safe, offline)
            if (context != null) {
                loadUserVocabulary(context)
            }

            initialized = true
        }
    }

    private fun loadUserVocabulary(context: Context) {
        try {
            val file = File(context.filesDir, "user_dict.txt")
            if (file.exists()) {
                file.readLines().forEach { line ->
                    val parts = line.split(":")
                    if (parts.size == 2) {
                        val word = parts[0].trim().lowercase()
                        val freq = parts[1].toIntOrNull() ?: 50
                        if (word.isNotEmpty()) {
                            userWords[word] = freq
                            val boostedFreq = freq + 15000
                            insert(word, boostedFreq)
                            allDictionaryWords.add(word)
                            wordFrequencies[word] = boostedFreq
                            if (word.length < 25 && word !in wordsByLength[word.length]) {
                                wordsByLength[word.length].add(word)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val file = File(context.filesDir, "user_bigrams.txt")
            if (file.exists()) {
                file.readLines().forEach { line ->
                    val parts = line.split("->")
                    if (parts.size == 2) {
                        val prev = parts[0].trim().lowercase()
                        val nextParts = parts[1].split(":")
                        if (nextParts.size == 2) {
                            val next = nextParts[0].trim().lowercase()
                            val freq = nextParts[1].toIntOrNull() ?: 10
                            bigramMap.getOrPut(prev) { mutableMapOf() }[next] = freq
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val file = File(context.filesDir, "user_trigrams.txt")
            if (file.exists()) {
                file.readLines().forEach { line ->
                    val parts = line.split("->")
                    if (parts.size == 2) {
                        val key = parts[0].trim().lowercase()
                        val nextParts = parts[1].split(":")
                        if (nextParts.size == 2) {
                            val next = nextParts[0].trim().lowercase()
                            val freq = nextParts[1].toIntOrNull() ?: 15
                            trigramMap.getOrPut(key) { mutableMapOf() }[next] = freq
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears all user-learned unigrams, bigrams, and trigrams from disk and memory.
     */
    fun clearLearnedVocabulary(context: Context? = null) {
        synchronized(this) {
            userWords.clear()
            userWhitelistedWords.clear()
            cachedTopUserWords = null
        }
        if (context != null) {
            ioScope.launch {
                try {
                    File(context.filesDir, "user_dict.txt").delete()
                    File(context.filesDir, "user_bigrams.txt").delete()
                    File(context.filesDir, "user_trigrams.txt").delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun insert(word: String, frequency: Int) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
            updateTopWords(node, word, frequency)
        }
        node.isWord = true
        node.frequency = max(node.frequency, frequency)
        node.word = word
        updateTopWords(node, word, frequency)
    }

    private fun updateTopWords(node: TrieNode, word: String, frequency: Int) {
        val list = node.topWords
        val existingIndex = list.indexOfFirst { it.first == word }
        if (existingIndex >= 0) {
            if (frequency > list[existingIndex].second) {
                list[existingIndex] = Pair(word, frequency)
                list.sortByDescending { it.second }
            }
        } else {
            if (list.size < 12 || frequency > list.last().second) {
                list.add(Pair(word, frequency))
                list.sortByDescending { it.second }
                if (list.size > 12) {
                    list.removeAt(list.size - 1)
                }
            }
        }
    }

    /**
     * Learns a user word dynamically and boosts its frequency rank.
     */
    fun learnWord(context: Context? = null, word: String) {
        if (!isLearningEnabled) return
        if (word.isBlank() || word.length < 2) return
        val normalized = word.trim().lowercase()
        if (!normalized.all { it.isLetter() || it == '\'' }) return

        initialize(context)

        // Mark in whitelist so it is never auto-corrected away
        userWhitelistedWords.add(normalized)

        val wordsSnapshot: Map<String, Int>
        synchronized(this) {
            val currentFreq = userWords[normalized] ?: 0
            val newFreq = currentFreq + 100
            userWords[normalized] = newFreq
            cachedTopUserWords = null
            val boostedFreq = newFreq + 15000
            insert(normalized, boostedFreq)
            wordFrequencies[normalized] = boostedFreq
            if (allDictionaryWords.add(normalized) && normalized.length < 25) {
                wordsByLength[normalized.length].add(normalized)
            }
            wordsSnapshot = userWords.toMap()
        }

        if (context != null) {
            ioScope.launch {
                try {
                    val file = File(context.filesDir, "user_dict.txt")
                    file.printWriter().use { writer ->
                        wordsSnapshot.forEach { (w, f) ->
                            writer.println("$w:$f")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Learns a sequence of 2 words (Bigram typing pattern) to predict next words dynamically.
     */
    fun learnWordSequence(context: Context? = null, prevWord: String, nextWord: String) {
        if (!isLearningEnabled) return
        if (prevWord.isBlank() || nextWord.isBlank()) return
        val prev = prevWord.trim().lowercase()
        val next = nextWord.trim().lowercase()

        if (!prev.all { it.isLetter() || it == '\'' } || !next.all { it.isLetter() || it == '\'' }) return

        initialize(context)

        val bigramsSnapshot: Map<String, Map<String, Int>>
        synchronized(this) {
            val nextMap = bigramMap.getOrPut(prev) { mutableMapOf() }
            val currentFreq = nextMap[next] ?: 0
            nextMap[next] = currentFreq + 25
            bigramsSnapshot = bigramMap.mapValues { it.value.toMap() }
        }

        if (context != null) {
            ioScope.launch {
                try {
                    val file = File(context.filesDir, "user_bigrams.txt")
                    file.printWriter().use { writer ->
                        bigramsSnapshot.forEach { (p, map) ->
                            map.forEach { (n, f) ->
                                writer.println("$p->$n:$f")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Learns a sequence of 3 words (Trigram typing pattern) for deep contextual prediction.
     */
    fun learnTrigram(context: Context? = null, prevPrevWord: String, prevWord: String, nextWord: String) {
        if (!isLearningEnabled) return
        if (prevPrevWord.isBlank() || prevWord.isBlank() || nextWord.isBlank()) return
        val p1 = prevPrevWord.trim().lowercase()
        val p2 = prevWord.trim().lowercase()
        val next = nextWord.trim().lowercase()

        if (!p1.all { it.isLetter() || it == '\'' } ||
            !p2.all { it.isLetter() || it == '\'' } ||
            !next.all { it.isLetter() || it == '\'' }) return

        initialize(context)

        val key = "$p1 $p2"
        val trigramsSnapshot: Map<String, Map<String, Int>>
        synchronized(this) {
            val nextMap = trigramMap.getOrPut(key) { mutableMapOf() }
            val currentFreq = nextMap[next] ?: 0
            nextMap[next] = currentFreq + 30
            trigramsSnapshot = trigramMap.mapValues { it.value.toMap() }
        }

        if (context != null) {
            ioScope.launch {
                try {
                    val file = File(context.filesDir, "user_trigrams.txt")
                    file.printWriter().use { writer ->
                        trigramsSnapshot.forEach { (k, map) ->
                            map.forEach { (n, f) ->
                                writer.println("$k->$n:$f")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Resolves words with repeated elongated characters (e.g., "helllo" -> "hello", "pleeease" -> "please", "soooo" -> "so").
     */
    fun resolveRepeatedCharacters(word: String): String? {
        if (word.length < 3) return null
        var hasRun = false
        for (i in 0 until word.length - 2) {
            if (word[i] == word[i + 1] && word[i] == word[i + 2]) {
                hasRun = true
                break
            }
        }
        if (!hasRun) return null

        val regex3plus = Regex("(.)\\1{2,}")
        val collapsed2 = word.replace(regex3plus, "$1$1")
        val lower2 = collapsed2.lowercase()

        val regex2plus = Regex("(.)\\1+")
        val collapsed1 = word.replace(regex2plus, "$1")
        val lower1 = collapsed1.lowercase()

        val isWord1 = allDictionaryWords.contains(lower1) || commonTypoMap.containsKey(lower1)
        val isWord2 = allDictionaryWords.contains(lower2) || commonTypoMap.containsKey(lower2)

        if (isWord1 && isWord2) {
            val freq1 = wordFrequencies[lower1] ?: (if (commonTypoMap.containsKey(lower1)) 20000 else 0)
            val freq2 = wordFrequencies[lower2] ?: (if (commonTypoMap.containsKey(lower2)) 20000 else 0)
            val chosen = if (freq1 >= freq2) lower1 else lower2
            val target = commonTypoMap[chosen] ?: chosen
            return applyCasing(word, target)
        } else if (isWord1) {
            val target = commonTypoMap[lower1] ?: lower1
            return applyCasing(word, target)
        } else if (isWord2) {
            val target = commonTypoMap[lower2] ?: lower2
            return applyCasing(word, target)
        }

        return null
    }

    /**
     * Preserves user casing style, capitalized proper nouns, acronyms, and contractions.
     */
    fun applyCasing(source: String, target: String): String {
        if (source.isEmpty() || target.isEmpty()) return target

        // Special standalone pronoun "I"
        if (target.equals("i", ignoreCase = true)) {
            return "I"
        }

        // Special contractions starting with I: "I'm", "I've", "I'll", "I'd"
        if (target.startsWith("i'", ignoreCase = true) || target.equals("i'm", ignoreCase = true) ||
            target.equals("i've", ignoreCase = true) || target.equals("i'll", ignoreCase = true) ||
            target.equals("i'd", ignoreCase = true)) {
            return target.replaceFirstChar { it.uppercase() }
        }

        // All uppercase input: "HELLO" -> "WORLD", "ASAP" -> "ASAP"
        if (source.length >= 2 && source.all { it.isUpperCase() || !it.isLetter() }) {
            return target.uppercase()
        }

        // Canonical proper casing if available (e.g. "delhi" -> "Delhi", "github" -> "GitHub", "asap" -> "ASAP")
        properCasingMap[target.lowercase()]?.let { proper ->
            if (source.first().isUpperCase()) {
                return proper.replaceFirstChar { it.uppercase() }
            }
            return proper
        }

        // Title case: "Hel" -> "Hello"
        if (source.first().isUpperCase()) {
            return target.replaceFirstChar { it.uppercase() }
        }

        return target
    }

    /**
     * Returns next-word predictions based on previous word context (Trigram & Bigram language model)
     * supplemented by conversational starters and contextual emojis.
     */
    fun getNextWordPredictions(prevWord: String, prevPrevWord: String = "", limit: Int = 3): List<String> {
        if (!initialized) initialize()
        if (!isNextWordPredictionEnabled) return emptyList()

        val lowerPrev = prevWord.trim().lowercase()
        val lowerPrevPrev = prevPrevWord.trim().lowercase()
        val results = mutableListOf<String>()

        if (lowerPrev.isEmpty()) {
            return defaultStarters.take(limit)
        }

        // 1. Contextual Trigram lookup ("$prevPrev $prev" -> nextWord)
        if (lowerPrevPrev.isNotEmpty()) {
            val trigramKey = "$lowerPrevPrev $lowerPrev"
            val trigramCandidates = synchronized(this) {
                trigramMap[trigramKey]?.toMap()
            }
            if (trigramCandidates != null) {
                val sorted = trigramCandidates.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                for (word in sorted) {
                    if (word !in results) {
                        results.add(word)
                        if (results.size >= limit) break
                    }
                }
            }
        }

        // 2. Contextual Bigram lookup (prevWord -> nextWord)
        if (results.size < limit) {
            val nextMap = synchronized(this) {
                bigramMap[lowerPrev]?.toMap()
            }
            if (nextMap != null) {
                val sortedBigrams = nextMap.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                for (word in sortedBigrams) {
                    if (word !in results) {
                        results.add(word)
                        if (results.size >= limit) break
                    }
                }
            }
        }

        // 3. Inline contextual emoji match (e.g., "chai" -> ☕, "deal" -> 🤝)
        if (results.size < limit) {
            val emojiMatch = emojiMap[lowerPrev]
            if (emojiMatch != null && emojiMatch !in results) {
                results.add(emojiMatch)
            }
        }

        // 4. User top frequent words
        if (results.size < limit && isLearningEnabled) {
            val topUserWords = synchronized(this) {
                cachedTopUserWords ?: userWords.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                    .also { cachedTopUserWords = it }
            }
            for (word in topUserWords) {
                if (word !in results && word != lowerPrev) {
                    results.add(word)
                    if (results.size >= limit) break
                }
            }
        }

        // 5. Conversational fallbacks (English + Hinglish)
        if (results.size < limit) {
            val fallbacks = listOf("I", "the", "to", "you", "kya", "hai", "bhai", "and", "is", "for", "in", "it", "that", "theek", "please")
            for (word in fallbacks) {
                if (word !in results && !word.equals(lowerPrev, ignoreCase = true)) {
                    results.add(word)
                    if (results.size >= limit) break
                }
            }
        }

        return results.take(limit).map { word ->
            if (properCasingMap.containsKey(word.lowercase())) {
                properCasingMap[word.lowercase()]!!
            } else if (word.equals("i", ignoreCase = true)) {
                "I"
            } else if (word.startsWith("i'", ignoreCase = true) || word.equals("i'm", ignoreCase = true) ||
                word.equals("i've", ignoreCase = true) || word.equals("i'll", ignoreCase = true) ||
                word.equals("i'd", ignoreCase = true)) {
                word.replaceFirstChar { it.uppercase() }
            } else {
                word
            }
        }
    }

    /**
     * Returns ranked autocomplete & auto-correct suggestions for the current prefix.
     * Integrates:
     * 1. Shortcut expansions ("gm" -> "good morning", "idk" -> "I don't know")
     * 2. Direct typo & contraction map ("dont" -> "don't", "im" -> "I'm")
     * 3. Hinglish variant recognition & code-switching ("kr" -> "kar", "nhi" -> "nahi", "acha" -> "accha")
     * 4. Trigram & Bigram context-aware prefix completion
     * 5. High-speed Trie unigram frequency matching
     * 6. Repeated character normalization ("helllo" -> "hello", "bhaii" -> "bhai")
     * 7. Spatial QWERTY proximity & Damerau-Levenshtein fuzzy matching
     * 8. Contextual inline emoji matches
     */
    fun getSuggestions(
        prefix: String,
        prevWord: String = "",
        prevPrevWord: String = "",
        limit: Int = 3
    ): List<String> {
        if (!initialized) initialize()
        if (prefix.isBlank()) return emptyList()

        val lowerPrefix = prefix.lowercase().trim()
        val lowerPrev = prevWord.lowercase().trim()
        val lowerPrevPrev = prevPrevWord.lowercase().trim()
        val scoredCandidates = mutableMapOf<String, Float>()

        // 1. Shortcut expansions (multi-letter shortcuts get priority; single-letter shortcuts get moderate boost so context bigram wins)
        shortcutsMap[lowerPrefix]?.let { expansions ->
            var bonus = if (lowerPrefix.length >= 2) 130000f else 40000f
            expansions.forEach { exp ->
                scoredCandidates[exp] = bonus
                bonus -= 10000f
            }
        }

        // 2. Direct typo & contraction check (Always applied so "dont" -> "don't", "cant" -> "can't", "im" -> "I'm")
        commonTypoMap[lowerPrefix]?.let { typoCorrection ->
            val typoScore = if (lowerPrefix.length >= 2) 110000f else 50000f
            scoredCandidates[typoCorrection.lowercase()] = max(scoredCandidates[typoCorrection.lowercase()] ?: 0f, typoScore)
        }

        // 3. Hinglish variant matching (e.g. "acha" -> "accha", "nhi" -> "nahi", "kr" -> "kar")
        if (isHinglishEnabled) {
            variantToCanonicalMap[lowerPrefix]?.let { canonical ->
                if (!scoredCandidates.containsKey(canonical)) {
                    scoredCandidates[canonical] = 99000f
                }
            }
            canonicalToVariantsMap[lowerPrefix]?.let { variants ->
                var vBonus = 95000f
                variants.forEach { v ->
                    if (v !in scoredCandidates) {
                        scoredCandidates[v] = vBonus
                        vBonus -= 5000f
                    }
                }
            }
        }

        // 4. High-confidence auto-correction sync (guarantees center chip matches space autocorrect)
        val autoCorrection = getAutoCorrection(lowerPrefix, lowerPrev, lowerPrevPrev)
        if (autoCorrection != null && !autoCorrection.equals(lowerPrefix, ignoreCase = true)) {
            scoredCandidates[autoCorrection.lowercase()] = max(scoredCandidates[autoCorrection.lowercase()] ?: 0f, 98000f)
        }

        // 5. Contraction homographs (provide both options: "were" / "we're", "well" / "we'll")
        when (lowerPrefix) {
            "cant" -> {
                scoredCandidates["can't"] = 120000f
                scoredCandidates["cant"] = 40000f
            }
            "dont" -> {
                scoredCandidates["don't"] = 120000f
                scoredCandidates["dont"] = 40000f
            }
            "were" -> {
                scoredCandidates["were"] = 40000f
                scoredCandidates["we're"] = 35000f
            }
            "well" -> {
                scoredCandidates["well"] = 40000f
                scoredCandidates["we'll"] = 35000f
            }
            "its" -> {
                scoredCandidates["its"] = 40000f
                scoredCandidates["it's"] = 35000f
            }
            "ill" -> {
                scoredCandidates["I'll"] = 40000f
                scoredCandidates["ill"] = 35000f
            }
            "id" -> {
                scoredCandidates["ID"] = 40000f
                scoredCandidates["I'd"] = 35000f
            }
        }

        // 6. Context-Aware Trigram Match: "$prevPrev $prev"
        if (lowerPrevPrev.isNotEmpty() && lowerPrev.isNotEmpty()) {
            val trigramKey = "$lowerPrevPrev $lowerPrev"
            val contextTrigrams = synchronized(this) {
                trigramMap[trigramKey]?.toMap()
            }
            if (contextTrigrams != null) {
                contextTrigrams.forEach { (candidate, freq) ->
                    if (candidate.startsWith(lowerPrefix)) {
                        val boost = freq * 450f + 95000f
                        scoredCandidates[candidate] = scoredCandidates.getOrDefault(candidate, 0f) + boost
                    }
                }
            }
        }

        // 7. Context-Aware Bigram Match: prevWord (Strong boost so "good" + "m" gives "morning" as #1)
        if (lowerPrev.isNotEmpty()) {
            val contextBigrams = synchronized(this) {
                bigramMap[lowerPrev]?.toMap()
            }
            if (contextBigrams != null) {
                contextBigrams.forEach { (candidate, freq) ->
                    if (candidate.startsWith(lowerPrefix)) {
                        val boost = freq * 300f + 60000f
                        scoredCandidates[candidate] = scoredCandidates.getOrDefault(candidate, 0f) + boost
                    }
                }
            }
        }

        // 8. Exact match in dictionary gets strong baseline score
        if (allDictionaryWords.contains(lowerPrefix) && (lowerPrefix.length >= 2 || lowerPrefix == "a" || lowerPrefix == "i")) {
            scoredCandidates[lowerPrefix] = scoredCandidates.getOrDefault(lowerPrefix, 0f) + 35000f
        }

        // 9. Trie Prefix Search (Pre-indexed top candidates & Unigram Frequency Ranking)
        var node: TrieNode? = root
        for (char in lowerPrefix) {
            node = node?.children?.get(char)
            if (node == null) break
        }

        if (node != null) {
            // 9a. Pre-indexed top completions
            node.topWords.forEach { (matchedWord, freq) ->
                val lenDiff = matchedWord.length - lowerPrefix.length
                val lenPenalty = lenDiff * 15f
                val score = freq.toFloat() - lenPenalty + 5000f
                scoredCandidates[matchedWord] = scoredCandidates.getOrDefault(matchedWord, 0f) + score
            }

            // 9b. Supplemental DFS collection if fewer than 6 candidates
            if (node.topWords.size < 6) {
                val prefixMatches = mutableListOf<Pair<String, Int>>()
                collectWords(node, StringBuilder(lowerPrefix), prefixMatches, maxResults = 30)
                prefixMatches.forEach { (matchedWord, freq) ->
                    val lenDiff = matchedWord.length - lowerPrefix.length
                    val lenPenalty = lenDiff * 15f
                    val score = freq.toFloat() - lenPenalty + 5000f
                    scoredCandidates[matchedWord] = scoredCandidates.getOrDefault(matchedWord, 0f) + score
                }
            }
        }

        // 10. Repeated character normalization (e.g., "helllo" -> "hello", "bhaii" -> "bhai")
        resolveRepeatedCharacters(lowerPrefix)?.let { normalized ->
            val normLower = normalized.lowercase()
            scoredCandidates[normLower] = scoredCandidates.getOrDefault(normLower, 0f) + 90000f
        }

        // 11. Spatial QWERTY Proximity & Fuzzy Typo Search (if not enough high-confidence candidates)
        if (scoredCandidates.size < 12 && lowerPrefix.length >= 2) {
            val prefixLen = lowerPrefix.length
            val minLen = (prefixLen - 1).coerceAtLeast(1)
            val maxLen = (prefixLen + 1).coerceAtMost(24)
            val firstChar = lowerPrefix[0]

            var testedCount = 0
            for (len in minLen..maxLen) {
                if (testedCount >= 100) break
                val bucket = wordsByLength[len]
                for (i in 0 until bucket.size) {
                    val candidate = bucket[i]
                    if (candidate.startsWith(firstChar) ||
                        KeyboardProximity.getSubstitutionCost(candidate[0], firstChar) <= 0.55f ||
                        candidate.length == prefixLen
                    ) {
                        val distance = KeyboardProximity.calculateWeightedDistance(lowerPrefix, candidate)
                        if (distance <= 1.8f) {
                            val freq = wordFrequencies[candidate] ?: 0
                            val score = 2500f - (distance * 2000f) + (ln(1.0 + freq).toFloat() * 50f)
                            val existing = scoredCandidates[candidate] ?: 0f
                            if (score > existing) {
                                scoredCandidates[candidate] = score
                            }
                        }
                        testedCount++
                        if (testedCount >= 100) break
                    }
                }
            }
        }

        // 12. Contextual Inline Emoji match (e.g., typing "chai" suggests "☕")
        val matchingEmoji = emojiMap[lowerPrefix]

        // Rank all candidates by score descending
        val sortedList = scoredCandidates.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .toMutableList()

        val results = mutableListOf<String>()
        for (cand in sortedList) {
            if (cand !in results) {
                results.add(cand)
                if (results.size >= limit + 1) break
            }
        }

        // Append emoji if available
        if (matchingEmoji != null && matchingEmoji !in results) {
            if (results.size >= limit) {
                results[limit - 1] = matchingEmoji
            } else {
                results.add(matchingEmoji)
            }
        }

        return results.take(limit).map { applyCasing(prefix, it) }
    }

    /**
     * Determines whether to auto-correct the typed word when SPACE is pressed.
     * Returns the target correction string if high confidence, or null if no correction.
     *
     * In accordance with requirements:
     * - Valid English and Hinglish words are never corrupted.
     * - Preserves user whitelisted words (reverted by backspace).
     * - Normalizes casual elongated chat spelling (e.g., "bhaii" -> "bhai", "helllo" -> "hello").
     * - Does not aggressively replace ambiguous informal short spellings (suggests instead).
     */
    fun getAutoCorrection(
        word: String,
        prevWord: String = "",
        prevPrevWord: String = ""
    ): String? {
        if (!initialized) initialize()
        if (word.isBlank() || word.length < 2) {
            // Special single-letter auto-correction for "i" -> "I"
            if (word.equals("i", ignoreCase = false)) return "I"
            return null
        }

        val lower = word.lowercase().trim()

        // 0. Check if word was explicitly whitelisted by user via backspace undo
        if (userWhitelistedWords.contains(lower)) {
            return null
        }

        // 1. Repeated character run normalization (e.g., "helllo" -> "hello", "pleeease" -> "please", "soooo" -> "so")
        resolveRepeatedCharacters(word)?.let { return it }

        // 2. Direct typo / contraction check (e.g., "dont" -> "don't", "teh" -> "the", "alot" -> "a lot")
        // Protect homograph words from accidental typo map override
        val homographContractions = setOf("were", "well", "its", "hell", "shed")
        if (!homographContractions.contains(lower)) {
            commonTypoMap[lower]?.let {
                return applyCasing(word, it)
            }
        }

        // 3. Proper casing canonical form (e.g., "delhi" -> "Delhi", "github" -> "GitHub", "asap" -> "ASAP", "diwali" -> "Diwali")
        properCasingMap[lower]?.let { proper ->
            if (word != proper) {
                return proper
            }
        }

        // 4. Trailing double-letter casual chat normalization (e.g., "bhaii" -> "bhai", "yaarr" -> "yaar", "chaloo" -> "chalo", "sunoo" -> "suno", "boloo" -> "bolo")
        if (lower.length >= 4 && lower[lower.length - 1] == lower[lower.length - 2]) {
            val trimmed = lower.dropLast(1)
            val doubleChar = lower.last()
            val isEnglishDoubleConsonant = doubleChar in setOf('l', 's', 'f', 'z')
            if (allDictionaryWords.contains(trimmed) && trimmed != lower) {
                if (!isEnglishDoubleConsonant || variantToCanonicalMap[lower] == trimmed || !allDictionaryWords.contains(lower)) {
                    val freqTrimmed = wordFrequencies[trimmed] ?: 0
                    val freqLower = wordFrequencies[lower] ?: 0
                    if (freqTrimmed >= freqLower || variantToCanonicalMap[lower] == trimmed || !allDictionaryWords.contains(lower)) {
                        return applyCasing(word, trimmed)
                    }
                }
            }
        }

        // 5. If word is already a valid dictionary word or recognized variant:
        if (allDictionaryWords.contains(lower)) {
            // Real English & Hinglish words (e.g. "were", "well", "its", "kya", "hai", "nahi", "tum", "mera", "chalo") are never altered
            return null
        }

        // 6. Spatial QWERTY Proximity & Context-Boosted Typo Search for misspelled words
        val prefixLen = lower.length
        val minLen = (prefixLen - 1).coerceAtLeast(1)
        val maxLen = (prefixLen + 1).coerceAtMost(24)
        val firstChar = lower[0]
        val lowerPrev = prevWord.lowercase().trim()
        val trigramKey = if (prevPrevWord.isNotBlank() && lowerPrev.isNotEmpty()) {
            "${prevPrevWord.lowercase().trim()} $lowerPrev"
        } else ""

        var bestCandidate: String? = null
        var bestScore = Float.MIN_VALUE
        var minDistance = Float.MAX_VALUE

        for (len in minLen..maxLen) {
            val bucket = wordsByLength[len]
            var testedInBucket = 0
            for (i in 0 until bucket.size) {
                if (testedInBucket >= 100) break
                val candidate = bucket[i]
                val cFirst = candidate[0]
                val firstCost = KeyboardProximity.getSubstitutionCost(firstChar, cFirst)

                if (firstChar == cFirst || firstCost <= 0.55f || candidate.length == prefixLen) {
                    val dist = KeyboardProximity.calculateWeightedDistance(lower, candidate)
                    if (dist <= 1.4f) {
                        val freq = wordFrequencies[candidate] ?: 0
                        var score = (2.0f - dist) * 1000f + (ln(1.0 + freq).toFloat() * 60f)

                        // N-Gram Contextual Confidence Boost
                        if (trigramKey.isNotEmpty() && trigramMap[trigramKey]?.containsKey(candidate) == true) {
                            score += 800f
                        } else if (lowerPrev.isNotEmpty() && bigramMap[lowerPrev]?.containsKey(candidate) == true) {
                            score += 450f
                        }

                        if (score > bestScore) {
                            bestScore = score
                            bestCandidate = candidate
                            minDistance = dist
                        }
                    }
                    testedInBucket++
                }
            }
        }

        if (bestCandidate != null) {
            val hasContextBoost = (trigramKey.isNotEmpty() && trigramMap[trigramKey]?.containsKey(bestCandidate) == true) ||
                (lowerPrev.isNotEmpty() && bigramMap[lowerPrev]?.containsKey(bestCandidate) == true)

            if (minDistance <= 1.05f || (hasContextBoost && minDistance <= 1.35f)) {
                return applyCasing(word, bestCandidate)
            }
        }

        return null
    }

    private fun collectWords(
        node: TrieNode,
        current: StringBuilder,
        results: MutableList<Pair<String, Int>>,
        maxResults: Int = 40
    ) {
        if (results.size >= maxResults) return
        if (node.isWord && node.word != null) {
            results.add(node.word!! to node.frequency)
        }
        for ((char, child) in node.children) {
            if (results.size >= maxResults) return
            current.append(char)
            collectWords(child, current, results, maxResults)
            current.deleteCharAt(current.length - 1)
        }
    }
}
