package com.yourname.trackr.ui

import android.view.View

/** One-shot fade-in for cards that appear asynchronously (weather, stats) instead of popping in.
 *  200ms everywhere - the one consistent duration/easing used across every screen. */
fun View.fadeIn(durationMs: Long = 200L) {
    alpha = 0f
    visibility = View.VISIBLE
    animate().alpha(1f).setDuration(durationMs).start()
}
