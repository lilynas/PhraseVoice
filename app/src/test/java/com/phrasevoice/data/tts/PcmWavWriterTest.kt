package com.phrasevoice.data.tts

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmWavWriterTest {
    @Test
    fun toWavBytes_wrapsPcmWithWavHeader() {
        val pcm = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val wav = PcmWavWriter.toWavBytes(pcm)

        assertEquals(44 + pcm.size, wav.size)
        assertAscii("RIFF", wav, 0)
        assertEquals(36 + pcm.size, wav.intLeAt(4))
        assertAscii("WAVE", wav, 8)
        assertAscii("fmt ", wav, 12)
        assertEquals(16, wav.intLeAt(16))
        assertEquals(1, wav.shortLeAt(20))
        assertEquals(1, wav.shortLeAt(22))
        assertEquals(24_000, wav.intLeAt(24))
        assertEquals(48_000, wav.intLeAt(28))
        assertEquals(2, wav.shortLeAt(32))
        assertEquals(16, wav.shortLeAt(34))
        assertAscii("data", wav, 36)
        assertEquals(pcm.size, wav.intLeAt(40))
        assertArrayEquals(pcm, wav.copyOfRange(44, wav.size))
    }

    private fun assertAscii(expected: String, bytes: ByteArray, offset: Int) {
        assertEquals(expected, String(bytes, offset, expected.length, Charsets.US_ASCII))
    }

    private fun ByteArray.intLeAt(offset: Int): Int =
        (this[offset].toInt() and 0xff) or
            (this[offset + 1].toInt() and 0xff shl 8) or
            (this[offset + 2].toInt() and 0xff shl 16) or
            (this[offset + 3].toInt() and 0xff shl 24)

    private fun ByteArray.shortLeAt(offset: Int): Int =
        (this[offset].toInt() and 0xff) or
            (this[offset + 1].toInt() and 0xff shl 8)
}
