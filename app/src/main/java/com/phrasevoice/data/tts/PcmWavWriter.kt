package com.phrasevoice.data.tts

import java.io.ByteArrayOutputStream
import java.io.File

object PcmWavWriter {
    private const val DEFAULT_SAMPLE_RATE = 24_000
    private const val DEFAULT_CHANNELS = 1
    private const val DEFAULT_BITS_PER_SAMPLE = 16

    fun writeWav(
        file: File,
        pcmBytes: ByteArray,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channels: Int = DEFAULT_CHANNELS,
        bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE,
    ) {
        file.writeBytes(toWavBytes(pcmBytes, sampleRate, channels, bitsPerSample))
    }

    fun toWavBytes(
        pcmBytes: ByteArray,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channels: Int = DEFAULT_CHANNELS,
        bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE,
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmBytes.size

        return ByteArrayOutputStream(WAV_HEADER_SIZE + dataSize).use { output ->
            output.writeAscii("RIFF")
            output.writeIntLe(36 + dataSize)
            output.writeAscii("WAVE")
            output.writeAscii("fmt ")
            output.writeIntLe(16)
            output.writeShortLe(1)
            output.writeShortLe(channels)
            output.writeIntLe(sampleRate)
            output.writeIntLe(byteRate)
            output.writeShortLe(blockAlign)
            output.writeShortLe(bitsPerSample)
            output.writeAscii("data")
            output.writeIntLe(dataSize)
            output.write(pcmBytes)
            output.toByteArray()
        }
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun ByteArrayOutputStream.writeIntLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
        write(value shr 16 and 0xff)
        write(value shr 24 and 0xff)
    }

    private fun ByteArrayOutputStream.writeShortLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
    }

    private const val WAV_HEADER_SIZE = 44
}
