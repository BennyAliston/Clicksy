package com.clicksy.keyboard.data

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Repository for managing Clicksy's English + Hinglish unified vocabulary and provenance metadata.
 */
object ClicksyDictionaryRepository {

    const val ASSET_FILE = "clicksy_dictionary.json"

    var metadata: DictionaryMetadata = DictionaryMetadata()
        private set

    @Volatile
    var isLoaded: Boolean = false
        private set

    /**
     * Loads the unified dictionary from assets or filesystem into the given [consumer].
     */
    fun loadDictionary(context: Context?, consumer: ClicksyDictionaryConsumer): Boolean {
        var inputStream: InputStream? = null
        try {
            if (context != null) {
                try {
                    inputStream = context.assets.open(ASSET_FILE)
                } catch (e: Exception) {
                    // Fallback to filesystem
                }
            }

            if (inputStream == null) {
                // Try relative paths for Gradle JVM unit tests or tooling
                val candidatePaths = listOf(
                    "src/main/assets/$ASSET_FILE",
                    "app/src/main/assets/$ASSET_FILE",
                    "../app/src/main/assets/$ASSET_FILE",
                    "tools/$ASSET_FILE",
                    "../tools/$ASSET_FILE"
                )
                for (path in candidatePaths) {
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        inputStream = FileInputStream(file)
                        break
                    }
                }
            }

            if (inputStream != null) {
                val parser = ClicksyJsonParser()
                parser.parse(inputStream, object : ClicksyDictionaryConsumer {
                    override fun onMetadata(m: DictionaryMetadata) {
                        metadata = m
                        consumer.onMetadata(m)
                    }

                    override fun onShortcut(shortcut: String, expansions: List<String>) {
                        consumer.onShortcut(shortcut, expansions)
                    }

                    override fun onWord(entry: DictionaryWordEntry) {
                        consumer.onWord(entry)
                    }

                    override fun onBigram(prev: String, next: String, frequency: Int) {
                        consumer.onBigram(prev, next, frequency)
                    }

                    override fun onTrigram(prevPrevPrev: String, next: String, frequency: Int) {
                        consumer.onTrigram(prevPrevPrev, next, frequency)
                    }

                    override fun onProperCasing(lower: String, proper: String) {
                        consumer.onProperCasing(lower, proper)
                    }

                    override fun onEmoji(keyword: String, emoji: String) {
                        consumer.onEmoji(keyword, emoji)
                    }
                })
                isLoaded = true
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
        return false
    }
}
