package com.example.facemetrics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.roundToInt

/**
 * Custom view that renders face metrics as an overlay on top of the camera preview.
 * Displays bounding box, landmarks, and metrics panels.
 */
class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects for different elements
    private val boundingBoxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private val landmarkPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val crosshairPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val panelPaint = Paint().apply {
        color = Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
    }
    
    private val headerTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    // Current face metrics to display
    private var faceMetrics: FaceMetrics? = null
    
    // Panel dimensions and positions
    private val panelPadding = 20f
    private val panelWidth = 300f
    private val panelHeight = 500f
    private val bottomPanelHeight = 100f
    
    /**
     * Update the face metrics to be displayed
     */
    fun updateFaceMetrics(metrics: FaceMetrics) {
        this.faceMetrics = metrics
        invalidate() // Trigger redraw
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val metrics = faceMetrics ?: return
        
        val canvasWidth = width.toFloat()
        val canvasHeight = height.toFloat()
        
        // Draw the metrics panels first (as background)
        drawPositionPanel(canvas, canvasWidth, canvasHeight, metrics)
        drawQualityPanel(canvas, canvasWidth, canvasHeight, metrics)
        drawOrientationPanel(canvas, canvasWidth, canvasHeight, metrics)
        
        // Draw face bounding box if face detected
        if (metrics.detectionConfidence > 0) {
            drawBoundingBox(canvas, canvasWidth, canvasHeight, metrics)
            // Removed drawLandmarks to hide the white dots
            // drawLandmarks(canvas, canvasWidth, canvasHeight, metrics)
            // Also removed crosshair
            // drawCrosshair(canvas, canvasWidth, canvasHeight, metrics) 
        }
    }
    
    /**
     * Draw the bounding box around the detected face
     */
    private fun drawBoundingBox(canvas: Canvas, canvasWidth: Float, canvasHeight: Float, metrics: FaceMetrics) {
        val boundingBox = metrics.boundingBox
        
        val rect = RectF(
            boundingBox.left * canvasWidth,
            boundingBox.top * canvasHeight,
            boundingBox.right * canvasWidth,
            boundingBox.bottom * canvasHeight
        )
        
        canvas.drawRect(rect, boundingBoxPaint)
    }
    
    /**
     * Draw facial landmarks as small circles
     */
    private fun drawLandmarks(canvas: Canvas, canvasWidth: Float, canvasHeight: Float, metrics: FaceMetrics) {
        for (landmark in metrics.landmarks) {
            val x = landmark.position.x * canvasWidth
            val y = landmark.position.y * canvasHeight
            canvas.drawCircle(x, y, 5f, landmarkPaint)
        }
    }
    
    /**
     * Draw a crosshair at the center of the face
     */
    private fun drawCrosshair(canvas: Canvas, canvasWidth: Float, canvasHeight: Float, metrics: FaceMetrics) {
        val centerX = (metrics.facePosition.x + 0.5f) * canvasWidth
        val centerY = (metrics.facePosition.y + 0.5f) * canvasHeight
        
        // Draw crosshair lines
        canvas.drawLine(centerX - 15, centerY, centerX + 15, centerY, crosshairPaint)
        canvas.drawLine(centerX, centerY - 15, centerX, centerY + 15, crosshairPaint)
    }
    
    /**
     * Draw the position metrics panel (left side)
     */
    private fun drawPositionPanel(canvas: Canvas, canvasWidth: Float, canvasHeight: Float, metrics: FaceMetrics) {
        // Left panel for position metrics
        val panelRect = RectF(
            panelPadding,
            panelPadding,
            panelPadding + panelWidth,
            panelPadding + panelHeight
        )
        
        canvas.drawRoundRect(panelRect, 15f, 15f, panelPaint)
        
        // Draw header
        canvas.drawText("Position Metrics:", panelRect.left + 20, panelRect.top + 50, headerTextPaint)
        
        // Draw metrics
        var y = panelRect.top + 120
        val x = panelRect.left + 20
        val lineSpacing = 50f
        
        canvas.drawText("Distance:", x, y, textPaint)
        canvas.drawText("${(metrics.interpupillaryDistance * 100).roundToInt()}mm", x, y + 40, textPaint)
        y += lineSpacing + 40
        
        canvas.drawText("Face Size:", x, y, textPaint)
        canvas.drawText("${(metrics.faceWidth * canvasWidth).roundToInt()}x${(metrics.faceHeight * canvasHeight).roundToInt()}px", 
            x, y + 40, textPaint)
        y += lineSpacing + 40
        
        canvas.drawText("Center:", x, y, textPaint)
        canvas.drawText("X: ${(metrics.facePosition.x * 100).roundToInt()}mm", x, y + 40, textPaint)
        canvas.drawText("Y: ${(metrics.facePosition.y * 100).roundToInt()}mm", x, y + 80, textPaint)
    }
    
    /**
     * Draw the quality metrics panel (right side)
     */
    private fun drawQualityPanel(canvas: Canvas, canvasWidth: Float, canvasHeight: Float, metrics: FaceMetrics) {
        // Right panel for quality metrics
        val panelRect = RectF(
            canvasWidth - panelPadding - panelWidth,
            panelPadding,
            canvasWidth - panelPadding,
            panelPadding + panelHeight
        )
        
        canvas.drawRoundRect(panelRect, 15f, 15f, panelPaint)
        
        // Draw header
        canvas.drawText("Quality Metrics:", panelRect.left + 20, panelRect.top + 50, headerTextPaint)
        
        // Draw metrics
        var y = panelRect.top + 120
        val x = panelRect.left + 20
        val lineSpacing = 50f
        
        canvas.drawText("Quality:", x, y, textPaint)
        canvas.drawText("${(metrics.qualityScore * 100).roundToInt() / 100f}", x, y + 40, textPaint)
        y += lineSpacing + 40
        
        canvas.drawText("Smiling:", x, y, textPaint)
        canvas.drawText(
            if (metrics.isSmiling) "Yes (${(metrics.smileConfidence * 10).roundToInt() / 10f})" 
            else "No (${(metrics.smileConfidence * 10).roundToInt() / 10f})",
            x, y + 40, textPaint
        )
        y += lineSpacing + 40
        
        canvas.drawText("Eyes:", x, y, textPaint)
        canvas.drawText(
            if (metrics.areEyesOpen) "Open"
            else "Closed",
            x, y + 40, textPaint
        )
        
        y += lineSpacing + 40
        canvas.drawText("Glasses:", x, y, textPaint)
        canvas.drawText(
            if (metrics.hasGlasses) "Yes" else "No",
            x, y + 40, textPaint
        )
    }
    
    /**
     * Draw the orientation panel (bottom)
     */
    private fun drawOrientationPanel(canvas: Canvas, canvasWidth: Float, canvasHeight: Float, metrics: FaceMetrics) {
        // Bottom panel for orientation data
        val panelRect = RectF(
            canvasWidth / 2 - 250,
            canvasHeight - panelPadding - bottomPanelHeight,
            canvasWidth / 2 + 250,
            canvasHeight - panelPadding
        )
        
        canvas.drawRoundRect(panelRect, 15f, 15f, panelPaint)
        
        // Draw header
        canvas.drawText("Orientation Data:", panelRect.left + 20, panelRect.top + 40, headerTextPaint)
        
        // Draw orientation values on one line
        val y = panelRect.top + 80
        val pitch = String.format(Locale.US, "%.1f°", metrics.pitch)
        val roll = String.format(Locale.US, "%.1f°", metrics.roll)
        val yaw = String.format(Locale.US, "%.1f°", metrics.yaw)
        
        canvas.drawText("Pitch: $pitch   Roll: $roll   Yaw: $yaw", 
            panelRect.left + 50, y, textPaint)
    }
}