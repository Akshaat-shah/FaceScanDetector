package com.example.facemetrics

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class MainActivity : AppCompatActivity() {

    /* ───── View references ───── */
    private lateinit var previewView: PreviewView
    private lateinit var overlay: FaceOverlayView
    private lateinit var captureButton: FloatingActionButton

    /* ───── Camera / ML │ ViewModel & executor ───── */
    private lateinit var viewModel: FaceMetricsViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    /* ───── Runtime‑permission launchers ───── */
    private val camPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(this,
                "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePhoto()
        else Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
    }

    /* ───────────────────────────── onCreate ───────────────────────────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)          // XML uses previewView & overlay IDs

        /* 1) findViewById */
        previewView   = findViewById(R.id.previewView)  // <androidx.camera.view.PreviewView>
        overlay       = findViewById(R.id.overlay)      // <FaceOverlayView>
        captureButton = findViewById(R.id.capture_button)

        /* 2) ViewModel & single‑thread executor */
        viewModel = ViewModelProvider(this)[FaceMetricsViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()

        /* 3) Capture‑button handler */
        captureButton.setOnClickListener {
            if (needStoragePermission()) requestStoragePermission() else takePhoto()
        }

        /* 4) Observe face metrics → overlay */
        viewModel.faceMetrics.observe(this) { metrics ->
            overlay.updateFaceMetrics(metrics)
        }

        /* 5) Camera permission */
        if (hasCamPermission()) startCamera() else camPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /* ───────────────────────────── Camera start ───────────────────────── */

//    private fun startCamera() {
//        /* Create still‑capture use‑case (optional) */
//        imageCapture = ImageCapture.Builder()
//            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//            .build()
//
//        /* Kick off ViewModel’s CameraX pipeline */
//        viewModel.startCamera(
//            lifecycleOwner = this,
//            previewView    = previewView,
//            imageCapture   = imageCapture               // may be null
//        )
//
//        /* Tell overlay how preview is transformed (mirror + rotation) */
//        overlay.setPreviewInfo(
//            mirrored = true,                                    // front camera fixed in VM
//            rotationDegrees = previewView.display.rotation * 90 // 0/90/180/270
//        )
//    }


    private fun startCamera() {
        // … everything you already have …

        viewModel.startCamera(this, previewView, imageCapture)

        // Moved into a post{} block  ↓↓↓
        previewView.post {
            val rotationDeg = previewView.display.rotation * 90  // 0 / 90 / 180 / 270
            overlay.setPreviewInfo(mirrored = true, rotationDegrees = rotationDeg)
        }
    }

    /* ───────────────────────────── Photo capture ─────────────────────── */

    private fun takePhoto() {
        val capture = imageCapture ?: return

        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(img: ImageProxy) {
                imageProxyToBitmap(img)?.let { saveImageToGallery(it) }
                img.close()
            }
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                runOnUiThread { Toast.makeText(this@MainActivity,
                    R.string.photo_save_error, Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun imageProxyToBitmap(proxy: ImageProxy): Bitmap? {
        val buf  = proxy.planes[0].buffer
        val data = ByteArray(buf.remaining()).also { buf.get(it) }
        val bmp  = BitmapFactory.decodeByteArray(data,0,data.size) ?: return null

        val m = Matrix().apply {
            postRotate(proxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f)                            // mirror for front camera
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun saveImageToGallery(bmp: Bitmap) {
        val ts  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fn  = "FaceMetrics_$ts.jpg"
        val cv  = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fn)
            put(MediaStore.MediaColumns.MIME_TYPE,   "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/FaceMetrics")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv) ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cv.clear(); cv.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, cv, null, null)
            }
            runOnUiThread { Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            Log.e(TAG, "Saving image failed: ${e.message}", e)
            runOnUiThread { Toast.makeText(this, R.string.photo_save_error, Toast.LENGTH_SHORT).show() }
        }
    }

    /* ───────────────────────── permissions helpers ───────────────────── */

    private fun hasCamPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun needStoragePermission() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

    private fun requestStoragePermission() =
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /* ───────────────────────── lifecycle ─────────────────────────────── */

//    override fun onResume() {
//        super.onResume()
//        if (::overlay.isInitialized) {
//            overlay.setPreviewInfo(
//                mirrored = true,
//                rotationDegrees = previewView.display.rotation * 90
//            )
//        }
//    }

    override fun onResume() {
        super.onResume()

        // Run after PreviewView is attached → display is non‑null
        previewView.post {
            val rotationDeg = previewView.display.rotation * 90
            overlay.setPreviewInfo(
                mirrored = true,
                rotationDegrees = rotationDeg
            )
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
