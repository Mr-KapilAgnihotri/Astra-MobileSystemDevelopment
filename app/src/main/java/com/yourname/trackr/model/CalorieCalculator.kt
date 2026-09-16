package com.yourname.trackr.model

/**
 * Rough calorie estimate using the standard MET formula: calories = MET * weightKg * hours.
 * MET is a fixed constant per activity type (not adjusted for pace/incline), so this is an
 * at-a-glance estimate, not a fitness-grade calculation.
 */
object CalorieCalculator {

    private const val MET_WALK = 3.5
    private const val MET_RUN = 8.0
    private const val MET_CYCLE = 6.0

    fun metFor(activityType: String): Double = when (activityType.lowercase()) {
        "walk" -> MET_WALK
        "run" -> MET_RUN
        "cycle" -> MET_CYCLE
        else -> MET_WALK
    }

    fun estimateCalories(activityType: String, weightKg: Float, durationMillis: Long): Float {
        val hours = durationMillis / 3_600_000.0
        return (metFor(activityType) * weightKg * hours).toFloat()
    }
}
