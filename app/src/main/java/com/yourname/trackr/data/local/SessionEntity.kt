package com.yourname.trackr.data.local

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "sessions")
@Parcelize
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val startTime: Long,
    val endTime: Long,
    val stepCount: Int,
    val distanceMeters: Float,
    val pathJson: String,
    val weatherTempC: Float? = null,
    val weatherWeatherCode: Int? = null,
    val photoUri: String? = null,
    val accelSamplesJson: String? = null
) : Parcelable
