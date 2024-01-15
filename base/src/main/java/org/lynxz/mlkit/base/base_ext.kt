package org.lynxz.mlkit.base

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * 在图像上绘制方框和文本标签信息
 */
fun Bitmap.drawWithRectAndLabel(
    bounds: List<Rect>,
    labels: List<String>,
    paint4Rect: Paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        textSize = 20.0f
        strokeWidth = 2.0f
        isAntiAlias = true
    },
    paint4Text: Paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        textSize = 14.0f
        strokeWidth = 0.0f
        isAntiAlias = true
    }
): Bitmap {
    val bitmap = copy(config, true)
    val canvas = Canvas(bitmap)
    for ((index, bound) in bounds.withIndex()) {
        val labelTip = labels[index]
        canvas.drawRect(bound, paint4Rect)
        canvas.drawText(
            labelTip,
            bound.left.toFloat(),
            bound.top.toFloat() - paint4Rect.strokeWidth,
            paint4Text
        )
    }
    return bitmap
}