package com.yourname.trackr.ui.graph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.yourname.trackr.R

/**
 * Draws a connected line graph of magnitude samples over index (value-over-time).
 * Same normalize-then-draw technique as RouteView, just plotting a single value
 * axis instead of lat/lng.
 */
class AccelChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val samples = mutableListOf<Float>()
    private val path = Path()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_accent)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun setSamples(values: List<Float>) {
        samples.clear()
        samples.addAll(values)
        recalculatePath()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculatePath()
    }

    private fun recalculatePath() {
        path.reset()
        if (samples.size < 2 || width == 0 || height == 0) return

        val minValue = samples.min()
        val maxValue = samples.max()
        val span = (maxValue - minValue).takeIf { it > 0f } ?: 1f

        val usableHeight = height - 2 * PADDING_PX
        val stepX = width.toFloat() / (samples.size - 1)

        samples.forEachIndexed { index, value ->
            val x = index * stepX
            // Flip Y: larger values draw higher on screen.
            val y = PADDING_PX + (1f - (value - minValue) / span) * usableHeight
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, linePaint)
    }

    companion object {
        private const val PADDING_PX = 16f
    }
}
