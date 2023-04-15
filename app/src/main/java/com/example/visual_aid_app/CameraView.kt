package com.example.visual_aid_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.Image
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.visual_aid_app.databinding.ActivityCameraViewBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraView : AppCompatActivity() {
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private lateinit var binding: ActivityCameraViewBinding
    var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    private companion object {
        val TAG = CameraView::class.java.simpleName
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    class TextReaderAnalyzer(
        private val textFoundListener: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            imageProxy.image?.let { process(it, imageProxy) }
        }

        private fun process(image: Image, imageProxy: ImageProxy) {
            try {
                readTextFromImage(InputImage.fromMediaImage(image, 90), imageProxy)
            } catch (e: IOException) {
                Log.d(TAG, "Failed to load the image")
                e.printStackTrace()
            }
        }

        private fun readTextFromImage(image: InputImage, imageProxy: ImageProxy) {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { visionText ->
                    processTextFromImage(visionText, imageProxy)
                    imageProxy.close()
                }
                .addOnFailureListener { error ->
                    Log.d(TAG, "Failed to process the image")
                    error.printStackTrace()
                    imageProxy.close()
                }
        }

        private fun processTextFromImage(visionText: Text, imageProxy: ImageProxy) {
            for (block in visionText.textBlocks) {
                // You can access whole block of text using block.text
                for (line in block.lines) {
                    // You can access whole line of text using line.text
                    for (element in line.elements) {
                        textFoundListener(element.text)
                    }
                }
            }
        }
    }


    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    TextReaderAnalyzer(::onTextFound)
                )
            }
    }

    private fun onTextFound(foundText: String)  {
        Log.d(TAG, "We got new text: $foundText")
        playMessage(foundText);

    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            Runnable {
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(binding.cameraPreviewView.surfaceProvider) }
                cameraProviderFuture.get().bind(preview, imageAnalyzer)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun ProcessCameraProvider.bind(
        preview: Preview,
        imageAnalyzer: ImageAnalysis
    ) = try {
        unbindAll()
        bindToLifecycle(
            this@CameraView,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalyzer
        )
    } catch (ise: IllegalStateException) {
        // Thrown if binding is not done from the main thread
        Log.e(TAG, "Binding failed", ise)
    }
    private fun playMessage(textString: String) {
        tts = TextToSpeech(this) { i ->
            if (i == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.US)
                tts?.speak(textString, TextToSpeech.QUEUE_ADD, null)
                cameraExecutor.shutdown()
                finish()
            }
        }

    }

    override fun onStop() {
        super.onStop()
    }
}