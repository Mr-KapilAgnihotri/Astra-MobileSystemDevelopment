package com.yourname.trackr.data.sensors

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

/**
 * A short synthesized beep for distance milestones - no audio asset needed.
 * ToneGenerator talks to the audio system directly, so this doesn't need a Context.
 */
class MilestoneChime {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)

    private fun play() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, DURATION_MS)
    }

    /**
     * Three short beeps in quick succession - a single beep is easy to miss mid-run.
     * Uses delay(), so this must run in a coroutine scope (e.g. viewModelScope), never
     * called directly from the raw sensor callback thread.
     */
    suspend fun playMilestoneAlert() {
        repeat(BEEP_COUNT) { index ->
            play()
            if (index < BEEP_COUNT - 1) delay(BEEP_GAP_MS)
        }
    }

    fun release() {
        toneGenerator.release()
    }

    companion object {
        private const val VOLUME = 100
        private const val DURATION_MS = 150
        private const val BEEP_COUNT = 3
        private const val BEEP_GAP_MS = 200L
    }
}
