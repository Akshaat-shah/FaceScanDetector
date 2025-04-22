package com.example.facemetrics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
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
        isMirrored = mirrored
        frameRotation = (rotationDegrees % 360 + 360) % 360   // normalise
        Log.d(TAG, "Setting preview info - mirrored: $mirrored, rotation: $frameRotation")
        invalidate()
    }

    /** Push fresh metrics and redraw. */
    fun updateFaceMetrics(metrics: FaceMetrics?) {
        faceMetrics = metrics
        if (metrics != null) {
            Log.d(TAG, "Updating face metrics - bbox: ${metrics.boundingBox.left},${metrics.boundingBox.top},${metrics.boundingBox.right},${metrics.boundingBox.bottom}")
        }
        invalidate()
    }

    /* ───── private state ───── */

    private val TAG = "FaceOverlayView"
    private var faceMetrics: FaceMetrics? = null
    private var isMirrored = false
    private var frameRotation = 0

    /* ───── paint brushes ───── */

    private val boxPaint = Paint().apply {
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
    private val panelPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
    }
    private val headerPaint = Paint(textPaint).apply {
        textSize = 40f
        isFakeBoldText = true
    }

    /* ───── drawing ───── */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val metrics = faceMetrics ?: return            // nothing to show

        // 1)  green rectangle
        drawBoundingBox(canvas, metrics)

        // 2)  landmarks  (optional – uncomment if needed)
        // drawLandmarks(canvas, metrics)

        // 3)  side / bottom info panels
        drawPanels(canvas, metrics)
    }

    /* ---------- rectangle ---------- */

    private fun drawBoundingBox(canvas: Canvas, m: FaceMetrics) {
        val rNorm = transformRect(m.boundingBox)

        // Log the transformation for debugging
        Log.d(TAG, "Original bbox: ${m.boundingBox.left},${m.boundingBox.top},${m.boundingBox.right},${m.boundingBox.bottom}")
        Log.d(TAG, "Transformed bbox: ${rNorm.left},${rNorm.top},${rNorm.right},${rNorm.bottom}")
        Log.d(TAG, "View dimensions: ${width}x${height}")

        // Convert normalized coordinates to actual view coordinates
        val left = rNorm.left * width
        val top = rNorm.top * height
        val right = rNorm.right * width
        val bottom = rNorm.bottom * height

        Log.d(TAG, "Final rect: $left,$top,$right,$bottom")

        canvas.drawRect(left, top, right, bottom, boxPaint)
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
    private val panelWidth = 300f
    private val panelHeight = 480f
    private val bottomHeight = 100f

    private fun drawPanels(canvas: Canvas, m: FaceMetrics) {
        val w = width.toFloat()
        val h = height.toFloat()

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
        canvas.drawText(
            "IPD: ${(m.interpupillaryDistance * 100).roundToInt()} %",
            posRect.left + 20,
            y,
            textPaint
        )
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

        val qualRect = RectF(
            w - panelPadding - panelWidth,
            panelPadding,
            w - panelPadding,
            panelPadding + panelHeight
        )
        canvas.drawRoundRect(qualRect, 15f, 15f, panelPaint)
        canvas.drawText("Quality Metrics:", qualRect.left + 20, qualRect.top + 50, headerPaint)

        y = qualRect.top + 110
        canvas.drawText(
            "Score: ${(m.qualityScore * 100).roundToInt() / 100f}",
            qualRect.left + 20,
            y,
            textPaint
        )
        y += line
        canvas.drawText(
            "Smiling: ${if (m.isSmiling) "Yes" else "No"}",
            qualRect.left + 20,
            y,
            textPaint
        )
        y += line
        canvas.drawText(
            "Eyes: ${if (m.areEyesOpen) "Open" else "Closed"}",
            qualRect.left + 20,
            y,
            textPaint
        )
        y += line
        canvas.drawText(
            "Glasses: ${if (m.hasGlasses) "Yes" else "No"}",
            qualRect.left + 20,
            y,
            textPaint
        )

        val oriRect = RectF(
            w / 2 - 250,
            h - panelPadding - bottomHeight,
            w / 2 + 250,
            h - panelPadding
        )
        canvas.drawRoundRect(oriRect, 15f, 15f, panelPaint)
        canvas.drawText(
            "Orientation:",
            oriRect.left + 20, oriRect.top + 40, headerPaint
        )
        val pitch = String.format(Locale.US, "%.1f°", m.pitch)
        val roll = String.format(Locale.US, "%.1f°", m.roll)
        val yaw = String.format(Locale.US, "%.1f°", m.yaw)
        canvas.drawText(
            "Pitch $pitch  Roll $roll  Yaw $yaw",
            oriRect.left + 40, oriRect.top + 80, textPaint
        )
    }

    /* ───── coordinate helpers ───── */

    private fun transformPoint(p: FaceMetrics.Point): PointF {
        val point = floatArrayOf(p.x, p.y)
        val matrix = buildTransformMatrix()
        matrix.mapPoints(point)
        return PointF(point[0], point[1])
    }

    private fun transformRect(src: FaceMetrics.BoundingBox): RectF {
        val rect = RectF(src.left, src.top, src.right, src.bottom)
        val matrix = buildTransformMatrix()
        matrix.mapRect(rect)
        return rect
    }

    private fun buildTransformMatrix(): Matrix {
        val matrix = Matrix()

        // Center the coordinates for rotation
        matrix.postTranslate(-0.5f, -0.5f)

        // Apply mirroring if needed
        if (isMirrored) {
            matrix.postScale(-1f, 1f)
        }

        // Apply rotation based on the frame orientation
        when (frameRotation) {
            0 -> { /* No rotation needed */ }
            90 -> matrix.postRotate(-90f)
            180 -> matrix.postRotate(-180f)
            270 -> matrix.postRotate(-270f)
        }

        // Move back from center
        matrix.postTranslate(0.5f, 0.5f)

        return matrix
    }
}