package com.example.facemetrics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.face.Face

enum class DetectionStatus {
    NO_FACE,
    FACE_DETECTED,
    FACE_TOO_FAR,
    FACE_TOO_CLOSE,
    FACE_MISALIGNED
}

data class FaceMetrics(
    val sequence: Int = 0,
    val pitch: Float = 0f,   // Head up/down angle, positive is down
    val roll: Float = 0f,    // Head tilt left/right, positive is to the right
    val yaw: Float = 0f,     // Head turning left/right, positive is to the right
    val quality: Float = 0f, // Face detection quality
    val range: Float = 0f,   // Distance from camera
    val ipd: Float = 0f,     // Interpupillary distance
    val bbRow: Int = 0,      // Bounding box row coordinate
    val bbCol: Int = 0,      // Bounding box column coordinate
    val bbW: Int = 0,        // Bounding box width
    val bbH: Int = 0,        // Bounding box height
    val fusion: Int = 0,     // Fusion score (for liveness detection)
    val face: Int = 0,       // Face detection confidence
    val depth: Int = 0,      // Depth estimation
    val periL: Int = 0,      // Left periocular score
    val periR: Int = 0,      // Right periocular score
    val glasses: Int = 0,    // Glasses detection (0=no, 1=yes)
    val blink: Int = 0,      // Blink detection
    val liveProb: Int = 0    // Liveness probability
) {
    override fun toString(): String {
        return """
            Sequence: $sequence
            Pitch: $pitch
            Roll: $roll
            Yaw: $yaw
            Quality: $quality
            Range: $range
            IPD: $ipd
            bbRow: $bbRow
            bbCol: $bbCol
            bbW: $bbW
            bbH: $bbH
            Fusion: $fusion
            Face: $face
            Depth: $depth
            PeriL: $periL
            PeriR: $periR
            Glasses: $glasses
            Blink: $blink
            LiveProb: $liveProb
        """.trimIndent()
    }
}

class FaceMetricsViewModel : ViewModel() {
    
    private val _faceMetrics = MutableLiveData<FaceMetrics>()
    val faceMetrics: LiveData<FaceMetrics> = _faceMetrics
    
    private val _detectionStatus = MutableLiveData<DetectionStatus>()
    val detectionStatus: LiveData<DetectionStatus> = _detectionStatus
    
    private val metricsCalculator = MetricsCalculator()
    private var sequenceNumber = 0
    
    fun processFace(face: Face?, imageWidth: Int, imageHeight: Int) {
        if (face == null) {
            _detectionStatus.postValue(DetectionStatus.NO_FACE)
            return
        }
        
        // Update sequence number
        sequenceNumber++
        
        // Calculate metrics
        val metrics = metricsCalculator.calculateMetrics(face, sequenceNumber, imageWidth, imageHeight)
        _faceMetrics.postValue(metrics)
        
        // Determine detection status
        updateDetectionStatus(metrics)
    }
    
    private fun updateDetectionStatus(metrics: FaceMetrics) {
        // Example thresholds - these should be calibrated based on real usage
        when {
            metrics.range > 150 -> _detectionStatus.postValue(DetectionStatus.FACE_TOO_FAR)
            metrics.range < 50 -> _detectionStatus.postValue(DetectionStatus.FACE_TOO_CLOSE)
            // Check for face alignment (pitch, roll, yaw within acceptable range)
            Math.abs(metrics.pitch) > 20f || 
            Math.abs(metrics.roll) > 20f || 
            Math.abs(metrics.yaw) > 20f -> _detectionStatus.postValue(DetectionStatus.FACE_MISALIGNED)
            else -> _detectionStatus.postValue(DetectionStatus.FACE_DETECTED)
        }
    }
    
    fun submitData() {
        // This would typically send data to a server or process it further
        // For this demo, we're just incrementing the sequence number
        sequenceNumber++
        
        // Update the current metrics with the new sequence number
        _faceMetrics.value?.let {
            val updated = it.copy(sequence = sequenceNumber)
            _faceMetrics.postValue(updated)
        }
    }
}
