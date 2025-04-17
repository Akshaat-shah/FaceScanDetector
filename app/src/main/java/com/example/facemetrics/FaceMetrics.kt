package com.example.facemetrics

/**
 * Data class representing the facial metrics extracted from a detected face.
 * All measurements are in normalized units unless specified otherwise.
 */
data class FaceMetrics(
    // Positional metrics
    val boundingBox: BoundingBox,
    val interpupillaryDistance: Float,
    val faceWidth: Float,
    val faceHeight: Float,
    val facePosition: Point,
    
    // Orientation metrics (in degrees)
    val pitch: Float,
    val roll: Float,
    val yaw: Float,
    
    // Quality metrics
    val qualityScore: Float,
    val smileConfidence: Float,
    val isSmiling: Boolean,
    val leftEyeOpenConfidence: Float,
    val rightEyeOpenConfidence: Float,
    val areEyesOpen: Boolean,
    val hasGlasses: Boolean,
    
    // Landmarks 
    val landmarks: List<Landmark>,
    
    // Raw confidence value
    val detectionConfidence: Float
) {
    /**
     * Data class representing a bounding box around a detected face.
     * @property left The left coordinate of the bounding box
     * @property top The top coordinate of the bounding box
     * @property right The right coordinate of the bounding box
     * @property bottom The bottom coordinate of the bounding box
     */
    data class BoundingBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val centerX: Float get() = left + width / 2
        val centerY: Float get() = top + height / 2
    }
    
    /**
     * Data class representing a 2D point.
     * @property x The x-coordinate
     * @property y The y-coordinate
     */
    data class Point(
        val x: Float,
        val y: Float
    )
    
    /**
     * Data class representing a facial landmark.
     * @property type The type of landmark (e.g. LEFT_EYE, RIGHT_EYE, NOSE_BASE)
     * @property position The position of the landmark
     */
    data class Landmark(
        val type: LandmarkType,
        val position: Point
    )
    
    /**
     * Enum representing the types of facial landmarks that can be detected.
     */
    enum class LandmarkType {
        LEFT_EYE,
        RIGHT_EYE,
        LEFT_EAR,
        RIGHT_EAR,
        LEFT_CHEEK,
        RIGHT_CHEEK,
        NOSE_BASE,
        MOUTH_LEFT,
        MOUTH_RIGHT,
        MOUTH_BOTTOM
    }
    
    companion object {
        /**
         * Creates a default instance of FaceMetrics with default values.
         * Used when no face is detected or as a placeholder.
         */
        fun createDefault(): FaceMetrics {
            return FaceMetrics(
                boundingBox = BoundingBox(0f, 0f, 0f, 0f),
                interpupillaryDistance = 0f,
                faceWidth = 0f,
                faceHeight = 0f,
                facePosition = Point(0f, 0f),
                pitch = 0f,
                roll = 0f,
                yaw = 0f,
                qualityScore = 0f,
                smileConfidence = 0f,
                isSmiling = false,
                leftEyeOpenConfidence = 0f,
                rightEyeOpenConfidence = 0f,
                areEyesOpen = false,
                hasGlasses = false,
                landmarks = emptyList(),
                detectionConfidence = 0f
            )
        }
    }
}