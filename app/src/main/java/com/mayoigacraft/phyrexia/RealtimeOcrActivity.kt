package com.mayoigacraft.phyrexia

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowManager
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
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mayoigacraft.phyrexia.databinding.ActivityRealtimeOcrBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * リアルタイムOCRアクティビティ
 */
class RealtimeOcrActivity : AppCompatActivity() {
    /**
     * 定数
     */
    companion object {
        /**
         * 権限要求のリクエストコード
         */
        private const val REQUEST_CODE_PERMISSIONS = 10

        /**
         * 要求する権限
         */
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        /**
         * 設定用非公開APIを使用するか
         */
        private const val USE_HIDDEN_CONFIG_API = false
    }

    /**
     * 初期化処理
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        context = this
        viewBinding = ActivityRealtimeOcrBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // タイトルバーを消す
        supportActionBar?.hide()

        // ステータスバーを隠す
        // ナビゲーションバーを隠す
        // 時間経過で再度ナビゲーションバーを隠す
        val view = window.decorView
        view.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        if (isAllPermissionsGranted()) {
            // 権限が付与済みならカメラを起動する
            startCamera()
        } else {
            // 権限を要求
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

    /**
     * 終了処理
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * 権限要求結果のコールバック
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (isAllPermissionsGranted()) {
                // 権限が付与されたらカメラを起動する
                startCamera()
            } else {
                // 権限が付与されなかったらアクティビティを終了する
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * 戻るボタン押下イベント
     */
    fun onBackButtonClicked(view: View) {
        finish()
    }

    /**
     * カメラの処理を開始する
     */
    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun startCamera() {
        val processCameraProvider =
            ProcessCameraProvider.getInstance(this)
        processCameraProvider.addListener({
            // Cameraの細かい設定を非公開APIで設定する
            var configBuilder : Camera2ImplConfig.Builder? = null
            if (USE_HIDDEN_CONFIG_API) {
                configBuilder = setupHiddenConfigApi()
            }

            // プレビューユースケース
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        viewBinding.cameraPreview.surfaceProvider
                    )
                }

            // 分析ユースケース
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
                val cameraProvider: ProcessCameraProvider =
                    processCameraProvider.get()

                // ユースケースをバインド解除
                cameraProvider.unbindAll()

                // ユースケースをバインドする
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )

                // 非公開APIでの設定を有効化する
                if (USE_HIDDEN_CONFIG_API && (configBuilder != null)) {
                    (camera.cameraControl as Camera2CameraControlImpl)
                        .addInteropConfig(configBuilder.build())
                }
            } catch (e: Exception) {
                logE(AppConst.APP_NAME, "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 要求権限がすべて付与されているかの判定
     */
    private fun isAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        val permissionState = ContextCompat.checkSelfPermission(baseContext, it)
        (permissionState == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * テキスト認識に成功した場合の処理
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun onOcrSucceeded(
        imageProxy: ImageProxy,
        textBlocks: List<TextBlock>) {
        // 描画ビューに認識結果をセット
        viewBinding.overlayDrawView.setTextBlocks(textBlocks)
        viewBinding.overlayDrawView.invalidate()
        // 画像をファイルに保存
        saveToFile(imageProxy)
    }

    /**
     * 非公開APIでの設定を行う
     */
    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun setupHiddenConfigApi() : Camera2ImplConfig.Builder {
        val configBuilder = Camera2ImplConfig.Builder()
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
        return configBuilder
    }

    /**
     * 画像をファイルに保存する
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun saveToFile(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            logE(AppConst.APP_NAME, "image is null")
            return
        }
        else if (image.format != ImageFormat.YUV_420_888) {
            logE(AppConst.APP_NAME, "image.format is not YUV_420_888")
            return
        }

        val bitmap = convertImageToBitmap(
            image,
            imageProxy.imageInfo.rotationDegrees)
        if (bitmap == null) {
            logE(AppConst.APP_NAME, "convertImageToBitmap failed")
            return
        }

        val dir = context.getExternalFilesDir(null)
        if (dir == null) {
            logE(AppConst.APP_NAME, "getExternalFileDir failed")
            return
        }

        val filename =
            SimpleDateFormat(AppConst.DATE_FORMAT, Locale.JAPAN)
                .format(System.currentTimeMillis()) +
                    AppConst.JPEG_EXTENSION
        val file = File(dir, filename)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            AppConst.JPEG_QUALITY_FILE_OUTPUT,
            outputStream)
        file.writeBytes(outputStream.toByteArray())

        logI(AppConst.APP_NAME, file.absolutePath)
    }

    /**
     * アクティビティコンテキスト
     */
    private lateinit var context: Context

    /**
     * ビューバインディング
     */
    private lateinit var viewBinding: ActivityRealtimeOcrBinding

    /**
     * カメラ処理スレッド
     */
    private lateinit var cameraExecutor: ExecutorService

    /**
     * テキスト認識機
     */
    private lateinit var textRecognizer: TextRecognizer

}
