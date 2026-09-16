package com.yourname.trackr.ui

import android.view.View

/** One-shot fade-in for cards that appear asynchronously (weather, stats) instead of popping in. */
fun View.fadeIn(durationMs: Long = 300L) {
    alpha = 0f
    visibility = View.VISIBLE
    animate().alpha(1f).setDuration(durationMs).start()
}
