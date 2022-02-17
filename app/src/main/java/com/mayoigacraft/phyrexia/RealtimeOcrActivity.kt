package com.mayoigacraft.phyrexia

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.internal.Camera2CameraControlImpl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mayoigacraft.phyrexia.databinding.ActivityRealtimeOcrBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias OcrListener = (textInfo: Text) -> Unit

class RealtimeOcrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityRealtimeOcrBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (isAllPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        textRecognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

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
            if (isAllPermissionsGranted()) {
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

    companion object {
        private const val APP_NAME = "Phyrexia"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider =
                cameraProviderFuture.get()


            // Cameraの細かい設定を非公開APIで設定する
            var useHiddenConfigFlag = false
            val configBuilder = Camera2ImplConfig.Builder()
            if (useHiddenConfigFlag) {
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_MODE,
                    CaptureRequest.CONTROL_MODE_AUTO
                )
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO
                )
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO
                )
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY,
                    100
                )
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_FRAME_DURATION,
                    16666666
                )
                configBuilder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    20400000
                )
            }

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        viewBinding.cameraPreview.surfaceProvider
                    )
                }

            // Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(640, 480))
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        OcrAnalyzer(textRecognizer, ::onOcrSucceeded))
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                var camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                if (useHiddenConfigFlag) {
                    (camera.cameraControl as Camera2CameraControlImpl)
                        .addInteropConfig(configBuilder.build())
                }

            } catch (e: Exception) {
                Log.e(APP_NAME, "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun isAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        val permissionState = ContextCompat.checkSelfPermission(baseContext, it)
        (permissionState == PackageManager.PERMISSION_GRANTED)
    }

    private fun onOcrSucceeded(text : Text) {
        Log.e(APP_NAME, text.text)
    }

    private lateinit var viewBinding: ActivityRealtimeOcrBinding

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var textRecognizer: TextRecognizer

    private class OcrAnalyzer(
        private val textRecognizer: TextRecognizer,
        private val listener: OcrListener
    ) : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val image = imageProxy.image
            if (image != null) {
                val inputImage = InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees)

                val result = textRecognizer.process(inputImage)
                    .addOnSuccessListener { textInfo ->
                        listener(textInfo)
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e(APP_NAME, "Text recognition failed", e)
                        imageProxy.close()
                    }
            }
        }
    }


}