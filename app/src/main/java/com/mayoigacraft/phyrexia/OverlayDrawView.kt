package com.mayoigacraft.phyrexia

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
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
        const val LINE_WIDTH : Float = 10f
        private const val LINE_OPACITY = 255
        private val LINE_COLOR_RED = Color.argb(
            LINE_OPACITY,
            211,
            47,
            47
        )
        private val LINE_COLOR_PURPLE = Color.argb(
            LINE_OPACITY,
            123,
            31,
            162
        )
        private val LINE_COLOR_INDIGO = Color.argb(
            LINE_OPACITY,
            48,
            63,
            159
        )
        private val LINE_COLOR_LIGHT_BLUE = Color.argb(
            LINE_OPACITY,
            2,
            136,
            209
        )
        private val LINE_COLOR_TEAL = Color.argb(
            LINE_OPACITY,
            0,
            121,
            107
        )
        private val LINE_COLOR_LIGHT_GREEN = Color.argb(
            LINE_OPACITY,
            104,
            159,
            56
        )
        private val LINE_COLOR_LIME = Color.argb(
            LINE_OPACITY,
            175,
            180,
            43
        )
        val LINE_COLORS = arrayListOf<Int>(
            LINE_COLOR_RED,
            LINE_COLOR_PURPLE,
            LINE_COLOR_INDIGO,
            LINE_COLOR_LIGHT_BLUE,
            LINE_COLOR_TEAL,
            LINE_COLOR_LIGHT_GREEN,
            LINE_COLOR_LIME
        )

    }

    init {}

    override fun onDraw(canvas: Canvas) {
        // キャンバスをクリア
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // ペイントする色の設定
        paint.color = Color.argb(255, 255, 0, 255)

        // ペイントストロークの太さを設定
        paint.strokeWidth = LINE_WIDTH

        // Styleのストロークを設定する
        paint.style = Paint.Style.STROKE

        // drawRectを使って矩形を描画する、引数に座標を設定
        // (x1,y1,x2,y2,paint) 左上の座標(x1,y1), 右下の座標(x2,y2)
        canvas.drawRect(300f, 300f, 600f, 600f, paint)
    }

    private var paint: Paint = Paint()
}
