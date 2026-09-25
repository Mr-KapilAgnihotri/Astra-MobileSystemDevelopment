package com.yourname.trackr.ui

import android.animation.ValueAnimator
import android.graphics.Paint
import android.view.animation.LinearInterpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/**
 * Renders the live route on a real MapView during tracking: a rounded polyline for the
 * path so far, a start marker, and a pulsing marker for the current position - with new
 * GPS fixes (arriving roughly every 5s) glided to smoothly over GLIDE_MS instead of
 * teleporting, the same interpolation idea as RouteView but in lat/lng space, which stays
 * correct under the map's own pan/zoom/projection since osmdroid re-projects every frame.
 * The camera follows the live (possibly still-gliding) point.
 */
class LiveRouteRenderer(
    private val mapView: MapView,
    routeColor: Int,
    startColor: Int
) {
    private val liveMarker = MapDotOverlay(routeColor, pulsing = true)
    private val startMarker = MapDotOverlay(startColor)
    private val polyline = Polyline().apply {
        outlinePaint.apply {
            color = routeColor
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    private var committedPoints: List<GeoPoint> = emptyList()
    private var displayedLast: GeoPoint? = null
    private var glideAnimator: ValueAnimator? = null
    private var overlaysAdded = false

    fun start() {
        liveMarker.startPulsing(mapView)
    }

    fun stop() {
        liveMarker.stopPulsing()
        glideAnimator?.cancel()
    }

    fun setPoints(points: List<GeoPoint>) {
        val previousDisplayed = displayedLast
        val hadPrevious = committedPoints.isNotEmpty()
        committedPoints = points
        val newLast = points.lastOrNull() ?: return

        ensureOverlaysAdded()

        if (hadPrevious && previousDisplayed != null && points.size > 1) {
            glideTo(previousDisplayed, newLast)
        } else {
            displayedLast = newLast
            redraw()
            if (mapView.zoomLevelDouble < LIVE_ZOOM) mapView.controller.setZoom(LIVE_ZOOM)
            mapView.controller.animateTo(newLast)
        }
    }

    private fun glideTo(from: GeoPoint, to: GeoPoint) {
        glideAnimator?.cancel()
        if (mapView.zoomLevelDouble < LIVE_ZOOM) mapView.controller.setZoom(LIVE_ZOOM)
        glideAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = GLIDE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                val lat = from.latitude + (to.latitude - from.latitude) * t
                val lng = from.longitude + (to.longitude - from.longitude) * t
                displayedLast = GeoPoint(lat, lng)
                redraw()
                // Follow the glide directly (setCenter, not animateTo) - our own
                // ValueAnimator is already providing the smoothing, so a second built-in
                // camera animation on top would just fight this one.
                mapView.controller.setCenter(displayedLast)
            }
            start()
        }
    }

    private fun redraw() {
        val last = displayedLast ?: return
        val displayPoints = if (committedPoints.size > 1) {
            committedPoints.dropLast(1) + last
        } else {
            committedPoints
        }
        polyline.setPoints(displayPoints)
        liveMarker.point = last
        if (committedPoints.size > 1) {
            startMarker.point = committedPoints.first()
        }
        mapView.invalidate()
    }

    private fun ensureOverlaysAdded() {
        if (overlaysAdded) return
        overlaysAdded = true
        mapView.overlays.add(polyline)
        mapView.overlays.add(startMarker)
        mapView.overlays.add(liveMarker)
    }

    companion object {
        private const val LIVE_ZOOM = 18.0
        private const val GLIDE_MS = 900L
    }
}
