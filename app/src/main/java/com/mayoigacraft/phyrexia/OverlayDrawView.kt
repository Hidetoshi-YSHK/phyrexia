package com.mayoigacraft.phyrexia

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

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
