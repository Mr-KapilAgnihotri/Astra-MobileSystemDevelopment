package com.yourname.trackr.ui.compass

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.yourname.trackr.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A circular compass dial. The dial face and N/E/S/W(+ordinal) labels stay fixed; only the
 * needle rotates via canvas.rotate() to point at magnetic north, matching how real compass
 * apps behave. The displayed heading is itself animated between updates (not snapped) so it
 * reads smoothly even if the underlying sensor value arrives in small steps.
 *
 * All proportions scale off the measured radius rather than fixed dp constants, since this
 * is used at a small ambient size (~120-140dp on Home) - ordinal labels (NE/SE/SW/NW) and
 * the minor 15-degree ticks are dropped below a size threshold where they'd just be clutter.
 */
class CompassDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.trackr_surface_variant)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.trackr_on_surface_muted)
    }
    private val cardinalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val ordinalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface_muted)
        textAlign = Paint.Align.CENTER
    }
    private val needleAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_accent)
        style = Paint.Style.FILL
    }
    private val needleNeutralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_on_surface_muted)
        style = Paint.Style.FILL
    }
    private val centerPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackr_surface)
        style = Paint.Style.FILL
    }

    private val oval = RectF()
    private val northPath = Path()
    private val southPath = Path()

    private var displayedHeading = 0f
    private var rotationAnimator: ValueAnimator? = null

    private val cardinals = listOf(0f to "N", 90f to "E", 180f to "S", 270f to "W")
    private val ordinals = listOf(45f to "NE", 135f to "SE", 225f to "SW", 315f to "NW")

    /** Animates the needle from its current displayed angle to the new heading, always via
     *  the shorter way around the circle (e.g. 350 -> 10 animates +20, not -340). */
    fun setHeading(headingDegrees: Float) {
        val from = displayedHeading
        var delta = (headingDegrees - from) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        val to = from + delta

        rotationAnimator?.cancel()
        rotationAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = ROTATION_ANIM_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                displayedHeading = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        rotationAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (min(width, height) / 2f) - (min(width, height) * PADDING_FRACTION)
        if (radius <= 0f) return

        dialPaint.strokeWidth = radius * DIAL_STROKE_FRACTION
        tickPaint.strokeWidth = radius * TICK_STROKE_FRACTION
        cardinalLabelPaint.textSize = radius * CARDINAL_TEXT_FRACTION
        ordinalLabelPaint.textSize = radius * ORDINAL_TEXT_FRACTION

        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawOval(oval, dialPaint)

        val showFineDetail = radius >= FINE_DETAIL_MIN_RADIUS_PX
        drawTicks(canvas, cx, cy, radius, showFineDetail)
        drawLabels(canvas, cx, cy, radius, showFineDetail)
        drawNeedle(canvas, cx, cy, radius)
    }

    private fun drawTicks(canvas: Canvas, cx: Float, cy: Float, radius: Float, showFineDetail: Boolean) {
        val step = if (showFineDetail) 15 else 90
        val majorTick = radius * MAJOR_TICK_FRACTION
        val minorTick = radius * MINOR_TICK_FRACTION
        for (degree in 0 until 360 step step) {
            val angle = Math.toRadians(degree - 90.0)
            val outerX = cx + radius * cos(angle).toFloat()
            val outerY = cy + radius * sin(angle).toFloat()
            val tickLength = if (degree % 90 == 0) majorTick else minorTick
            val innerX = cx + (radius - tickLength) * cos(angle).toFloat()
            val innerY = cy + (radius - tickLength) * sin(angle).toFloat()
            canvas.drawLine(innerX, innerY, outerX, outerY, tickPaint)
        }
    }

    private fun drawLabels(canvas: Canvas, cx: Float, cy: Float, radius: Float, showFineDetail: Boolean) {
        val labelRadius = radius - radius * LABEL_INSET_FRACTION
        cardinals.forEach { (degree, label) -> drawLabelAt(canvas, cx, cy, labelRadius, degree, label, cardinalLabelPaint) }
        if (showFineDetail) {
            ordinals.forEach { (degree, label) -> drawLabelAt(canvas, cx, cy, labelRadius, degree, label, ordinalLabelPaint) }
        }
    }

    private fun drawLabelAt(
        canvas: Canvas, cx: Float, cy: Float, labelRadius: Float, degree: Float, label: String, paint: Paint
    ) {
        val angle = Math.toRadians(degree - 90.0)
        val x = cx + labelRadius * cos(angle).toFloat()
        val y = cy + labelRadius * sin(angle).toFloat() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(label, x, y, paint)
    }

    private fun drawNeedle(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val needleLength = radius - radius * NEEDLE_INSET_FRACTION
        val needleWidth = radius * NEEDLE_WIDTH_FRACTION

        northPath.reset()
        northPath.moveTo(cx, cy - needleLength)
        northPath.lineTo(cx - needleWidth, cy)
        northPath.lineTo(cx + needleWidth, cy)
        northPath.close()

        southPath.reset()
        southPath.moveTo(cx, cy + needleLength * SOUTH_LENGTH_FRACTION)
        southPath.lineTo(cx - needleWidth, cy)
        southPath.lineTo(cx + needleWidth, cy)
        southPath.close()

        canvas.save()
        // Rotate the needle only (not the dial/labels) so it points at magnetic north.
        canvas.rotate(-displayedHeading, cx, cy)
        canvas.drawPath(northPath, needleAccentPaint)
        canvas.drawPath(southPath, needleNeutralPaint)
        canvas.restore()

        canvas.drawCircle(cx, cy, radius * CENTER_PIN_FRACTION, centerPinPaint)
    }

    companion object {
        private const val PADDING_FRACTION = 0.1f
        private const val DIAL_STROKE_FRACTION = 0.03f
        private const val TICK_STROKE_FRACTION = 0.02f
        private const val MAJOR_TICK_FRACTION = 0.14f
        private const val MINOR_TICK_FRACTION = 0.07f
        private const val LABEL_INSET_FRACTION = 0.32f
        private const val NEEDLE_INSET_FRACTION = 0.22f
        private const val NEEDLE_WIDTH_FRACTION = 0.08f
        private const val SOUTH_LENGTH_FRACTION = 0.6f
        private const val CENTER_PIN_FRACTION = 0.06f
        private const val CARDINAL_TEXT_FRACTION = 0.24f
        private const val ORDINAL_TEXT_FRACTION = 0.15f
        private const val FINE_DETAIL_MIN_RADIUS_PX = 90f
        private const val ROTATION_ANIM_MS = 180L
    }
}
