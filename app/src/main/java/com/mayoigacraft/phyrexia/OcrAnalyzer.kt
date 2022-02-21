package com.mayoigacraft.phyrexia

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

typealias OcrListener = (textInfo: Text) -> Unit

/**
 * CameraX用の分析ユースケースクラス
 */
class OcrAnalyzer(
    private val context: Context,
    private val textRecognizer: TextRecognizer,
    private val listener: OcrListener
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            logE(AppConst.APP_NAME, "image is null")
            return
        }

        if (image.format != ImageFormat.YUV_420_888) {
            logE(AppConst.APP_NAME, "image.format is not YUV_420_888")
            return
        }

        val dir = context.getExternalFilesDir(null)
        if (dir == null) {
            logE(AppConst.APP_NAME, "dir is null")
        }
        else {
            logE(AppConst.APP_NAME, dir.absolutePath)
            val filename =
                SimpleDateFormat(AppConst.DATE_FORMAT, Locale.JAPAN)
                    .format(System.currentTimeMillis()) +
                        AppConst.IMAGE_EXTENSION
            val file = File(dir, filename)
            val bitmap = convertImageToBitmap(
                image,
                imageProxy.imageInfo.rotationDegrees)
            if (bitmap == null) {
                logE(AppConst.APP_NAME, "bitmap is null")
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
                logE(AppConst.APP_NAME, "Text recognition failed", e)
                imageProxy.close()
            }
    }
}
