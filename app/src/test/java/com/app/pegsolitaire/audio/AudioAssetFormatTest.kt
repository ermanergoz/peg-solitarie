package com.app.pegsolitaire.audio

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * The rules/audio.md asset rules a committed file can prove on its own: every sound in
 * `res/raw` is a mono 16-bit 44.1 kHz PCM WAV of at most 200 KB that peaks at or under −1 dBFS,
 * and there is exactly one file per [SoundCue] (`docs/tech/audio-silent-mode.md` §2.3, P5).
 *
 * The files are read by walking their RIFF chunks, never by assuming the 44-byte header today's
 * renders happen to have: ffmpeg writes a `LIST` chunk before `data` unless it is told to be
 * bitexact, and the next render must not pass or fail on that accident.
 *
 * The in-game mix peak is not a property of any one file, so it stays in the sound-designer's
 * measurements; the jump's own pairing with its mix level is `SoundParamsTest`'s.
 *
 * Runner-free: a plain scan of the working tree.
 */
class AudioAssetFormatTest {
    private val rawFiles: List<File> = AudioRepoFiles.rawFiles()

    @Test
    fun everyFileInResRawIsAWav() {
        assertThat(rawFiles).isNotEmpty()
        assertThat(rawFiles.filterNot { it.extension == WAV_EXTENSION }.map { it.name }).isEmpty()
    }

    @Test
    fun everySoundIsMonoSixteenBitPcmAt44100Hz() {
        assertThat(rawFiles).isNotEmpty()
        rawFiles.forEach { file ->
            assertWithMessage(file.name).that(WavFile.parse(file.readBytes()).format).isEqualTo(HOUSE_FORMAT)
        }
    }

    @Test
    fun noSoundIsLargerThan200Kilobytes() {
        assertThat(rawFiles).isNotEmpty()
        assertThat(rawFiles.filter { it.length() > MAX_SOUND_BYTES }.map { it.name }).isEmpty()
    }

    @Test
    fun noSoundPeaksAboveMinusOneDbfs() {
        assertThat(rawFiles).isNotEmpty()
        rawFiles.forEach { file ->
            val peak = WavFile.parse(file.readBytes()).peakMagnitude
            assertWithMessage("largest |sample| of ${file.name}").that(peak.toDouble()).isAtMost(PEAK_CEILING)
        }
    }

    @Test
    fun resRawHoldsExactlyOneFilePerCue() {
        assertThat(rawFiles).hasSize(SoundCue.entries.size)
    }

    @Test
    fun theReaderFindsTheSamplesBehindAnOddSizedListChunk() {
        val samples = shortArrayOf(0, SMALL_SAMPLE, LOUDEST_SAMPLE, SMALL_NEGATIVE_SAMPLE)

        val wav = WavFile.parse(wavBytes(samples, listChunkSize = ODD_LIST_CHUNK_SIZE))

        assertThat(wav.format).isEqualTo(HOUSE_FORMAT)
        assertThat(wav.peakMagnitude).isEqualTo(-LOUDEST_SAMPLE)
    }

    /**
     * A RIFF/WAVE file in [HOUSE_FORMAT] carrying [samples], with a `LIST` chunk of
     * [listChunkSize] bytes (plus its pad byte when odd) between `fmt ` and `data`.
     */
    private fun wavBytes(
        samples: ShortArray,
        listChunkSize: Int,
    ): ByteArray {
        val listPadded = listChunkSize + listChunkSize % 2
        val dataSize = samples.size * BYTES_PER_SAMPLE
        val riffSize =
            RIFF_FORM_TYPE_BYTES + CHUNK_HEADER_BYTES + FMT_BODY_BYTES +
                CHUNK_HEADER_BYTES + listPadded + CHUNK_HEADER_BYTES + dataSize
        val buffer = ByteBuffer.allocate(CHUNK_HEADER_BYTES + riffSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(riffSize).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(FMT_BODY_BYTES)
        buffer
            .putShort(HOUSE_FORMAT.audioFormat.toShort())
            .putShort(HOUSE_FORMAT.channels.toShort())
            .putInt(HOUSE_FORMAT.sampleRate)
            .putInt(HOUSE_FORMAT.sampleRate * BYTES_PER_SAMPLE)
            .putShort(BYTES_PER_SAMPLE.toShort())
            .putShort(HOUSE_FORMAT.bitsPerSample.toShort())
        buffer.put("LIST".toByteArray()).putInt(listChunkSize).put(ByteArray(listPadded) { LIST_FILLER })
        buffer.put("data".toByteArray()).putInt(dataSize)
        samples.forEach { buffer.putShort(it) }
        return buffer.array()
    }

    private companion object {
        const val WAV_EXTENSION = "wav"

        /** PCM, mono, 44,100 Hz, 16 bits: the only format rules/audio.md allows a sound effect. */
        val HOUSE_FORMAT = WavFormat(audioFormat = 1, channels = 1, sampleRate = 44_100, bitsPerSample = 16)

        /** rules/audio.md: at most 200 KB per sound effect. */
        const val MAX_SOUND_BYTES = 200 * 1_024L

        /** rules/audio.md: the peak is at most −1 dBFS. */
        const val PEAK_LIMIT_DBFS = -1.0

        /** [PEAK_LIMIT_DBFS] as a 16-bit sample magnitude: about 29,203. */
        val PEAK_CEILING = Short.MAX_VALUE * 10.0.pow(PEAK_LIMIT_DBFS / 20.0)

        const val BYTES_PER_SAMPLE = 2
        const val CHUNK_HEADER_BYTES = 8
        const val RIFF_FORM_TYPE_BYTES = 4
        const val FMT_BODY_BYTES = 16
        const val ODD_LIST_CHUNK_SIZE = 27
        const val LIST_FILLER: Byte = 0x41
        const val SMALL_SAMPLE: Short = 1_200
        const val SMALL_NEGATIVE_SAMPLE: Short = -1_200
        const val LOUDEST_SAMPLE: Short = -30_000
    }
}
