package com.example.facemetrics

import android.graphics.PointF
import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Calculator class that converts ML Kit Face objects to FaceMetrics.
 * Handles all metric calculations from raw face detection data.
 */
class MetricsCalculator {

    /**
     * Calculates comprehensive face metrics from a detected face
     * @param face The face object from ML Kit detection
     * @param imageWidth Width of the image being processed
     * @param imageHeight Height of the image being processed
     * @return FaceMetrics containing all calculated metrics
     */
    fun calculateMetrics(face: Face, imageWidth: Int, imageHeight: Int): FaceMetrics {
        // Handle rotation-adjusted image dimensions if needed
        val boundingBox = face.boundingBox
        val normalizedBoundingBox = normalizeRect(boundingBox, imageWidth, imageHeight)

        val ipd = calculateInterpupillaryDistance(face, imageWidth, imageHeight)

        val faceCenter = calculateFaceCenter(boundingBox)
        val normalizedFaceCenter = normalizeFaceCenter(faceCenter, imageWidth, imageHeight)

        val faceWidth = boundingBox.width() / imageWidth.toFloat()
        val faceHeight = boundingBox.height() / imageHeight.toFloat()

        val pitch = face.headEulerAngleX
        val roll = face.headEulerAngleZ
        val yaw = face.headEulerAngleY

        val smileConfidence = face.smilingProbability ?: 0f
        val isSmiling = smileConfidence > 0.7f

        val leftEyeOpenConfidence = face.leftEyeOpenProbability ?: 0f
        val rightEyeOpenConfidence = face.rightEyeOpenProbability ?: 0f
        val areEyesOpen = (leftEyeOpenConfidence + rightEyeOpenConfidence) / 2 > 0.5f

        val hasGlasses = detectGlasses(face)

        val qualityScore = calculateQualityScore(
            face,
            leftEyeOpenConfidence,
            rightEyeOpenConfidence,
            smileConfidence,
            pitch, roll, yaw
        )

        val landmarks = extractLandmarks(face, imageWidth, imageHeight)

        return FaceMetrics(
            boundingBox = FaceMetrics.BoundingBox(
                normalizedBoundingBox.left,
                normalizedBoundingBox.top,
                normalizedBoundingBox.right,
                normalizedBoundingBox.bottom
            ),
            interpupillaryDistance = ipd,
            faceWidth = faceWidth,
            faceHeight = faceHeight,
            facePosition = FaceMetrics.Point(normalizedFaceCenter.x, normalizedFaceCenter.y),
            pitch = pitch,
            roll = roll,
            yaw = yaw,
            qualityScore = qualityScore,
            smileConfidence = smileConfidence,
            isSmiling = isSmiling,
            leftEyeOpenConfidence = leftEyeOpenConfidence,
            rightEyeOpenConfidence = rightEyeOpenConfidence,
            areEyesOpen = areEyesOpen,
            hasGlasses = hasGlasses,
            landmarks = landmarks,
            detectionConfidence = 1.0f
        )
    }

    private fun calculateInterpupillaryDistance(face: Face, imageWidth: Int, imageHeight: Int): Float {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

        return if (leftEye != null && rightEye != null) {
            val leftPoint = leftEye.position
            val rightPoint = rightEye.position
            val deltaX = rightPoint.x - leftPoint.x
            val deltaY = rightPoint.y - leftPoint.y
            val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
            distance / imageWidth
        } else {
            0f
        }
    }

    private fun calculateFaceCenter(boundingBox: Rect): PointF {
        return PointF(boundingBox.exactCenterX(), boundingBox.exactCenterY())
    }

    private fun normalizeFaceCenter(center: PointF, imageWidth: Int, imageHeight: Int): PointF {
        return PointF((center.x / imageWidth) - 0.5f, (center.y / imageHeight) - 0.5f)
    }

    private fun normalizeRect(rect: Rect, imageWidth: Int, imageHeight: Int): RectF {
        return RectF(
            rect.left / imageWidth.toFloat(),
            rect.top / imageHeight.toFloat(),
            rect.right / imageWidth.toFloat(),
            rect.bottom / imageHeight.toFloat()
        )
    }

    private fun calculateQualityScore(
        face: Face,
        leftEyeOpenConfidence: Float,
        rightEyeOpenConfidence: Float,
        smileConfidence: Float,
        pitch: Float,
        roll: Float,
        yaw: Float
    ): Float {
        val orientationWeight = 0.4f
        val eyeWeight = 0.3f
        val smileWeight = 0.1f
        val landmarkWeight = 0.2f

        val orientationScore = 1.0f - (
                (abs(pitch) / 45f).coerceAtMost(1f) * 0.4f +
                        (abs(roll) / 45f).coerceAtMost(1f) * 0.3f +
                        (abs(yaw) / 45f).coerceAtMost(1f) * 0.3f
                )

        val eyeScore = (leftEyeOpenConfidence + rightEyeOpenConfidence) / 2
        val smileScore = 1.0f - abs(smileConfidence - 0.5f) * 2
        val landmarkScore = if (face.allLandmarks.size >= 5) 1.0f else face.allLandmarks.size / 5f

        return (orientationScore * orientationWeight +
                eyeScore * eyeWeight +
                smileScore * smileWeight +
                landmarkScore * landmarkWeight).coerceIn(0f, 1f)
    }

    private fun extractLandmarks(face: Face, imageWidth: Int, imageHeight: Int): List<FaceMetrics.Landmark> {
        val landmarkMap = mapOf(
            FaceLandmark.LEFT_EYE to FaceMetrics.LandmarkType.LEFT_EYE,
            FaceLandmark.RIGHT_EYE to FaceMetrics.LandmarkType.RIGHT_EYE,
            FaceLandmark.LEFT_EAR to FaceMetrics.LandmarkType.LEFT_EAR,
            FaceLandmark.RIGHT_EAR to FaceMetrics.LandmarkType.RIGHT_EAR,
            FaceLandmark.LEFT_CHEEK to FaceMetrics.LandmarkType.LEFT_CHEEK,
            FaceLandmark.RIGHT_CHEEK to FaceMetrics.LandmarkType.RIGHT_CHEEK,
            FaceLandmark.NOSE_BASE to FaceMetrics.LandmarkType.NOSE_BASE,
            FaceLandmark.MOUTH_LEFT to FaceMetrics.LandmarkType.MOUTH_LEFT,
            FaceLandmark.MOUTH_RIGHT to FaceMetrics.LandmarkType.MOUTH_RIGHT,
            FaceLandmark.MOUTH_BOTTOM to FaceMetrics.LandmarkType.MOUTH_BOTTOM
        )

        val landmarks = mutableListOf<FaceMetrics.Landmark>()

        for ((mlkitType, metricType) in landmarkMap) {
            face.getLandmark(mlkitType)?.let { landmark ->
                val position = landmark.position
                val normalizedX = position.x / imageWidth
                val normalizedY = position.y / imageHeight

                landmarks.add(
                    FaceMetrics.Landmark(
                        metricType,
                        FaceMetrics.Point(normalizedX, normalizedY)
                    )
                )
            }
        }

        return landmarks
    }

    data class RectF(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    private fun detectGlasses(face: Face): Boolean {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

        if (leftEye == null || rightEye == null) return false

        val leftEyeOpen = face.leftEyeOpenProbability ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f

        val hasLeftEar = face.getLandmark(FaceLandmark.LEFT_EAR) != null
        val hasRightEar = face.getLandmark(FaceLandmark.RIGHT_EAR) != null

        return (hasLeftEar || hasRightEar) && leftEyeOpen > 0.5f && rightEyeOpen > 0.5f
    }
}