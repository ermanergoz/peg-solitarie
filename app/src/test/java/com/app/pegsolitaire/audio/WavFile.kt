package com.app.pegsolitaire.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/** The `fmt ` chunk's four fields this project's audio rule constrains. */
internal data class WavFormat(
    val audioFormat: Int,
    val channels: Int,
    val sampleRate: Int,
    val bitsPerSample: Int,
)

/**
 * A RIFF/WAVE file read far enough to check rules/audio.md: its format, and the largest
 * sample magnitude in its `data` chunk.
 *
 * The reader **walks the chunks** instead of assuming a 44-byte header: every chunk is a
 * four-byte id, a little-endian 32-bit size and a body padded to an even length, and `fmt ` and
 * `data` may sit anywhere among them (ffmpeg puts a `LIST` chunk between the two unless told to
 * be bitexact).
 *
 * Test-source-set only.
 */
internal class WavFile(
    val format: WavFormat,
    /** The largest |sample| in the `data` chunk, for 16-bit PCM; 0 for any other bit depth. */
    val peakMagnitude: Int,
) {
    companion object {
        private const val RIFF_ID = "RIFF"
        private const val WAVE_ID = "WAVE"
        private const val FMT_ID = "fmt "
        private const val DATA_ID = "data"
        private const val ID_BYTES = 4
        private const val FIRST_CHUNK_OFFSET = 12
        private const val WAVE_ID_OFFSET = 8
        private const val CHUNK_HEADER_BYTES = 8
        private const val SIXTEEN_BITS = 16

        fun parse(bytes: ByteArray): WavFile {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            check(idAt(bytes, 0) == RIFF_ID && idAt(bytes, WAVE_ID_OFFSET) == WAVE_ID) { "Not a RIFF/WAVE file" }
            var format: WavFormat? = null
            var data: ByteBuffer? = null
            var offset = FIRST_CHUNK_OFFSET
            while (offset + CHUNK_HEADER_BYTES <= bytes.size) {
                val id = idAt(bytes, offset)
                val size = buffer.getInt(offset + ID_BYTES)
                val body = offset + CHUNK_HEADER_BYTES
                check(size >= 0 && body + size <= bytes.size) { "Chunk '$id' runs past the end of the file" }
                when (id) {
                    FMT_ID -> format = formatAt(buffer, body)
                    DATA_ID -> data = buffer.slice(body, size).order(ByteOrder.LITTLE_ENDIAN)
                }
                offset = body + size + size % 2
            }
            val fmt = checkNotNull(format) { "No '$FMT_ID' chunk" }
            val samples = checkNotNull(data) { "No '$DATA_ID' chunk" }
            return WavFile(format = fmt, peakMagnitude = if (fmt.bitsPerSample == SIXTEEN_BITS) peakOf(samples) else 0)
        }

        private fun idAt(
            bytes: ByteArray,
            offset: Int,
        ): String = String(bytes, offset, ID_BYTES, Charsets.US_ASCII)

        private fun formatAt(
            buffer: ByteBuffer,
            body: Int,
        ): WavFormat =
            WavFormat(
                audioFormat = buffer.getShort(body).toInt(),
                channels = buffer.getShort(body + 2).toInt(),
                sampleRate = buffer.getInt(body + 4),
                bitsPerSample = buffer.getShort(body + 14).toInt(),
            )

        private fun peakOf(samples: ByteBuffer): Int {
            var peak = 0
            while (samples.remaining() >= Short.SIZE_BYTES) {
                peak = maxOf(peak, abs(samples.getShort().toInt()))
            }
            return peak
        }
    }
}
