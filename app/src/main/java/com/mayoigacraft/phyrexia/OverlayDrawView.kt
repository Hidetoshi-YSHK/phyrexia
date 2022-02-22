package com.mayoigacraft.phyrexia

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * カメラプレビューの上に配置する描画ビュー
 */
class OverlayDrawView(
    context: Context?,
    attributeSet: AttributeSet?
) : View(context, attributeSet) {

    companion object {
        const val LINE_WIDTH_TEXT_BLOCK : Float = 10f
        const val LINE_WIDTH_TEXT_LINE : Float = 5f
        private const val OPACITY_TEXT_BLOCK = 255
        private const val OPACITY_TEXT_LINE = 128

        private val COLOR_RED_TEXT_BLOCK =
            Color.argb(OPACITY_TEXT_BLOCK, 211, 47, 47)
        private val COLOR_PURPLE_TEXT_BLOCK =
            Color.argb(OPACITY_TEXT_BLOCK, 123, 31, 162)
        private val COLOR_INDIGO_TEXT_BLOCK =
            Color.argb(OPACITY_TEXT_BLOCK, 48, 63, 159)
        private val COLOR_LIGHT_BLUE_TEXT_BLOCK =
            Color.argb(OPACITY_TEXT_BLOCK, 2, 136, 209)
        private val COLOR_TEAL_TEXT_BLOCK =
            Color.argb(OPACITY_TEXT_BLOCK, 0, 121, 107)
        private val COLOR_LIGHT_GREEN_TEXT_BLOCK =
            Color.argb(OPACITY_TEXT_BLOCK, 104, 159, 56)
        private val COLOR_LIME_TEXT_BLOCK =
            Color.argb(OPACITY_TEXT_BLOCK, 175, 180, 43)
        val COLORS_TEXT_BLOCK = arrayListOf<Int>(
            COLOR_RED_TEXT_BLOCK,
            COLOR_PURPLE_TEXT_BLOCK,
            COLOR_INDIGO_TEXT_BLOCK,
            COLOR_LIGHT_BLUE_TEXT_BLOCK,
            COLOR_TEAL_TEXT_BLOCK,
            COLOR_LIGHT_GREEN_TEXT_BLOCK,
            COLOR_LIME_TEXT_BLOCK
        )

        private val COLOR_RED_TEXT_LINE =
            Color.argb(OPACITY_TEXT_LINE, 211, 47, 47)
        private val COLOR_PURPLE_TEXT_LINE =
            Color.argb(OPACITY_TEXT_LINE, 123, 31, 162)
        private val COLOR_INDIGO_TEXT_LINE =
            Color.argb(OPACITY_TEXT_LINE, 48, 63, 159)
        private val COLOR_LIGHT_BLUE_TEXT_LINE =
            Color.argb(OPACITY_TEXT_LINE, 2, 136, 209)
        private val COLOR_TEAL_TEXT_LINE =
            Color.argb(OPACITY_TEXT_LINE, 0, 121, 107)
        private val COLOR_LIGHT_GREEN_TEXT_LINE =
            Color.argb(OPACITY_TEXT_LINE, 104, 159, 56)
        private val COLOR_LIME_TEXT_LINE =
            Color.argb(OPACITY_TEXT_LINE, 175, 180, 43)
        val COLORS_TEXT_LINE = arrayListOf<Int>(
            COLOR_RED_TEXT_LINE,
            COLOR_PURPLE_TEXT_LINE,
            COLOR_INDIGO_TEXT_LINE,
            COLOR_LIGHT_BLUE_TEXT_LINE,
            COLOR_TEAL_TEXT_LINE,
            COLOR_LIGHT_GREEN_TEXT_LINE,
            COLOR_LIME_TEXT_LINE
        )

        const val COLOR_NUM = 7;
    }

    init {}

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        for ((i, textBlock) in textBlocks.withIndex()) {
            val colorIndex = i % COLOR_NUM
            paint.color = COLORS_TEXT_BLOCK[colorIndex]
            paint.strokeWidth = LINE_WIDTH_TEXT_BLOCK
            paint.style = Paint.Style.STROKE
            canvas.drawRect(
                textBlock.percentRect.left * w,
                textBlock.percentRect.top * h,
                textBlock.percentRect.right * w,
                textBlock.percentRect.bottom * h,
                paint
            )
        }
    }

    /**
     * テキストブロックのリストを設定する
     */
    fun setTextBlocks(textBlocks: List<TextBlock>) {
        this.textBlocks = textBlocks
    }

    private var screenWidth: Int = 0

    private var screenHeight: Int = 0

    private var paint: Paint = Paint()

    private var textBlocks: List<TextBlock> = listOf<TextBlock>()
}
