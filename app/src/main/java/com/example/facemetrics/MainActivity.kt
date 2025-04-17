package com.example.facemetrics

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.facemetrics.databinding.ActivityMainBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: FaceMetricsViewModel by viewModels()
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    
    companion object {
        private const val TAG = "FaceMetricsApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = 
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        
        // Set up the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Set up the tab layout
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Not implementing tab functionality for now, but could be added here
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        
        // Set up the button click listeners
        binding.submitButton.setOnClickListener {
            viewModel.submitData()
            Toast.makeText(this, "Data submitted", Toast.LENGTH_SHORT).show()
        }
        
        binding.qrButton.setOnClickListener {
            generateQRCode()
        }
        
        binding.saveButton.setOnClickListener {
            saveCurrentMetrics()
        }
        
        // Observe the face metrics
        viewModel.faceMetrics.observe(this, Observer { metrics ->
            binding.faceOverlayView.updateMetrics(metrics)
        })
        
        // Observe detection status
        viewModel.detectionStatus.observe(this, Observer { status ->
            when (status) {
                DetectionStatus.NO_FACE -> 
                    Toast.makeText(this, R.string.no_face_detected, Toast.LENGTH_SHORT).show()
                DetectionStatus.FACE_TOO_FAR -> 
                    Toast.makeText(this, R.string.face_too_far, Toast.LENGTH_SHORT).show()
                DetectionStatus.FACE_TOO_CLOSE -> 
                    Toast.makeText(this, R.string.face_too_close, Toast.LENGTH_SHORT).show()
                DetectionStatus.FACE_MISALIGNED -> 
                    Toast.makeText(this, R.string.align_face, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        })
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()
            
            // Select front camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            
            // Set up the preview use case
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            // Set up the image analysis use case
            val faceAnalyzer = FaceAnalyzer(viewModel)
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, faceAnalyzer)
                }
            
            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    private fun generateQRCode() {
        val metrics = viewModel.faceMetrics.value
        if (metrics == null) {
            Toast.makeText(this, "No metrics to generate QR code", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val metricsData = metrics.toString()
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                metricsData,
                BarcodeFormat.QR_CODE,
                500,
                500
            )
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            
            // Save the QR code
            saveImageToGallery(bitmap, "qrcode")
        } catch (e: Exception) {
            Log.e(TAG, "QR generation failed", e)
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveCurrentMetrics() {
        val metrics = viewModel.faceMetrics.value
        if (metrics == null) {
            Toast.makeText(this, "No metrics to save", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Save metrics data to a file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "FaceMetrics_$timestamp.txt"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/FaceMetrics")
                }
            }
            
            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(metrics.toString().toByteArray())
                }
                Toast.makeText(this, R.string.data_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.error_saving_data, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving metrics data", e)
            Toast.makeText(this, R.string.error_saving_data, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveImageToGallery(bitmap: Bitmap, prefix: String) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "${prefix}_$timestamp.jpg"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FaceMetrics")
            }
        }
        
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e(TAG, "Error saving image", e)
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
