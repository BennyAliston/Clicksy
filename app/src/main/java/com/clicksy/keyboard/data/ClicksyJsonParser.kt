package com.clicksy.keyboard.data

import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

/**
 * High-performance streaming JSON parser for Clicksy's Unified Dictionary.
 *
 * Streams dictionary entries directly to a [ClicksyDictionaryConsumer] without
 * building an intermediate DOM tree, maximizing memory efficiency and minimizing load latency.
 * Works natively on both Android devices and JVM desktop unit tests.
 */
class ClicksyJsonParser {

    fun parse(inputStream: InputStream, consumer: ClicksyDictionaryConsumer) {
        InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
            parse(reader, consumer)
        }
    }

    fun parse(reader: Reader, consumer: ClicksyDictionaryConsumer) {
        val tokenizer = SimpleJsonTokenizer(reader)
        tokenizer.expect('{')

        var version = "1.0.0"
        var generatedAt = ""
        var totalWords = 0
        var englishCount = 0
        var hinglishCount = 0
        var totalShortcuts = 0
        var totalBigrams = 0
        var totalTrigrams = 0

        while (tokenizer.hasNext() && tokenizer.peek() != '}') {
            val key = tokenizer.readString()
            tokenizer.expect(':')

            when (key) {
                "version" -> {
                    version = tokenizer.readString()
                }
                "generatedAt" -> {
                    generatedAt = tokenizer.readString()
                }
                "metadata" -> {
                    tokenizer.expect('{')
                    while (tokenizer.peek() != '}') {
                        val metaKey = tokenizer.readString()
                        tokenizer.expect(':')
                        when (metaKey) {
                            "totalWords" -> totalWords = tokenizer.readInt()
                            "englishWordCount" -> englishCount = tokenizer.readInt()
                            "hinglishWordCount" -> hinglishCount = tokenizer.readInt()
                            "totalShortcuts" -> totalShortcuts = tokenizer.readInt()
                            "totalBigrams" -> totalBigrams = tokenizer.readInt()
                            "totalTrigrams" -> totalTrigrams = tokenizer.readInt()
                            else -> tokenizer.skipValue()
                        }
                        if (tokenizer.peek() == ',') tokenizer.next()
                    }
                    tokenizer.expect('}')
                    consumer.onMetadata(
                        DictionaryMetadata(
                            version = version,
                            generatedAt = generatedAt,
                            totalWords = totalWords,
                            englishWordCount = englishCount,
                            hinglishWordCount = hinglishCount,
                            totalShortcuts = totalShortcuts,
                            totalBigrams = totalBigrams,
                            totalTrigrams = totalTrigrams
                        )
                    )
                }
                "shortcuts" -> {
                    tokenizer.expect('{')
                    while (tokenizer.peek() != '}') {
                        val shortcut = tokenizer.readString()
                        tokenizer.expect(':')
                        val expansions = tokenizer.readStringArray()
                        consumer.onShortcut(shortcut, expansions)
                        if (tokenizer.peek() == ',') tokenizer.next()
                    }
                    tokenizer.expect('}')
                }
                "words" -> {
                    tokenizer.expect('[')
                    while (tokenizer.peek() != ']') {
                        val entry = readWordEntry(tokenizer)
                        if (entry != null) {
                            consumer.onWord(entry)
                        }
                        if (tokenizer.peek() == ',') tokenizer.next()
                    }
                    tokenizer.expect(']')
                }
                "bigrams" -> {
                    tokenizer.expect('{')
                    while (tokenizer.peek() != '}') {
                        val prev = tokenizer.readString()
                        tokenizer.expect(':')
                        tokenizer.expect('{')
                        while (tokenizer.peek() != '}') {
                            val next = tokenizer.readString()
                            tokenizer.expect(':')
                            val freq = tokenizer.readInt()
                            consumer.onBigram(prev, next, freq)
                            if (tokenizer.peek() == ',') tokenizer.next()
                        }
                        tokenizer.expect('}')
                        if (tokenizer.peek() == ',') tokenizer.next()
                    }
                    tokenizer.expect('}')
                }
                "trigrams" -> {
                    tokenizer.expect('{')
                    while (tokenizer.peek() != '}') {
                        val prevPrevPrev = tokenizer.readString()
                        tokenizer.expect(':')
                        tokenizer.expect('{')
                        while (tokenizer.peek() != '}') {
                            val next = tokenizer.readString()
                            tokenizer.expect(':')
                            val freq = tokenizer.readInt()
                            consumer.onTrigram(prevPrevPrev, next, freq)
                            if (tokenizer.peek() == ',') tokenizer.next()
                        }
                        tokenizer.expect('}')
                        if (tokenizer.peek() == ',') tokenizer.next()
                    }
                    tokenizer.expect('}')
                }
                "properCasing" -> {
                    tokenizer.expect('{')
                    while (tokenizer.peek() != '}') {
                        val lower = tokenizer.readString()
                        tokenizer.expect(':')
                        val proper = tokenizer.readString()
                        consumer.onProperCasing(lower, proper)
                        if (tokenizer.peek() == ',') tokenizer.next()
                    }
                    tokenizer.expect('}')
                }
                "emojis" -> {
                    tokenizer.expect('{')
                    while (tokenizer.peek() != '}') {
                        val word = tokenizer.readString()
                        tokenizer.expect(':')
                        val emoji = tokenizer.readString()
                        consumer.onEmoji(word, emoji)
                        if (tokenizer.peek() == ',') tokenizer.next()
                    }
                    tokenizer.expect('}')
                }
                else -> {
                    tokenizer.skipValue()
                }
            }

            if (tokenizer.peek() == ',') {
                tokenizer.next()
            }
        }
        tokenizer.expect('}')
    }

    private fun readWordEntry(tokenizer: SimpleJsonTokenizer): DictionaryWordEntry? {
        tokenizer.expect('{')
        var word = ""
        var language = "English"
        var variants = emptyList<String>()
        var frequency = 1000
        var shortForms = emptyList<String>()
        var corrections = emptyList<String>()
        var source = ""
        var license = ""

        while (tokenizer.peek() != '}') {
            val key = tokenizer.readString()
            tokenizer.expect(':')
            when (key) {
                "word" -> word = tokenizer.readString()
                "language" -> language = tokenizer.readString()
                "variants" -> variants = tokenizer.readStringArray()
                "frequency" -> frequency = tokenizer.readInt()
                "shortForms" -> shortForms = tokenizer.readStringArray()
                "corrections" -> corrections = tokenizer.readStringArray()
                "source" -> source = tokenizer.readString()
                "license" -> license = tokenizer.readString()
                else -> tokenizer.skipValue()
            }
            if (tokenizer.peek() == ',') tokenizer.next()
        }
        tokenizer.expect('}')
        if (word.isEmpty()) return null
        return DictionaryWordEntry(
            word = word,
            language = language,
            variants = variants,
            frequency = frequency,
            shortForms = shortForms,
            corrections = corrections,
            source = source,
            license = license
        )
    }

    private class SimpleJsonTokenizer(private val reader: Reader) {
        private var current: Int = reader.read()

        fun hasNext(): Boolean {
            skipWhitespace()
            return current != -1
        }

        fun peek(): Char {
            skipWhitespace()
            return current.toChar()
        }

        fun next(): Char {
            val c = peek()
            current = reader.read()
            return c
        }

        fun expect(expected: Char) {
            val c = peek()
            if (c != expected) {
                throw IllegalStateException("Expected '$expected' but found '$c' (codepoint: ${c.code})")
            }
            current = reader.read()
        }

        private fun skipWhitespace() {
            while (current != -1 && current.toChar().isWhitespace()) {
                current = reader.read()
            }
        }

        fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (current != -1) {
                val c = current.toChar()
                current = reader.read()
                if (c == '"') {
                    return sb.toString()
                } else if (c == '\\') {
                    if (current == -1) break
                    val esc = current.toChar()
                    current = reader.read()
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hexChars = CharArray(4)
                            for (i in 0 until 4) {
                                hexChars[i] = current.toChar()
                                current = reader.read()
                            }
                            val code = String(hexChars).toInt(16)
                            sb.append(code.toChar())
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        fun readInt(): Int {
            skipWhitespace()
            val sb = StringBuilder()
            if (current.toChar() == '-') {
                sb.append('-')
                current = reader.read()
            }
            while (current != -1 && current.toChar().isDigit()) {
                sb.append(current.toChar())
                current = reader.read()
            }
            // Skip decimal part if present
            if (current.toChar() == '.') {
                current = reader.read()
                while (current != -1 && current.toChar().isDigit()) {
                    current = reader.read()
                }
            }
            return sb.toString().toIntOrNull() ?: 0
        }

        fun readStringArray(): List<String> {
            expect('[')
            val list = mutableListOf<String>()
            while (peek() != ']') {
                list.add(readString())
                if (peek() == ',') next()
            }
            expect(']')
            return list
        }

        fun skipValue() {
            skipWhitespace()
            when (peek()) {
                '"' -> readString()
                '{' -> {
                    expect('{')
                    var depth = 1
                    while (depth > 0 && current != -1) {
                        when (current.toChar()) {
                            '{' -> depth++
                            '}' -> depth--
                            '"' -> readString() // Skip string to avoid braces inside quotes
                        }
                        if (depth > 0) current = reader.read()
                    }
                    if (current != -1) current = reader.read()
                }
                '[' -> {
                    expect('[')
                    var depth = 1
                    while (depth > 0 && current != -1) {
                        when (current.toChar()) {
                            '[' -> depth++
                            ']' -> depth--
                            '"' -> readString()
                        }
                        if (depth > 0) current = reader.read()
                    }
                    if (current != -1) current = reader.read()
                }
                else -> {
                    // Primitive (boolean, number, null)
                    while (current != -1 && current.toChar() != ',' && current.toChar() != '}' && current.toChar() != ']' && !current.toChar().isWhitespace()) {
                        current = reader.read()
                    }
                }
            }
        }
    }
}
