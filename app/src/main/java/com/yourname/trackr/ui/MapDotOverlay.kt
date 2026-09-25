package com.yourname.trackr.ui

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.animation.LinearInterpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws a solid colored dot at a GeoPoint, optionally with an animated expanding/fading
 * ring around it - used to mark the live/current position while tracking, and a plain
 * dot for the start point or a finished route's endpoints. Purely a rendering overlay:
 * it carries no session data, just redraws wherever `point` is currently set.
 */
class MapDotOverlay(private val color: Int, private val pulsing: Boolean = false) : Overlay() {

    var point: GeoPoint? = null

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = this@MapDotOverlay.color
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = this@MapDotOverlay.color
    }
    private val screenPoint = Point()

    private var progress = 0f
    private var animator: ValueAnimator? = null

    fun startPulsing(mapView: MapView) {
        if (!pulsing) return
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_DURATION_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                mapView.invalidate()
            }
            start()
        }
    }

    fun stopPulsing() {
        animator?.cancel()
        animator = null
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val current = point ?: return
        mapView.projection.toPixels(current, screenPoint)

        if (pulsing) {
            ringPaint.alpha = ((1f - progress) * 180).toInt()
            val ringRadius = DOT_RADIUS_PX + progress * MAX_RING_GROWTH_PX
            canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), ringRadius, ringPaint)
        }
        canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), DOT_RADIUS_PX, dotPaint)
    }

    companion object {
        private const val DOT_RADIUS_PX = 16f
        private const val MAX_RING_GROWTH_PX = 22f
        private const val PULSE_DURATION_MS = 1400L
    }
}
