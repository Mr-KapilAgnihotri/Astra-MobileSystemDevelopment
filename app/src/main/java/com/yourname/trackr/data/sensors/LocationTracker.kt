package com.yourname.trackr.data.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Wraps FusedLocationProviderClient for GPS updates, falling back to the plain
 * android.location.LocationManager on devices without Google Play Services. Either path
 * reports lat/lng roughly every UPDATE_INTERVAL_MS, plus an immediate best-known fix on
 * start so the UI isn't sitting on a blank screen waiting for the first fresh reading -
 * which, especially indoors or on an emulator, can take a long time or never arrive.
 */
class LocationTracker(private val context: Context) {

    private val useFusedProvider: Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    private val fusedClient: FusedLocationProviderClient? =
        if (useFusedProvider) LocationServices.getFusedLocationProviderClient(context) else null

    private val plainLocationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private var fusedCallback: LocationCallback? = null
    private var plainListener: LocationListener? = null

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /** True if the user has a location provider switched on at the OS level (Settings > Location).
     *  A granted permission with this false means requestLocationUpdates will just sit silently
     *  forever - worth checking explicitly rather than leaving the UI guessing. */
    fun isLocationEnabled(): Boolean {
        val manager = plainLocationManager ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun start(onLocation: (lat: Double, lng: Double) -> Unit) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "start() called without ACCESS_FINE_LOCATION granted - ignoring")
            return
        }

        val client = fusedClient
        if (client != null) {
            Log.d(TAG, "Starting updates via FusedLocationProviderClient")
            // Don't make the caller wait for the first periodic update - report a cached fix
            // right away if the device has one.
            client.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        Log.d(TAG, "Cached last location: ${it.latitude}, ${it.longitude}")
                        onLocation(it.latitude, it.longitude)
                    } ?: Log.d(TAG, "No cached last location available yet")
                }
                .addOnFailureListener { e -> Log.w(TAG, "getLastLocation failed", e) }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS).build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        Log.d(TAG, "Fused location update: ${it.latitude}, ${it.longitude}")
                        onLocation(it.latitude, it.longitude)
                    }
                }
            }
            fusedCallback = callback
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } else {
            Log.d(TAG, "Play Services unavailable - falling back to LocationManager")
            startPlainLocationUpdates(onLocation)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startPlainLocationUpdates(onLocation: (lat: Double, lng: Double) -> Unit) {
        val manager = plainLocationManager ?: return
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> {
                Log.w(TAG, "No location provider is enabled on this device")
                return
            }
        }

        manager.getLastKnownLocation(provider)?.let {
            Log.d(TAG, "Cached last known location ($provider): ${it.latitude}, ${it.longitude}")
            onLocation(it.latitude, it.longitude)
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "LocationManager update: ${location.latitude}, ${location.longitude}")
                onLocation(location.latitude, location.longitude)
            }

            @Deprecated("Deprecated in Java", ReplaceWith(""))
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        plainListener = listener
        manager.requestLocationUpdates(provider, UPDATE_INTERVAL_MS, 0f, listener, Looper.getMainLooper())
    }

    fun stop() {
        fusedCallback?.let { fusedClient?.removeLocationUpdates(it) }
        fusedCallback = null
        plainListener?.let { plainLocationManager?.removeUpdates(it) }
        plainListener = null
    }

    companion object {
        private const val TAG = "LocationTracker"
        private const val UPDATE_INTERVAL_MS = 5000L
    }
}
