package com.yourname.trackr.data

import android.content.Context

/** Small SharedPreferences wrapper for the handful of user-level settings the app has so far. */
class UserPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var weightKg: Float
        get() = prefs.getFloat(KEY_WEIGHT_KG, DEFAULT_WEIGHT_KG)
        set(value) = prefs.edit().putFloat(KEY_WEIGHT_KG, value).apply()

    /** Tracks whether the first-run weight dialog has been shown, independent of whether
     *  the user actually entered a value or skipped it - so we don't keep re-prompting. */
    var hasBeenPromptedForWeight: Boolean
        get() = prefs.getBoolean(KEY_PROMPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROMPTED, value).apply()

    companion object {
        private const val PREFS_NAME = "trackr_prefs"
        private const val KEY_WEIGHT_KG = "weight_kg"
        private const val KEY_PROMPTED = "has_prompted_for_weight"
        const val DEFAULT_WEIGHT_KG = 70f
    }
}
