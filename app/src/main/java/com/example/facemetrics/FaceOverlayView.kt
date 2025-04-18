package com.example.facemetrics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.roundToInt

/**
 * Transparent view that sits on top of your camera preview and
 * visualises the [FaceMetrics] coming from MetricsCalculator.
 *
 * Call [setPreviewInfo] once when the camera starts (or orientation
 * changes) so the overlay knows if the preview is mirrored and/or rotated.
 */
class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /* ───── public API ───── */

    /** Provide current preview transform:  */
    fun setPreviewInfo(
        mirrored: Boolean,
        rotationDegrees: Int       // 0 | 90 | 180 | 270
    ) {
        isMirrored     = mirrored
        frameRotation  = (rotationDegrees % 360 + 360) % 360   // normalise
        invalidate()
    }

    /** Push fresh metrics and redraw. */
    fun updateFaceMetrics(metrics: FaceMetrics?) {
        faceMetrics = metrics
        invalidate()
    }

    /* ───── private state ───── */

    private var faceMetrics: FaceMetrics? = null
    private var isMirrored     = false
    private var frameRotation  = 0

    /* ───── paint brushes ───── */

    private val boxPaint      = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val landmarkPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val panelPaint    = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val textPaint     = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
    }
    private val headerPaint   = Paint(textPaint).apply {
        textSize = 40f
        isFakeBoldText = true
    }

    /* ───── drawing ───── */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val metrics = faceMetrics ?: return            // nothing to show

        // 1)  green rectangle
        drawBoundingBox(canvas, metrics)

        // 2)  landmarks  (optional – comment out if not wanted)
        // drawLandmarks(canvas, metrics)

        // 3)  side / bottom info panels
        drawPanels(canvas, metrics)
    }

    /* ---------- rectangle ---------- */

    private fun drawBoundingBox(canvas: Canvas, m: FaceMetrics) {
        // Input rect is in 0‥1 normalised coordinates (upright, non‑mirrored)
        val rNorm = transformRect(m.boundingBox)
        canvas.drawRect(
            rNorm.left  * width,
            rNorm.top   * height,
            rNorm.right * width,
            rNorm.bottom* height,
            boxPaint
        )
    }

    /* ---------- landmarks ---------- */

    private fun drawLandmarks(canvas: Canvas, m: FaceMetrics) {
        for (lm in m.landmarks) {
            val p = transformPoint(lm.position)
            canvas.drawCircle(p.x * width, p.y * height, 5f, landmarkPaint)
        }
    }

    /* ---------- metric panels (unchanged except for spacing) ---------- */

    private val panelPadding = 20f
    private val panelWidth   = 300f
    private val panelHeight  = 480f
    private val bottomHeight = 100f

    private fun drawPanels(canvas: Canvas, m: FaceMetrics) {
        val w = width.toFloat()
        val h = height.toFloat()

        /* position panel (left) */
        val posRect = RectF(
            panelPadding,
            panelPadding,
            panelPadding + panelWidth,
            panelPadding + panelHeight
        )
        canvas.drawRoundRect(posRect, 15f, 15f, panelPaint)
        canvas.drawText("Position Metrics:", posRect.left + 20, posRect.top + 50, headerPaint)

        var y = posRect.top + 110
        val line = 48f
        canvas.drawText("IPD: ${(m.interpupillaryDistance * 100).roundToInt()} %", posRect.left + 20, y, textPaint)
        y += line
        canvas.drawText("Face W×H:", posRect.left + 20, y, textPaint)
        canvas.drawText(
            "${(m.faceWidth * w).roundToInt()}×${(m.faceHeight * h).roundToInt()} px",
            posRect.left + 20, y + line, textPaint
        )
        y += line * 2
        canvas.drawText("Centre:", posRect.left + 20, y, textPaint)
        canvas.drawText(
            "x ${(m.facePosition.x * 100).roundToInt()}  y ${(m.facePosition.y * 100).roundToInt()}",
            posRect.left + 20, y + line, textPaint
        )

        /* quality panel (right) */
        val qualRect = RectF(
            w - panelPadding - panelWidth,
            panelPadding,
            w - panelPadding,
            panelPadding + panelHeight
        )
        canvas.drawRoundRect(qualRect, 15f, 15f, panelPaint)
        canvas.drawText("Quality Metrics:", qualRect.left + 20, qualRect.top + 50, headerPaint)

        y = qualRect.top + 110
        canvas.drawText("Score: ${(m.qualityScore*100).roundToInt()/100f}", qualRect.left + 20, y, textPaint)
        y += line
        canvas.drawText("Smiling: ${if (m.isSmiling) "Yes" else "No"}", qualRect.left + 20, y, textPaint)
        y += line
        canvas.drawText("Eyes: ${if (m.areEyesOpen) "Open" else "Closed"}", qualRect.left + 20, y, textPaint)
        y += line
        canvas.drawText("Glasses: ${if (m.hasGlasses) "Yes" else "No"}", qualRect.left + 20, y, textPaint)

        /* orientation bottom panel */
        val oriRect = RectF(
            w/2 - 250,
            h - panelPadding - bottomHeight,
            w/2 + 250,
            h - panelPadding
        )
        canvas.drawRoundRect(oriRect, 15f, 15f, panelPaint)
        canvas.drawText("Orientation:",
            oriRect.left + 20, oriRect.top + 40, headerPaint)
        val pitch = String.format(Locale.US, "%.1f°", m.pitch)
        val roll  = String.format(Locale.US, "%.1f°", m.roll)
        val yaw   = String.format(Locale.US, "%.1f°", m.yaw)
        canvas.drawText("Pitch $pitch  Roll $roll  Yaw $yaw",
            oriRect.left + 40, oriRect.top + 80, textPaint)
    }

    /* ───── coordinate helpers ───── */

    /** Apply rotation + mirroring to a normalised rectangle. */
    private fun transformRect(src: FaceMetrics.BoundingBox): RectF {
        var left   = src.left
        var top    = src.top
        var right  = src.right
        var bottom = src.bottom

        // 1) rotation
        when (frameRotation) {
            90  -> { val l = left;  left = top;        top = 1f - right
                val r = right; right = bottom;     bottom = 1f - l }
            180 -> { left = 1f - right; top = 1f - bottom
                right = 1f - src.left; bottom = 1f - src.top }
            270 -> { val l = left;  left = 1f - bottom; bottom = right
                val t = top;   top  = l;           right  = 1f - t }
        }

        // 2) mirroring (X axis)
        if (isMirrored) {
            val l = left
            left  = 1f - right
            right = 1f - l
        }
        return RectF(left, top, right, bottom)
    }

    /** Same transform for an individual point. */
    private fun transformPoint(p: FaceMetrics.Point): PointF {
        var x = p.x
        var y = p.y

        when (frameRotation) {
            90  -> { val t = x; x = y;       y = 1f - t }
            180 -> { x = 1f - x; y = 1f - y }
            270 -> { val t = x; x = 1f - y; y = t }
        }
        if (isMirrored) x = 1f - x
        return PointF(x, y)
    }
}
