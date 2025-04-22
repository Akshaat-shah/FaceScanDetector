package com.example.facemetrics

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

private const val TAG = "FaceAnalyzer"

/**
 * Analyzer class for processing camera frames to detect faces.
 * Uses ML Kit for face detection and sends results to a callback.
 */
class FaceAnalyzer(private val onFaceDetected: (FaceMetrics) -> Unit) : ImageAnalysis.Analyzer {

    private val executor = Executors.newSingleThreadExecutor()
    private val metricsCalculator = MetricsCalculator()

    // Configure face detector with all landmarks and classifications
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        // .setMinFaceSize(0.15f)
        .enableTracking()
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()

    

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    // To track processing state and avoid parallel processing of frames
    private var isProcessing = false

    @SuppressLint("UnsafeOptInUsageError")
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Skip if already processing a frame
        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing = false
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        // Process the image with face detector
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    // Get the face with highest confidence if multiple faces detected
                    val primaryFace = faces.maxByOrNull { it.trackingId ?: -1 } ?: faces[0]
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val (imageWidth, imageHeight) = if (rotation == 90 || rotation == 270) {
                        imageProxy.height to imageProxy.width
                    } else {
                        imageProxy.width to imageProxy.height
                    }
                    processFace(primaryFace, imageWidth, imageHeight)
//                    processFace(primaryFace, imageProxy.width, imageProxy.height)
                } else {
                    // No face detected
                    onFaceDetected(FaceMetrics.createDefault())
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                onFaceDetected(FaceMetrics.createDefault())
            }
            .addOnCompleteListener {
                // Always release the imageProxy when done
                imageProxy.close()
                isProcessing = false
            }
    }

    /**
     * Process a detected face and calculate metrics
     */
    private fun processFace(face: Face, imageWidth: Int, imageHeight: Int) {
        // Calculate metrics from face

        val metrics = metricsCalculator.calculateMetrics(face, imageWidth, imageHeight)
        Log.d(TAG, "► Metrics = $metrics")

        Log.d(TAG, """
        ▸ trackingId=${face.trackingId}
        ▸ bounds=${face.boundingBox}
        ▸ roll=${face.headEulerAngleZ}°
        ▸ yaw =${face.headEulerAngleY}°
        ▸ smileProb=${face.smilingProbability}
        ▸ leftEyeOpenProb=${face.leftEyeOpenProbability}
        ▸ rightEyeOpenProb=${face.rightEyeOpenProbability}
    """.trimIndent())

    // 3. (Optional) dump landmark positions
    face.allLandmarks.forEach { lm ->
        Log.d(TAG, "   landmark ${lm.landmarkType}: ${lm.position}")
    }
        onFaceDetected(metrics)
    }

    /**
     * Release resources when analyzer is no longer used
     */
    fun shutdown() {
        executor.shutdown()
        faceDetector.close()
    }
}