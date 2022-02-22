package com.mayoigacraft.phyrexia

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer

typealias OcrListenerFunc = (
    imageProxy: ImageProxy,
    textBlocks: List<TextBlock>) -> Unit

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
                val textBlocks = parseTextBlocks(
                    textInfo,
                    inputImage.width,
                    inputImage.height,
                    inputImage.rotationDegrees
                )
                listenerFunc(imageProxy, textBlocks)
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                logE(AppConst.APP_NAME, "Text recognition failed", e)
                imageProxy.close()
            }
    }

    /**
     * 認識結果をTextBlockのリストにする
     */
    private fun parseTextBlocks(
        textInfo: Text,
        parentWidth: Int,
        parentHeight: Int,
        parentRotationDegrees: Int
    ): List<TextBlock> {
        var w = parentWidth
        var h = parentHeight
        if ((parentRotationDegrees == 90) || (parentRotationDegrees == 270)) {
            w = parentHeight
            h = parentWidth
        }
        val textBlocks = mutableListOf<TextBlock>()
        for (block in textInfo.textBlocks) {
            val blockText = block.text
            var blockRect = PercentRect(0f,  0f,  0f, 0f)
            if (block.boundingBox != null) {
                val bb = block.boundingBox!!
                blockRect = PercentRect(
                    bb.top.toFloat() / h,
                    bb.left.toFloat() / w,
                    bb.bottom.toFloat() / h,
                    bb.right.toFloat() / w
                )
            }
            val textLines = mutableListOf<TextLine>()
            for (line in block.lines ) {
                val lineText = line.text
                var lineRect = PercentRect(0f, 0f, 0f, 0f)
                if (line.boundingBox != null) {
                    val bb = line.boundingBox!!
                    lineRect = PercentRect(
                        bb.top.toFloat() / h,
                        bb.left.toFloat() / w,
                        bb.bottom.toFloat() / h,
                        bb.right.toFloat() / w
                    )
                }
                textLines.add(TextLine(lineText, lineRect))
            }
            textBlocks.add(TextBlock(blockText, textLines, blockRect))
        }
        return textBlocks
    }
}
