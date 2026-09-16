package com.yourname.trackr.ui.tracking

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.yourname.trackr.R

/**
 * Draws a route as a connected line from a list of (x, y) points, normalized
 * to fill the view's current size. Reusable for both a live-updating
 * tracking screen and a static replay of a saved session.
 */
class RouteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rawPoints = mutableListOf<PointF>()
    private val normalizedPoints = mutableListOf<PointF>()
    private val path = Path()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_accent)
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface)
        style = Paint.Style.FILL
    }

    fun setPoints(points: List<PointF>) {
        rawPoints.clear()
        rawPoints.addAll(points)
        recalculateNormalizedPoints()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateNormalizedPoints()
    }

    private fun recalculateNormalizedPoints() {
        normalizedPoints.clear()
        path.reset()
        if (rawPoints.isEmpty() || width == 0 || height == 0) return

        val minX = rawPoints.minOf { it.x }
        val maxX = rawPoints.maxOf { it.x }
        val minY = rawPoints.minOf { it.y }
        val maxY = rawPoints.maxOf { it.y }

        val spanX = (maxX - minX).takeIf { it > 0f } ?: 1f
        val spanY = (maxY - minY).takeIf { it > 0f } ?: 1f

        val usableWidth = width - 2 * PADDING_PX
        val usableHeight = height - 2 * PADDING_PX

        rawPoints.forEachIndexed { index, point ->
            val normalizedX = PADDING_PX + ((point.x - minX) / spanX) * usableWidth
            // Flip Y: larger source-Y (e.g. latitude, north) should draw toward the top.
            val normalizedY = PADDING_PX + (1f - (point.y - minY) / spanY) * usableHeight
            val normalized = PointF(normalizedX, normalizedY)
            normalizedPoints.add(normalized)
            if (index == 0) path.moveTo(normalized.x, normalized.y) else path.lineTo(normalized.x, normalized.y)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (normalizedPoints.isEmpty()) return

        canvas.drawPath(path, linePaint)
        val last = normalizedPoints.last()
        canvas.drawCircle(last.x, last.y, MARKER_RADIUS_PX, markerPaint)
    }

    companion object {
        private const val PADDING_PX = 24f
        private const val MARKER_RADIUS_PX = 14f
    }
}
