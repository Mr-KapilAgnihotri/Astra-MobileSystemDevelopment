package com.yourname.trackr.ui.weekly

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.yourname.trackr.R
import com.yourname.trackr.model.DayDistance

/**
 * Apple-Health-style bar chart: one rounded-top bar per day, height relative to the
 * highest value in the set, a value label above each bar, and a day-of-week label
 * underneath. Today's bar (assumed to be the last entry - see WeeklyStatsViewModel,
 * which always builds oldest-to-newest) is drawn in a brighter shade so it's easy to
 * spot at a glance. No gridlines or axis ticks. Days with zero distance still draw a
 * minimal sliver so the week reads as seven bars, not six-plus-a-gap.
 */
class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<DayDistance> = emptyList()
    private val barRect = RectF()
    private val barPath = Path()
    private val cornerRadii = FloatArray(8)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_accent)
        style = Paint.Style.FILL
    }

    private val todayBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_accent_light)
        style = Paint.Style.FILL
    }

    private val dayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface_muted)
        textAlign = Paint.Align.CENTER
        textSize = 26f
    }

    private val valueLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface)
        textAlign = Paint.Align.CENTER
        textSize = 24f
    }

    fun setData(values: List<DayDistance>) {
        data = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxKm = data.maxOf { it.km }.takeIf { it > 0f } ?: 1f
        val dayLabelHeight = dayLabelPaint.textSize + LABEL_PADDING_PX
        val valueLabelHeight = valueLabelPaint.textSize + LABEL_PADDING_PX
        val chartTop = valueLabelHeight
        val chartBottom = height - dayLabelHeight
        val chartHeight = chartBottom - chartTop
        val slotWidth = width.toFloat() / data.size
        val barWidth = slotWidth * BAR_WIDTH_FRACTION

        val todayIndex = data.lastIndex

        data.forEachIndexed { index, day ->
            val slotCenterX = slotWidth * index + slotWidth / 2f
            val barHeightPx = (day.km / maxKm) * chartHeight
            val drawnHeight = barHeightPx.coerceAtLeast(MIN_BAR_HEIGHT_PX)
            val top = chartBottom - drawnHeight

            barRect.set(slotCenterX - barWidth / 2f, top, slotCenterX + barWidth / 2f, chartBottom)
            cornerRadii.fill(0f)
            cornerRadii[0] = BAR_CORNER_RADIUS_PX
            cornerRadii[1] = BAR_CORNER_RADIUS_PX
            cornerRadii[2] = BAR_CORNER_RADIUS_PX
            cornerRadii[3] = BAR_CORNER_RADIUS_PX
            barPath.reset()
            barPath.addRoundRect(barRect, cornerRadii, Path.Direction.CW)
            canvas.drawPath(barPath, if (index == todayIndex) todayBarPaint else barPaint)

            if (day.km > 0f) {
                canvas.drawText(
                    String.format("%.1f", day.km),
                    slotCenterX, top - LABEL_PADDING_PX, valueLabelPaint
                )
            }
            canvas.drawText(day.dayLabel, slotCenterX, height.toFloat() - 4f, dayLabelPaint)
        }
    }

    companion object {
        private const val BAR_WIDTH_FRACTION = 0.5f
        private const val LABEL_PADDING_PX = 10f
        private const val MIN_BAR_HEIGHT_PX = 6f
        private const val BAR_CORNER_RADIUS_PX = 8f
    }
}
