package com.yourname.trackr.ui.weekly

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.yourname.trackr.R
import com.yourname.trackr.model.DayDistance

/**
 * Simple Apple-Health-style bar chart: one bar per day, height relative to the
 * highest value in the set, with a day-of-week label underneath. No gridlines
 * or axis ticks. Days with zero distance still draw a minimal sliver so the
 * week reads as seven bars, not six-plus-a-gap.
 */
class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<DayDistance> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_accent)
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface_muted)
        textAlign = Paint.Align.CENTER
        textSize = 28f
    }

    fun setData(values: List<DayDistance>) {
        data = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxKm = data.maxOf { it.km }.takeIf { it > 0f } ?: 1f
        val chartHeight = height - (labelPaint.textSize + LABEL_PADDING_PX)
        val slotWidth = width.toFloat() / data.size
        val barWidth = slotWidth * BAR_WIDTH_FRACTION

        data.forEachIndexed { index, day ->
            val slotCenterX = slotWidth * index + slotWidth / 2f
            val barHeightPx = (day.km / maxKm) * chartHeight
            val drawnHeight = barHeightPx.coerceAtLeast(MIN_BAR_HEIGHT_PX)
            val top = chartHeight - drawnHeight
            canvas.drawRoundRect(
                slotCenterX - barWidth / 2f, top,
                slotCenterX + barWidth / 2f, chartHeight,
                8f, 8f, barPaint
            )
            canvas.drawText(day.dayLabel, slotCenterX, height.toFloat() - 4f, labelPaint)
        }
    }

    companion object {
        private const val BAR_WIDTH_FRACTION = 0.5f
        private const val LABEL_PADDING_PX = 12f
        private const val MIN_BAR_HEIGHT_PX = 6f
    }
}
