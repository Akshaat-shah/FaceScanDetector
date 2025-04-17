package com.example.facemetrics

import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MetricsCalculator {
    
    companion object {
        private const val DEFAULT_FACE_SIZE = 100 // Used for scaling metrics
        private const val DEFAULT_QUALITY = 50f
        private const val DEFAULT_DEPTH = 100
        private const val QUALITY_SCALE = 100f // Scale factor for quality metrics
    }
    
    // Calculate all metrics from a detected face
    fun calculateMetrics(face: Face, sequence: Int, imageWidth: Int, imageHeight: Int): FaceMetrics {
        // Get facial orientation (Euler angles)
        val pitch = face.headEulerAngleX // Head up/down
        val roll = face.headEulerAngleZ  // Head tilt
        val yaw = face.headEulerAngleY   // Head left/right
        
        // Get face bounding box
        val boundingBox = face.boundingBox
        val bbW = boundingBox.width()
        val bbH = boundingBox.height()
        val bbRow = boundingBox.top
        val bbCol = boundingBox.left
        
        // Calculate quality metric based on face size, orientation and tracking confidence
        val sizeQuality = calculateSizeQuality(bbW, bbH, imageWidth, imageHeight)
        val orientationQuality = calculateOrientationQuality(pitch, roll, yaw)
        val trackingQuality = face.trackingId?.let { 100f } ?: 50f
        val quality = (sizeQuality + orientationQuality + trackingQuality) / 3f
        
        // Calculate range (distance from camera) - this is a rough estimate based on face size
        val range = calculateRange(bbW, bbH, imageWidth, imageHeight)
        
        // Calculate interpupillary distance if landmarks available
        val ipd = calculateIPD(face, imageWidth)
        
        // Calculate other metrics (these would normally come from more advanced algorithms)
        // For demo purposes, we'll use simulated values based on available data
        val fusion = simulateFusionScore(quality)
        val faceConfidence = if (quality > 70f) 1 else 0
        val depth = DEFAULT_DEPTH
        val periL = simulatePeriodicScore(face, true)
        val periR = simulatePeriodicScore(face, false)
        val glasses = if (face.rightEyeOpenProbability != null && 
                         face.rightEyeOpenProbability!! < 0.2f && 
                         face.leftEyeOpenProbability != null && 
                         face.leftEyeOpenProbability!! < 0.2f) 1 else 0
        val blink = if (face.rightEyeOpenProbability != null && 
                       face.leftEyeOpenProbability != null) {
            if (face.rightEyeOpenProbability!! < 0.3f || 
                face.leftEyeOpenProbability!! < 0.3f) 1 else 0
        } else 0
        val liveProb = simulateLiveProb(quality)
        
        return FaceMetrics(
            sequence = sequence,
            pitch = pitch,
            roll = roll,
            yaw = yaw,
            quality = quality,
            range = range,
            ipd = ipd,
            bbRow = bbRow,
            bbCol = bbCol,
            bbW = bbW,
            bbH = bbH,
            fusion = fusion,
            face = faceConfidence,
            depth = depth,
            periL = periL,
            periR = periR,
            glasses = glasses,
            blink = blink,
            liveProb = liveProb
        )
    }
    
    // Calculate quality based on face size relative to image
    private fun calculateSizeQuality(faceWidth: Int, faceHeight: Int, imageWidth: Int, imageHeight: Int): Float {
        val faceSizeRatio = (faceWidth * faceHeight).toFloat() / (imageWidth * imageHeight).toFloat()
        // Optimal ratio is around 0.15-0.25 (face takes up about 20% of the frame)
        return when {
            faceSizeRatio < 0.05f -> 40f * (faceSizeRatio / 0.05f) // Too small
            faceSizeRatio > 0.5f -> 40f * (1 - (faceSizeRatio - 0.5f) / 0.5f) // Too big
            else -> 80f * (1 - abs(faceSizeRatio - 0.2f) / 0.15f) // Good range
        }.coerceIn(10f, 100f)
    }
    
    // Calculate quality based on face orientation
    private fun calculateOrientationQuality(pitch: Float, roll: Float, yaw: Float): Float {
        // Ideal orientation is near 0 for all angles
        val pitchQuality = 100f * (1 - min(abs(pitch) / 45f, 1f))
        val rollQuality = 100f * (1 - min(abs(roll) / 45f, 1f))
        val yawQuality = 100f * (1 - min(abs(yaw) / 45f, 1f))
        
        return (pitchQuality + rollQuality + yawQuality) / 3f
    }
    
    // Estimate distance from camera based on face size
    private fun calculateRange(faceWidth: Int, faceHeight: Int, imageWidth: Int, imageHeight: Int): Float {
        // This is a very rough estimate - would need calibration in real usage
        val faceSize = max(faceWidth, faceHeight)
        val imageSize = min(imageWidth, imageHeight)
        return 100f * (imageSize.toFloat() / faceSize.toFloat())
    }
    
    // Calculate interpupillary distance if eye landmarks are available
    private fun calculateIPD(face: Face, imageWidth: Int): Float {
        // If landmarks are available, calculate actual IPD
        val landmarks = face.allLandmarks
        
        // Find left and right eye landmarks
        val leftEye = landmarks.find { it.landmarkType == com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE }
        val rightEye = landmarks.find { it.landmarkType == com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE }
        
        return if (leftEye != null && rightEye != null) {
            val leftPoint = leftEye.position
            val rightPoint = rightEye.position
            val distance = sqrt(
                (leftPoint.x - rightPoint.x) * (leftPoint.x - rightPoint.x) +
                (leftPoint.y - rightPoint.y) * (leftPoint.y - rightPoint.y)
            )
            
            // Normalize by image width
            (distance / imageWidth) * 100f
        } else {
            // Default or estimated value if landmarks not available
            face.boundingBox.width() * 0.3f
        }
    }
    
    // Simulate fusion score based on quality
    private fun simulateFusionScore(quality: Float): Int {
        return if (quality > 80f) 20 else ((quality / 80f) * 20f).toInt()
    }
    
    // Simulate periocular score
    private fun simulatePeriodicScore(face: Face, isLeft: Boolean): Int {
        val eyeOpenProb = if (isLeft) face.leftEyeOpenProbability else face.rightEyeOpenProbability
        return if (eyeOpenProb != null && eyeOpenProb > 0.7f) 100 else 80
    }
    
    // Simulate liveness probability
    private fun simulateLiveProb(quality: Float): Int {
        return if (quality > 70f) 20 else 10
    }
}
