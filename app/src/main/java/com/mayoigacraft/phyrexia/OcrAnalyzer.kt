package com.mayoigacraft.phyrexia

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer

typealias OcrListenerFunc = (imageProxy: ImageProxy, textInfo: Text) -> Unit

/**
 * CameraX用の分析ユースケースクラス
 */
class OcrAnalyzer(
    private val context: Context,
    private val textRecognizer: TextRecognizer,
    private val listenerFunc: OcrListenerFunc
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            logE(AppConst.APP_NAME, "image is null")
            return
        }

        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees
        )

        textRecognizer.process(inputImage)
            .addOnSuccessListener { textInfo ->
                listenerFunc(imageProxy, textInfo)
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                logE(AppConst.APP_NAME, "Text recognition failed", e)
                imageProxy.close()
            }
    }
}
