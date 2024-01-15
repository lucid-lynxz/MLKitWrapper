package org.lynxz.mlkit.textdetector

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import org.lynxz.mlkit.base.OnTaskListener
import org.lynxz.mlkit.base.ui.GraphicOverlay
import org.lynxz.mlkit.base.util.LogWrapper

class TextRecognitionUtil(private val context: Context) {
    private var imageProcessor: TextRecognitionProcessor? = null

    fun init(debugMode: Boolean = false) {
        LogWrapper.enable(debugMode)
        imageProcessor?.stop()
        imageProcessor =
            TextRecognitionProcessor(context, ChineseTextRecognizerOptions.Builder().build())
    }

    fun uninit() {
        imageProcessor?.stop()
        imageProcessor = null
    }

    /**
     * 对图片进行文字识别, 支持中文英文
     */
    fun detectInImage(
        bitmap: Bitmap,
        processor: TextRecognitionProcessor? = imageProcessor,
        graphicOverlay: GraphicOverlay? = null,
        onTaskListener: OnTaskListener<Text>? = null
    ): Int = processor?.processBitmap(bitmap, graphicOverlay, onTaskListener) ?: -1
}