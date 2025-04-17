package com.example.facemetrics

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

private const val TAG = "FaceMetricsViewModel"

/**
 * ViewModel that manages face detection processing and metrics calculation.
 * Handles camera setup, face detection, and provides metrics via LiveData.
 */
class FaceMetricsViewModel(application: Application) : AndroidViewModel(application) {

    // Camera executor for background processing
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // Face analyzer for processing camera frames
    private lateinit var faceAnalyzer: FaceAnalyzer
    
    // LiveData for face metrics
    private val _faceMetrics = MutableLiveData<FaceMetrics>()
    val faceMetrics: LiveData<FaceMetrics> get() = _faceMetrics
    
    // Tracking average metrics for smoothing
    private val recentMetrics = mutableListOf<FaceMetrics>()
    private val maxRecentMetrics = 5 // Number of frames to average
    
    /**
     * Start the camera and face detection process
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        imageCapture: ImageCapture? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // Set up the preview use case
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                // Set up the analyzer use case
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                
                // Initialize the face analyzer
                faceAnalyzer = FaceAnalyzer { metrics ->
                    processFaceMetrics(metrics)
                }
                
                imageAnalysis.setAnalyzer(cameraExecutor, faceAnalyzer)
                
                // Select front camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                
                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                if (imageCapture != null) {
                    // Include image capture use case if provided
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis,
                        imageCapture
                    )
                } else {
                    // Without image capture
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                }
                
            } catch (exc: Exception) {
                Log.e(TAG, "Camera setup failed", exc)
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }
    
    /**
     * Process face metrics with smoothing to reduce jitter
     */
    private fun processFaceMetrics(metrics: FaceMetrics) {
        // Add to recent metrics for averaging
        recentMetrics.add(metrics)
        
        // Keep only the most recent frames
        if (recentMetrics.size > maxRecentMetrics) {
            recentMetrics.removeAt(0)
        }
        
        // If we have multiple frames and the latest has a face detected, 
        // calculate a smoothed version
        if (recentMetrics.size > 1 && metrics.detectionConfidence > 0) {
            val smoothedMetrics = calculateSmoothedMetrics()
            _faceMetrics.postValue(smoothedMetrics)
        } else {
            // Otherwise just use the current metrics
            _faceMetrics.postValue(metrics)
        }
    }
    
    /**
     * Calculate smoothed metrics by averaging recent frames
     * This reduces jitter in the display
     */
    private fun calculateSmoothedMetrics(): FaceMetrics {
        // Only include frames with detected faces
        val validMetrics = recentMetrics.filter { it.detectionConfidence > 0 }
        
        if (validMetrics.isEmpty()) {
            return FaceMetrics.createDefault()
        }
        
        // Use the most recent metrics as the base
        val latest = validMetrics.last()
        
        // If only one valid frame, return it
        if (validMetrics.size == 1) {
            return latest
        }
        
        // Calculate average for each value
        var avgIpd = 0f
        var avgFaceWidth = 0f
        var avgFaceHeight = 0f
        var avgPositionX = 0f
        var avgPositionY = 0f
        var avgPitch = 0f
        var avgRoll = 0f
        var avgYaw = 0f
        var avgQuality = 0f
        var avgSmile = 0f
        var avgLeftEye = 0f
        var avgRightEye = 0f
        
        // Sum all values
        for (metrics in validMetrics) {
            avgIpd += metrics.interpupillaryDistance
            avgFaceWidth += metrics.faceWidth
            avgFaceHeight += metrics.faceHeight
            avgPositionX += metrics.facePosition.x
            avgPositionY += metrics.facePosition.y
            avgPitch += metrics.pitch
            avgRoll += metrics.roll
            avgYaw += metrics.yaw
            avgQuality += metrics.qualityScore
            avgSmile += metrics.smileConfidence
            avgLeftEye += metrics.leftEyeOpenConfidence
            avgRightEye += metrics.rightEyeOpenConfidence
        }
        
        // Calculate averages
        val count = validMetrics.size.toFloat()
        avgIpd /= count
        avgFaceWidth /= count
        avgFaceHeight /= count
        avgPositionX /= count
        avgPositionY /= count
        avgPitch /= count
        avgRoll /= count
        avgYaw /= count
        avgQuality /= count
        avgSmile /= count
        avgLeftEye /= count
        avgRightEye /= count
        
        // Create smoothed bounding box (weighting recent more heavily)
        val recentBox = latest.boundingBox
        val smoothedBox = FaceMetrics.BoundingBox(
            (recentBox.left * 0.7f + avgPositionX * 0.3f - avgFaceWidth / 2).coerceIn(0f, 1f),
            (recentBox.top * 0.7f + avgPositionY * 0.3f - avgFaceHeight / 2).coerceIn(0f, 1f),
            (recentBox.right * 0.7f + avgPositionX * 0.3f + avgFaceWidth / 2).coerceIn(0f, 1f),
            (recentBox.bottom * 0.7f + avgPositionY * 0.3f + avgFaceHeight / 2).coerceIn(0f, 1f)
        )
        
        // Use boolean values from most recent frame
        val isSmiling = avgSmile > 0.7f
        val areEyesOpen = (avgLeftEye + avgRightEye) / 2 > 0.5f
        
        // Use landmarks from most recent frame
        return FaceMetrics(
            boundingBox = smoothedBox,
            interpupillaryDistance = avgIpd,
            faceWidth = avgFaceWidth,
            faceHeight = avgFaceHeight,
            facePosition = FaceMetrics.Point(avgPositionX, avgPositionY),
            pitch = avgPitch,
            roll = avgRoll,
            yaw = avgYaw,
            qualityScore = avgQuality,
            smileConfidence = avgSmile,
            isSmiling = isSmiling,
            leftEyeOpenConfidence = avgLeftEye,
            rightEyeOpenConfidence = avgRightEye,
            areEyesOpen = areEyesOpen,
            hasGlasses = latest.hasGlasses, // Use latest detection for boolean values
            landmarks = latest.landmarks, // Use latest landmarks
            detectionConfidence = latest.detectionConfidence
        )
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        if (::faceAnalyzer.isInitialized) {
            faceAnalyzer.shutdown()
        }
    }
}