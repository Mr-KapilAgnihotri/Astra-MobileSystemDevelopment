package com.yourname.trackr.ui.graph

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.yourname.trackr.R

/**
 * Area-chart style line graph of magnitude samples over index (value-over-time): a smooth
 * line, a gradient fill fading to transparent underneath it, light horizontal gridlines,
 * and a label on the peak value. Same normalize-then-draw technique as RouteView, plotting
 * a single value axis instead of lat/lng.
 */
class AccelChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val samples = mutableListOf<Float>()
    private val linePath = Path()
    private val fillPath = Path()
    private var peakPoint: PointF? = null
    private var peakValue: Float = 0f

    private val accentColor = ContextCompat.getColor(context, R.color.trackr_accent)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_gridline)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val peakLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface_muted)
        textAlign = Paint.Align.CENTER
        textSize = 26f
    }

    private val peakDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    fun setSamples(values: List<Float>) {
        samples.clear()
        samples.addAll(values)
        recalculatePaths()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculatePaths()
    }

    private fun recalculatePaths() {
        linePath.reset()
        fillPath.reset()
        peakPoint = null
        if (samples.size < 2 || width == 0 || height == 0) return

        val minValue = samples.min()
        val maxValue = samples.max()
        val isFlat = maxValue - minValue <= 0f
        val span = (maxValue - minValue).takeIf { it > 0f } ?: 1f

        val top = TOP_PADDING_PX
        val bottom = height - BOTTOM_PADDING_PX
        val usableHeight = bottom - top
        val stepX = width.toFloat() / (samples.size - 1)

        val points = samples.mapIndexed { index, value ->
            val x = index * stepX
            // Flip Y: larger values draw higher on screen. A perfectly flat reading (e.g. a
            // stationary device at rest) has no real span to divide by - draw it centered
            // instead of pinned to the bottom edge.
            val y = if (isFlat) top + usableHeight / 2f else top + (1f - (value - minValue) / span) * usableHeight
            PointF(x, y)
        }

        points.forEachIndexed { index, point ->
            if (index == 0) linePath.moveTo(point.x, point.y) else linePath.lineTo(point.x, point.y)
        }

        fillPath.set(linePath)
        fillPath.lineTo(points.last().x, bottom)
        fillPath.lineTo(points.first().x, bottom)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, top, 0f, bottom,
            accentColor, accentColor and 0x00FFFFFF, Shader.TileMode.CLAMP
        )

        val peakIndex = samples.indexOf(maxValue)
        peakPoint = points[peakIndex]
        peakValue = maxValue
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGridlines(canvas)

        if (samples.size < 2) return

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        peakPoint?.let { point ->
            canvas.drawCircle(point.x, point.y, PEAK_DOT_RADIUS_PX, peakDotPaint)
            canvas.drawText(
                String.format("%.1f", peakValue),
                point.x.coerceIn(PEAK_LABEL_MARGIN_PX, width - PEAK_LABEL_MARGIN_PX),
                (point.y - PEAK_LABEL_OFFSET_PX).coerceAtLeast(peakLabelPaint.textSize),
                peakLabelPaint
            )
        }
    }

    private fun drawGridlines(canvas: Canvas) {
        val top = TOP_PADDING_PX
        val bottom = height - BOTTOM_PADDING_PX
        for (i in 0..GRIDLINE_COUNT) {
            val y = top + (bottom - top) * (i.toFloat() / GRIDLINE_COUNT)
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }

    companion object {
        private const val TOP_PADDING_PX = 32f
        private const val BOTTOM_PADDING_PX = 16f
        private const val GRIDLINE_COUNT = 3
        private const val PEAK_DOT_RADIUS_PX = 6f
        private const val PEAK_LABEL_OFFSET_PX = 16f
        private const val PEAK_LABEL_MARGIN_PX = 40f
    }
}
