package com.mayoigacraft.phyrexia

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ReadOnlyBufferException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.experimental.inv


typealias OcrListener = (textInfo: Text) -> Unit

class RealtimeOcrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
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
        private const val USE_HIDDEN_CONFIG_API = false
        private const val DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val IMAGE_EXTENSION = ".jpg"

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
            val configBuilder = Camera2ImplConfig.Builder()
            if (USE_HIDDEN_CONFIG_API) {
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

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        viewBinding.cameraPreview.surfaceProvider
                    )
                }

            // Analyzer use case
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                )
                .setTargetResolution(Size(640, 480))
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        OcrAnalyzer(context, textRecognizer, ::onOcrSucceeded)
                    )
                }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )

                if (USE_HIDDEN_CONFIG_API) {
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

    private fun onOcrSucceeded(text: Text) {
        Log.e(APP_NAME, text.text)
    }

    private lateinit var context: Context

    private lateinit var viewBinding: ActivityRealtimeOcrBinding

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var textRecognizer: TextRecognizer


    private class OcrAnalyzer(
        private val context: Context,
        private val textRecognizer: TextRecognizer,
        private val listener: OcrListener
    ) : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val image = imageProxy.image
            if (image == null) {
                Log.e(APP_NAME, "image is null")
                return
            }

            if (image.format != ImageFormat.YUV_420_888) {
                Log.e(APP_NAME, "image.format is not YUV_420_888")
                return
            }

            val dir = context.getExternalFilesDir(null)
            if (dir == null) {
                Log.e(APP_NAME, "dir is null")
            }
            else {
                Log.e(APP_NAME, dir.absolutePath)
                val filename =
                    SimpleDateFormat(DATE_FORMAT, Locale.JAPAN)
                    .format(System.currentTimeMillis()) + IMAGE_EXTENSION
                val file = File(dir, filename)
                val bitmap = convertImageToBitmap(
                    image,
                    imageProxy.imageInfo.rotationDegrees)
                if (bitmap == null) {
                    Log.e(APP_NAME, "bitmap is null")
                }
                else {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        50,
                        outputStream)
                    file.writeBytes(outputStream.toByteArray())
                }
            }

            val inputImage = InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )

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

    class OverlayDrawView(
        context: Context?,
        attributeSet: AttributeSet?
    ) : View(context, attributeSet) {

        private var paint: Paint = Paint()

        // 描画するラインの太さ
        private val lineStrokeWidth = 20f

        init {
        }

        override fun onDraw(canvas: Canvas) {

            // ペイントする色の設定
            paint.color = Color.argb(255, 255, 0, 255)

            // ペイントストロークの太さを設定
            paint.strokeWidth = lineStrokeWidth

            // Styleのストロークを設定する
            paint.style = Paint.Style.STROKE

            // drawRectを使って矩形を描画する、引数に座標を設定
            // (x1,y1,x2,y2,paint) 左上の座標(x1,y1), 右下の座標(x2,y2)
            canvas.drawRect(300f, 300f, 600f, 600f, paint)
        }
    }
}

private fun convertImageToBitmap(image: Image, rotationDegrees: Int): Bitmap? {
    val data: ByteArray = image.toJpegBytes()
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
    return if (rotationDegrees == 0) {
        bitmap
    } else {
        rotateBitmap(bitmap, rotationDegrees)
    }
}

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap? {
    val mat = Matrix()
    mat.postRotate(rotationDegrees.toFloat())
    return Bitmap.createBitmap(
        bitmap, 0, 0,
        bitmap.width, bitmap.height, mat, true
    )
}

fun Image.toJpegBytes(): ByteArray {
    val nv21 = convertYUV420888toNV21(this) ?: return ByteArray(0)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        this.width,
        this.height,
        null
    )
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        Rect(0, 0, yuvImage.width, yuvImage.height),
        85,
        out
    )
    return out.toByteArray()
}

fun convertYUV420888toNV21(image: Image): ByteArray? {
    val width = image.width
    val height = image.height
    val ySize = width * height
    val uvSize = width * height / 4
    val nv21 = ByteArray(ySize + uvSize * 2)
    val yBuffer = image.planes[0].buffer // Y
    val uBuffer = image.planes[1].buffer // U
    val vBuffer = image.planes[2].buffer // V
    var rowStride = image.planes[0].rowStride
    assert(image.planes[0].pixelStride == 1)
    var pos = 0
    if (rowStride == width) { // likely
        yBuffer[nv21, 0, ySize]
        pos += ySize
    } else {
        var yBufferPos = -rowStride.toLong() // not an actual position
        while (pos < ySize) {
            yBufferPos += rowStride.toLong()
            yBuffer.position(yBufferPos.toInt())
            yBuffer[nv21, pos, width]
            pos += width
        }
    }
    rowStride = image.planes[2].rowStride
    val pixelStride = image.planes[2].pixelStride
    assert(rowStride == image.planes[1].rowStride)
    assert(pixelStride == image.planes[1].pixelStride)
    if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
        // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
        val savePixel = vBuffer[1]
        try {
            vBuffer.put(1, savePixel.inv() as Byte)
            if (uBuffer[0] == savePixel.inv() as Byte) {
                vBuffer.put(1, savePixel)
                vBuffer.position(0)
                uBuffer.position(0)
                vBuffer[nv21, ySize, 1]
                uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                return nv21 // shortcut
            }
        } catch (ex: ReadOnlyBufferException) {
            // unfortunately, we cannot check if vBuffer and uBuffer overlap
        }

        // unfortunately, the check failed. We must save U and V pixel by pixel
        vBuffer.put(1, savePixel)
    }

    // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
    // but performance gain would be less significant
    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val vuPos = col * pixelStride + row * rowStride
            nv21[pos++] = vBuffer[vuPos]
            nv21[pos++] = uBuffer[vuPos]
        }
    }

    return nv21
}