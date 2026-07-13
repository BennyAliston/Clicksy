package com.clicksy.keyboard.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.exp

enum class SoundType {
    MUTE,
    SYSTEM,
    BUBBLE,
    WOODBLOCK,
    TYPEWRITER,
    CHIME
}

class SoundFeedbackManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val executor = Executors.newSingleThreadExecutor()
    private val playExecutor = Executors.newCachedThreadPool()

    private val sampleRate = 22050 // Hz

    // SoundPool for ultra-low latency, zero-allocation playback
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA) // Use media stream to bypass touch/system volume restrictions
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var bubbleSoundId = 0
    private var woodblockSoundId = 0
    private var typewriterSoundId = 0
    private var chimeSoundId = 0

    init {
        // Pre-generate PCM buffers on startup, write them to temporary files, and load into SoundPool
        executor.submit {
            try {
                val bubble = generateBubblePop()
                val woodblock = generateWoodblock()
                val typewriter = generateTypewriter()
                val chime = generateChime()

                val cacheDir = context.cacheDir
                val bubbleFile = File(cacheDir, "bubble_temp.wav")
                val woodblockFile = File(cacheDir, "woodblock_temp.wav")
                val typewriterFile = File(cacheDir, "typewriter_temp.wav")
                val chimeFile = File(cacheDir, "chime_temp.wav")

                writePcmToWav(bubble, bubbleFile)
                writePcmToWav(woodblock, woodblockFile)
                writePcmToWav(typewriter, typewriterFile)
                writePcmToWav(chime, chimeFile)

                bubbleSoundId = soundPool.load(bubbleFile.absolutePath, 1)
                woodblockSoundId = soundPool.load(woodblockFile.absolutePath, 1)
                typewriterSoundId = soundPool.load(typewriterFile.absolutePath, 1)
                chimeSoundId = soundPool.load(chimeFile.absolutePath, 1)

                // The temporary files can be deleted after load completes since SoundPool keeps data in memory
                executor.submit {
                    Thread.sleep(5000) // Wait for loader threads to read the files
                    bubbleFile.delete()
                    woodblockFile.delete()
                    typewriterFile.delete()
                    chimeFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playSound(type: SoundType) {
        if (type == SoundType.MUTE) return

        playExecutor.submit {
            try {
                if (type == SoundType.SYSTEM) {
                    audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
                    return@submit
                }

                val soundId = when (type) {
                    SoundType.BUBBLE -> bubbleSoundId
                    SoundType.WOODBLOCK -> woodblockSoundId
                    SoundType.TYPEWRITER -> typewriterSoundId
                    SoundType.CHIME -> chimeSoundId
                    else -> 0
                }

                if (soundId != 0) {
                    soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Helper to write a raw PCM short array as a standard 16-bit Mono WAV file.
     */
    private fun writePcmToWav(pcmData: ShortArray, outputFile: File) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = sampleRate.toLong()
        val channels = 1
        val byteRate = longSampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.toByte() // WAVE
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // fmt 
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // Chunk size (16 for PCM)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Audio format (1 for PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2 // Block align (channels * bitsPerSample / 8)
        header[33] = 0
        header[34] = 16 // Bits per sample
        header[35] = 0
        header[36] = 'd'.toByte() // data
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        outputFile.outputStream().use { out ->
            out.write(header)
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                byteBuffer.putShort(sample)
            }
            out.write(byteBuffer.array())
        }
    }

    // 1. Cute bubble pop sound (fast pitch sweep upwards: 400Hz to 1100Hz)
    private fun generateBubblePop(): ShortArray {
        val duration = 0.08 // seconds
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 400.0 + (1100.0 - 400.0) * (t / duration)
            val envelope = exp(-5.0 * t / duration)
            val sample = sin(2.0 * PI * freq * t) * envelope
            buffer[i] = (sample * 32767).toInt().toShort()
        }
        return buffer
    }

    // 2. Woodblock sound (organic woodblock tap: 700Hz decaying to 300Hz)
    private fun generateWoodblock(): ShortArray {
        val duration = 0.06 // seconds
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 700.0 - (700.0 - 300.0) * (t / duration)
            val envelope = exp(-12.0 * t / duration)
            val sample = sin(2.0 * PI * freq * t) * envelope
            buffer[i] = (sample * 32767).toInt().toShort()
        }
        return buffer
    }

    // 3. Typewriter key clack (a low frequency transient + filtered high frequency noise clack)
    private fun generateTypewriter(): ShortArray {
        val duration = 0.04 // seconds
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        val random = java.util.Random()
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-18.0 * t / duration)
            val click = sin(2.0 * PI * 180.0 * t)
            val noise = random.nextFloat() * 2.0 - 1.0
            val sample = (click * 0.4 + noise * 0.6) * envelope
            buffer[i] = (sample * 32767).coerceIn(-32768.0, 32767.0).toInt().toShort()
        }
        return buffer
    }

    // 4. Cute chime / bell sound (multiple harmonics with slow decay)
    private fun generateChime(): ShortArray {
        val duration = 0.15 // seconds
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-4.0 * t / duration)
            val sample = (sin(2.0 * PI * 1200.0 * t) * 0.7 + sin(2.0 * PI * 1800.0 * t) * 0.3) * envelope
            buffer[i] = (sample * 32767).toInt().toShort()
        }
        return buffer
    }
}
