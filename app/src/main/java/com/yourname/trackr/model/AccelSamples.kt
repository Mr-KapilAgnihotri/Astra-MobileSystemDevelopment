package com.yourname.trackr.model

import org.json.JSONArray
import org.json.JSONException

fun List<Float>.toAccelJson(): String {
    val array = JSONArray()
    forEach { array.put(it.toDouble()) }
    return array.toString()
}

fun String.toFloatSamples(): List<Float> {
    if (isBlank()) return emptyList()
    return try {
        val array = JSONArray(this)
        (0 until array.length()).map { index -> array.getDouble(index).toFloat() }
    } catch (e: JSONException) {
        emptyList()
    }
}
