package com.example.facemetrics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var metrics: FaceMetrics? = null
    
    // Paints for different UI elements
    private val boundingBoxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#80000000")  // Semi-transparent black
    }
    
    private val separatorPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }
    
    private val crosshairPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
    }
    
    // Method to update with new metrics
    fun updateMetrics(metrics: FaceMetrics?) {
        this.metrics = metrics
        invalidate() // Request redraw
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val metrics = this.metrics ?: return
        
        // Draw the face bounding box if we have valid coordinates
        if (metrics.bbW > 0 && metrics.bbH > 0) {
            // Create rect for bounding box (scaled to view size)
            val rect = RectF(
                metrics.bbCol.toFloat(),
                metrics.bbRow.toFloat(),
                (metrics.bbCol + metrics.bbW).toFloat(),
                (metrics.bbRow + metrics.bbH).toFloat()
            )
            
            // Draw the bounding box
            canvas.drawRect(rect, boundingBoxPaint)
            
            // Draw crosshair in the center of the face
            val centerX = rect.centerX()
            val centerY = rect.centerY()
            val crosshairSize = 20f
            
            canvas.drawLine(centerX - crosshairSize, centerY, centerX + crosshairSize, centerY, crosshairPaint)
            canvas.drawLine(centerX, centerY - crosshairSize, centerX, centerY + crosshairSize, crosshairPaint)
        }
        
        // Draw metrics information panel on the left
        drawMetricsPanel(canvas, metrics, true)
        
        // Draw metrics information panel on the right
        drawMetricsPanel(canvas, metrics, false)
    }
    
    private fun drawMetricsPanel(canvas: Canvas, metrics: FaceMetrics, isLeftPanel: Boolean) {
        val metrics = listPairsByPanel(metrics, isLeftPanel)
        
        val padding = 10f
        val lineHeight = 40f
        val panelWidth = width / 3f  // Panel takes 1/3 of the screen width
        
        // Background rectangle
        val panelRect = RectF(
            if (isLeftPanel) 0f else width - panelWidth,
            0f,
            if (isLeftPanel) panelWidth else width.toFloat(),
            padding + metrics.size * lineHeight + padding
        )
        
        canvas.drawRect(panelRect, backgroundPaint)
        
        // Draw each metric
        metrics.forEachIndexed { index, pair ->
            val textY = padding + (index + 1) * lineHeight
            val textX = if (isLeftPanel) padding else width - panelWidth + padding
            
            // Draw metric name
            canvas.drawText(
                "${pair.first}",
                textX,
                textY,
                textPaint
            )
            
            // Draw metric value (aligned to right side of panel)
            val valueText = "${pair.second}"
            val valueWidth = textPaint.measureText(valueText)
            val valueX = if (isLeftPanel) {
                panelWidth - valueWidth - padding
            } else {
                width - valueWidth - padding
            }
            
            canvas.drawText(
                valueText,
                valueX,
                textY,
                textPaint
            )
        }
    }
    
    // Helper function to organize metrics by panel (left or right)
    private fun listPairsByPanel(metrics: FaceMetrics, isLeftPanel: Boolean): List<Pair<String, Any>> {
        return if (isLeftPanel) {
            listOf(
                Pair("Sequence", metrics.sequence),
                Pair("Pitch", metrics.pitch.toInt()),
                Pair("Roll", metrics.roll.toInt()),
                Pair("Yaw", metrics.yaw.toInt()),
                Pair("Quality", metrics.quality.toInt()),
                Pair("Range", metrics.range.toInt()),
                Pair("IPD", metrics.ipd.toInt()),
                Pair("bbRow", metrics.bbRow),
                Pair("bbCol", metrics.bbCol),
                Pair("bbW", metrics.bbW),
                Pair("bbH", metrics.bbH)
            )
        } else {
            listOf(
                Pair("Fusion", metrics.fusion),
                Pair("Face", metrics.face),
                Pair("Depth", metrics.depth),
                Pair("PeriL", metrics.periL),
                Pair("PeriR", metrics.periR),
                Pair("Glasses", metrics.glasses),
                Pair("Blink", metrics.blink),
                Pair("LiveProb", metrics.liveProb)
            )
        }
    }
}
