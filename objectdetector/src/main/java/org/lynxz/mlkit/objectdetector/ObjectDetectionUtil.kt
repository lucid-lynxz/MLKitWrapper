package org.lynxz.mlkit.objectdetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase.DetectorMode
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import org.lynxz.mlkit.base.OnTaskListener

class ObjectDetectionUtil(
    private val context: Context,
    customTFilePath: String = "", // 基于自定义的tfile模型进行物体检测时必传,可以是asset相对路径,也可以是绝对路径
    @DetectorMode mode: Int = CustomObjectDetectorOptions.SINGLE_IMAGE_MODE,
    enableMultipleObjects: Boolean = true,
    enableClassification: Boolean = true
) {
    companion object {
        private const val TAG = "ObjectDetectionUtil"

        /**
         * @param localTFilePath 本地 tffile 文件瑞景
         */
        @JvmStatic
        @JvmOverloads
        fun generateObjectProcessor(
            context: Context,
            localTFilePath: String,
            @DetectorMode mode: Int = CustomObjectDetectorOptions.SINGLE_IMAGE_MODE,
            enableMultipleObjects: Boolean = true,
            enableClassification: Boolean = true
        ): ObjectDetectorProcessor {
            val localModel = if (localTFilePath.startsWith("/")) {
                LocalModel.Builder().setAbsoluteFilePath(localTFilePath).build()
            } else {
                LocalModel.Builder().setAssetFilePath(localTFilePath).build()
            }

            val builder = CustomObjectDetectorOptions
                .Builder(localModel)
                .setDetectorMode(mode).apply {
                    if (enableMultipleObjects) {
                        enableMultipleObjects()
                    }

                    if (enableClassification) {
                        enableClassification().setMaxPerObjectLabelCount(1)
                    }
                }
                .build()
            return ObjectDetectorProcessor(context, builder)
        }
    }

    private val defaultObjectProcessor by lazy {
        val builder = ObjectDetectorOptions
            .Builder()
            .setDetectorMode(mode).apply {
                if (enableMultipleObjects) {
                    enableMultipleObjects()
                }

                if (enableClassification) {
                    enableClassification()
                }
            }
            .build()

        ObjectDetectorProcessor(context, builder)
    }

    private val customObjectProcessor by lazy {
        generateObjectProcessor(
            context,
            customTFilePath,
            mode,
            enableMultipleObjects,
            enableClassification
        )
    }

    /**
     * 对图片进行物体检测
     */
    fun detectInImage(
        bitmap: Bitmap,
        byCustomModel: Boolean, // 是否使用自定义的模型进行检测
        onTaskListener: OnTaskListener<List<DetectedObject>>? = null
    ): Int = detectInImage(
        bitmap,
        if (byCustomModel) customObjectProcessor else defaultObjectProcessor,
        onTaskListener
    )

    fun detectInImage(
        bitmap: Bitmap,
        processor: ObjectDetectorProcessor,
        onTaskListener: OnTaskListener<List<DetectedObject>>? = null
    ): Int = processor.processBitmap(bitmap, null, onTaskListener)

    fun uninit() {
        defaultObjectProcessor.stop()
        customObjectProcessor.stop()
    }

    private val rectanglePaint by lazy {
        Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            textSize = 20.0f
            strokeWidth = 2.0f
            isAntiAlias = true
        }
    }

    private val textPaint by lazy {
        Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            textSize = 14.0f
            strokeWidth = 0.0f
            isAntiAlias = true
        }
    }

    /**
     * 在图像上绘制物体检测结果框
     * 包含物体序号, 置信度最大的标签名及其置信度
     */
    fun drawWithRectangle(
        srcBitmap: Bitmap,
        objects: List<DetectedObject>,
        paint4Rect: Paint = rectanglePaint,
        paint4Text: Paint = textPaint,
    ): Bitmap {
        val bitmap = srcBitmap.copy(srcBitmap.config, true)
        val canvas = Canvas(bitmap)
        for ((index, obj) in objects.withIndex()) {
            val bounds = obj.boundingBox
            canvas.drawRect(bounds, paint4Rect)
            obj.labels.sortBy { it.confidence }
            val label = obj.labels.lastOrNull()
            val labelTip = "Obj:$index ${label?.text ?: ""} ${label?.confidence ?: ""}".trim()
            canvas.drawText(
                labelTip,
                bounds.left.toFloat(),
                bounds.top.toFloat() - paint4Rect.strokeWidth,
                paint4Text
            )
        }
        return bitmap
    }

//    fun drawWithLabels(
//        srcBitmap: Bitmap,
//        objects: List<DetectedObject>,
//        txtOutput: TextView? = null,
//        options: ImageLabelerOptions = ImageLabelerOptions.DEFAULT_OPTIONS,
//        onTaskListener: OnTaskListener<List<ImageLabel>>? = null
//    ) {
//        val labeler = ImageLabeling.getClient(options)
//        for (obj in objects) {
//            val bounds = obj.boundingBox
//            val croppedBitmap = Bitmap.createBitmap(
//                srcBitmap,
//                bounds.left,
//                bounds.top,
//                bounds.width(),
//                bounds.height()
//            )
//            val image = InputImage.fromBitmap(croppedBitmap, 0)
//            val taskId = defaultObjectProcessor.nextTaskId()
//            labeler.process(image)
//                .addOnSuccessListener { labels ->
//                    var labelText = txtOutput?.text?.toString() ?: ""
//                    if (labels.isNotEmpty()) {
//                        for (label in labels) {
//                            labelText += "${label.text},"
//                        }
//                        labelText += "\n"
//                    } else {
//                        labelText = "Not found.\n"
//                    }
//                    txtOutput?.text = labelText
//                    LogWrapper.d(TAG, "getLabels result=$labelText")
//                    onTaskListener?.onSuccess(taskId, labels)
//                }.addOnFailureListener {
//                    LogWrapper.w(TAG, "$taskId detection failed.$it")
//                    onTaskListener?.onFailure(taskId, it)
//                }
//        }
//    }
}