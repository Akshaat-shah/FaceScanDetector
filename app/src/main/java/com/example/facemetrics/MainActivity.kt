package com.example.facemetrics

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

/**
 * Main Activity for the Face Metrics application.
 * Handles permissions, camera setup, and UI coordination.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: FaceMetricsViewModel
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var faceOverlayView: FaceOverlayView
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[FaceMetricsViewModel::class.java]
        
        // Get references to views
        cameraPreviewView = findViewById(R.id.camera_preview)
        faceOverlayView = findViewById(R.id.face_overlay)
        
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
        viewModel.startCamera(this, cameraPreviewView)
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
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}