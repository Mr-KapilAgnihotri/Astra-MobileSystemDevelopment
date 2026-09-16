package com.yourname.trackr.data.sensors

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * A short synthesized beep for distance milestones - no audio asset needed.
 * ToneGenerator talks to the audio system directly, so this doesn't need a Context.
 */
class MilestoneChime {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)

    fun play() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, DURATION_MS)
    }

    fun release() {
        toneGenerator.release()
    }

    companion object {
        private const val VOLUME = 100
        private const val DURATION_MS = 150
    }
}
