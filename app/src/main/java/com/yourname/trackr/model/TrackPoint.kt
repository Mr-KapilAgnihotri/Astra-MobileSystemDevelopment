package com.yourname.trackr.model

import org.json.JSONArray
import org.json.JSONObject

data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)

fun List<TrackPoint>.toPathJson(): String {
    val array = JSONArray()
    forEach { point ->
        array.put(
            JSONObject().apply {
                put("lat", point.lat)
                put("lng", point.lng)
                put("t", point.timestamp)
            }
        )
    }
    return array.toString()
}

fun String.toTrackPoints(): List<TrackPoint> {
    if (isBlank()) return emptyList()
    return try {
        val array = JSONArray(this)
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            TrackPoint(obj.getDouble("lat"), obj.getDouble("lng"), obj.optLong("t"))
        }
    } catch (e: org.json.JSONException) {
        emptyList()
    }
}
