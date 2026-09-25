package com.yourname.trackr.ui.tracking

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.yourname.trackr.R

/**
 * Draws a route as a smoothed, gradient-stroked line from a list of (x, y) points,
 * normalized to fill the view's current size, with a start marker and an end/current
 * marker (pulsing while `isLive`), gliding new points in over POINT_GLIDE_MS instead of
 * snapping to them.
 *
 * Note: not currently wired into any screen. The live tracking screen uses a real osmdroid
 * MapView (see ui/LiveRouteRenderer.kt) so the route is a real, navigable map rather than a
 * plain drawn line - LiveRouteRenderer applies this same glide-instead-of-snap idea, just in
 * lat/lng space against the map's own projection. This class is kept for its custom-View
 * coursework value and the same visual techniques (gradient stroke, smoothing, glide), but a
 * running app won't show it.
 */
class RouteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rawPoints = mutableListOf<PointF>()

    /** Normalized (screen-space) position of every real point, recomputed whenever the
     *  point list or view size changes. The true end position - the animation's target. */
    private val committedNormalizedPoints = mutableListOf<PointF>()

    /** What's actually drawn for the last point right now - equals the true end position
     *  once any in-flight glide animation finishes. */
    private var displayedEndPoint: PointF? = null

    private val path = Path()

    private val accentColor = ContextCompat.getColor(context, R.color.trackr_accent)
    private val accentLightColor = ContextCompat.getColor(context, R.color.trackr_accent_light)
    private val startColor = ContextCompat.getColor(context, R.color.trackr_success)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val startDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = startColor
        style = Paint.Style.FILL
    }

    private val endDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    private val pulseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var isLive = false
    private var pulseProgress = 0f
    private var pulseAnimator: ValueAnimator? = null
    private var glideAnimator: ValueAnimator? = null

    /** Whether the end marker should pulse - set true while a session is actively tracking. */
    fun setLive(live: Boolean) {
        if (isLive == live) return
        isLive = live
        if (live) startPulsing() else stopPulsing()
    }

    fun setPoints(points: List<PointF>) {
        val hadPreviousPoint = committedNormalizedPoints.isNotEmpty()
        val previousDisplayedEnd = displayedEndPoint

        rawPoints.clear()
        rawPoints.addAll(points)
        recalculateNormalizedPoints()

        val newEnd = committedNormalizedPoints.lastOrNull()
        if (newEnd != null && hadPreviousPoint && previousDisplayedEnd != null) {
            glideTo(previousDisplayedEnd, newEnd)
        } else {
            // First point of a new session, or nothing to animate from - just show it.
            displayedEndPoint = newEnd
            rebuildPath()
            invalidate()
        }
    }

    private fun glideTo(from: PointF, to: PointF) {
        glideAnimator?.cancel()
        glideAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = POINT_GLIDE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                displayedEndPoint = PointF(
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t
                )
                rebuildPath()
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateNormalizedPoints()
        displayedEndPoint = committedNormalizedPoints.lastOrNull()
        rebuildPath()
    }

    override fun onDetachedFromWindow() {
        stopPulsing()
        glideAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun startPulsing() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_DURATION_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                pulseProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopPulsing() {
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    private fun recalculateNormalizedPoints() {
        committedNormalizedPoints.clear()
        if (rawPoints.isEmpty() || width == 0 || height == 0) return

        val minX = rawPoints.minOf { it.x }
        val maxX = rawPoints.maxOf { it.x }
        val minY = rawPoints.minOf { it.y }
        val maxY = rawPoints.maxOf { it.y }

        val spanX = (maxX - minX).takeIf { it > 0f } ?: 1f
        val spanY = (maxY - minY).takeIf { it > 0f } ?: 1f

        val usableWidth = width - 2 * PADDING_PX
        val usableHeight = height - 2 * PADDING_PX

        rawPoints.forEach { point ->
            val normalizedX = PADDING_PX + ((point.x - minX) / spanX) * usableWidth
            // Flip Y: larger source-Y (e.g. latitude, north) should draw toward the top.
            val normalizedY = PADDING_PX + (1f - (point.y - minY) / spanY) * usableHeight
            committedNormalizedPoints.add(PointF(normalizedX, normalizedY))
        }
    }

    /** Quadratic-through-midpoints smoothing: rounds off the sharp angles a straight
     *  point-to-point lineTo path would have, without needing a real spline library.
     *  Uses committedNormalizedPoints for every point except the last, which is swapped
     *  for displayedEndPoint so an in-flight glide animation is reflected in the path. */
    private fun rebuildPath() {
        path.reset()
        if (committedNormalizedPoints.isEmpty()) return

        val displayPoints = if (committedNormalizedPoints.size > 1) {
            committedNormalizedPoints.dropLast(1) + (displayedEndPoint ?: committedNormalizedPoints.last())
        } else {
            committedNormalizedPoints
        }

        path.moveTo(displayPoints.first().x, displayPoints.first().y)
        for (i in 0 until displayPoints.size - 1) {
            val current = displayPoints[i]
            val next = displayPoints[i + 1]
            val midX = (current.x + next.x) / 2f
            val midY = (current.y + next.y) / 2f
            path.quadTo(current.x, current.y, midX, midY)
        }
        if (displayPoints.size > 1) {
            val last = displayPoints.last()
            path.lineTo(last.x, last.y)
        }

        updateGradient(displayPoints)
    }

    private fun updateGradient(displayPoints: List<PointF>) {
        if (displayPoints.size < 2) {
            linePaint.shader = null
            linePaint.color = accentColor
            return
        }
        val start = displayPoints.first()
        val end = displayPoints.last()
        linePaint.shader = LinearGradient(
            start.x, start.y, end.x, end.y,
            accentLightColor, accentColor, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (committedNormalizedPoints.isEmpty()) return

        canvas.drawPath(path, linePaint)

        if (committedNormalizedPoints.size > 1) {
            val start = committedNormalizedPoints.first()
            canvas.drawCircle(start.x, start.y, DOT_RADIUS_PX, startDotPaint)
        }

        val end = displayedEndPoint ?: committedNormalizedPoints.last()
        if (isLive) {
            pulseRingPaint.alpha = ((1f - pulseProgress) * 180).toInt()
            val ringRadius = DOT_RADIUS_PX + pulseProgress * MAX_RING_GROWTH_PX
            canvas.drawCircle(end.x, end.y, ringRadius, pulseRingPaint)
        }
        canvas.drawCircle(end.x, end.y, DOT_RADIUS_PX, endDotPaint)
    }

    companion object {
        private const val PADDING_PX = 24f
        private const val DOT_RADIUS_PX = 14f
        private const val MAX_RING_GROWTH_PX = 18f
        private const val PULSE_DURATION_MS = 1400L
        private const val POINT_GLIDE_MS = 900L
    }
}
