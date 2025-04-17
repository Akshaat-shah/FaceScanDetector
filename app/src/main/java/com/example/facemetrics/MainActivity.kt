package com.example.facemetrics

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "MainActivity"

/**
 * Main Activity for the Face Metrics application.
 * Handles permissions, camera setup, and UI coordination.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: FaceMetricsViewModel
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var faceOverlayView: FaceOverlayView
    private lateinit var captureButton: FloatingActionButton
    
    // Image capture use case
    private var imageCapture: ImageCapture? = null
    
    // Executor for background tasks
    private lateinit var cameraExecutor: ExecutorService
    
    // Permission request launcher for camera
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start camera
            startCamera()
        } else {
            // Permission denied, show toast and finish activity
            Toast.makeText(this,
                "Camera permission is required for this app to function",
                Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    // Permission request launcher for storage
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, take picture
            takePhoto()
        } else {
            // Permission denied
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[FaceMetricsViewModel::class.java]
        
        // Get references to views
        cameraPreviewView = findViewById(R.id.camera_preview)
        faceOverlayView = findViewById(R.id.face_overlay)
        captureButton = findViewById(R.id.capture_button)
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Set up capture button click listener
        captureButton.setOnClickListener {
            if (checkStoragePermission()) {
                takePhoto()
            } else {
                requestStoragePermission()
            }
        }
        
        // Observe face metrics
        viewModel.faceMetrics.observe(this) { metrics ->
            faceOverlayView.updateFaceMetrics(metrics)
        }
        
        // Check and request camera permission
        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }
    
    /**
     * Start camera preview and face detection
     */
    private fun startCamera() {
        // Initialize image capture use case
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
            
        // Let the ViewModel handle camera setup for face detection
        viewModel.startCamera(this, cameraPreviewView, imageCapture)
    }
    
    /**
     * Take a photo and save it to gallery
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        // Create a callback for the image capture
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Convert the image to bitmap
                    val bitmap = imageProxyToBitmap(image)
                    
                    // Save bitmap to gallery
                    if (bitmap != null) {
                        saveImageToGallery(bitmap)
                    }
                    
                    // Close the image
                    image.close()
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.photo_save_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }
    
    /**
     * Convert ImageProxy to Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        // Convert to bitmap
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        
        // Rotate bitmap if needed (front camera usually needs rotation)
        val matrix = Matrix()
        // For front camera mirroring and rotation
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        matrix.postScale(-1f, 1f) // Mirror horizontally for front camera
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    /**
     * Save bitmap to gallery
     */
    private fun saveImageToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val imageFileName = "FaceMetrics_$timestamp.jpg"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/FaceMetrics")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        imageUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                
                runOnUiThread {
                    Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, R.string.photo_save_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request camera permission
     */
    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    /**
     * Check if storage permission is granted
     */
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Android 10+ uses scoped storage, no permission needed
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == 
                    PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request storage permission
     */
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permission for Android 10+
            takePhoto()
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}