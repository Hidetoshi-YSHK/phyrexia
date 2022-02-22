package com.mayoigacraft.phyrexia

import android.graphics.*
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv

/**
 * Convert YUV_420_888 image to Bitmap
 */
fun convertImageToBitmap(image: Image, rotationDegrees: Int): Bitmap? {
    val data: ByteArray = imageToJpegBytes(image) ?: return null
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
    return if (rotationDegrees == 0) {
        bitmap
    } else {
        rotateBitmap(bitmap, rotationDegrees)
    }
}

/**
 * Rotate bitmap
 */
private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap? {
    val mat = Matrix()
    mat.postRotate(rotationDegrees.toFloat())
    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        mat,
        true
    )
}


/**
 * Convert YUV_420_888 image to jpeg bytes
 */
private fun imageToJpegBytes(image: Image): ByteArray? {
    val nv21 = convertYUV420888toNV21(image) ?: return null

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        image.width,
        image.height,
        null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        Rect(0, 0, yuvImage.width, yuvImage.height),
        AppConst.JPEG_QUALITY_CONVERSION,
        out
    )
    return out.toByteArray()
}

/**
 * Convert YUV_420_888 image to NV21 bytes
 */
private fun convertYUV420888toNV21(image: Image): ByteArray? {
    if (image.format != ImageFormat.YUV_420_888) {
        return null
    }

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

    if ((pixelStride == 2) &&
        (rowStride == width) &&
        (uBuffer[0] == vBuffer[1])) {

        // maybe V an U planes overlap as per NV21,
        // which means vBuffer[1] is alias of uBuffer[0]
        val savePixel = vBuffer[1]
        try {
            vBuffer.put(1, savePixel.inv())
            if (uBuffer[0] == savePixel.inv()) {
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

    // Copy V and U data
    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val vuPos = col * pixelStride + row * rowStride
            nv21[pos++] = vBuffer[vuPos]
            nv21[pos++] = uBuffer[vuPos]
        }
    }

    return nv21
}
