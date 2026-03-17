package com.wiom.csp.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

object AudioFeedback {

    private const val SAMPLE_RATE = 44100

    fun playStandard() {
        playTone(frequency = 440.0, durationMs = 200, waveform = Waveform.SINE)
    }

    fun playUrgent() {
        playTone(frequency = 880.0, durationMs = 400, waveform = Waveform.SQUARE)
    }

    private enum class Waveform { SINE, SQUARE }

    private fun playTone(frequency: Double, durationMs: Int, waveform: Waveform) {
        val numSamples = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * frequency * i / SAMPLE_RATE
            val value = when (waveform) {
                Waveform.SINE -> sin(angle)
                Waveform.SQUARE -> if (sin(angle) >= 0) 1.0 else -1.0
            }
            samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }

        val bufferSize = samples.size * 2 // 2 bytes per short

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufferSize,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()

        // Release after playback completes
        audioTrack.setNotificationMarkerPosition(numSamples)
        audioTrack.setPlaybackPositionUpdateListener(object :
            AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) {
                track.stop()
                track.release()
            }

            override fun onPeriodicNotification(track: AudioTrack) {
                // Not used
            }
        })
    }
}
