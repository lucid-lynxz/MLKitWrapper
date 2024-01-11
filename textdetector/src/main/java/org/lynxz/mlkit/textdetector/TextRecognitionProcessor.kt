/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lynxz.mlkit.textdetector

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import org.lynxz.mlkit.base.OnTaskListener
import org.lynxz.mlkit.base.VisionProcessorBase
import org.lynxz.mlkit.base.ui.GraphicOverlay
import org.lynxz.mlkit.base.util.LogWrapper
import org.lynxz.mlkit.base.util.PreferenceUtils
import java.lang.ref.WeakReference
import java.util.Collections

/** Processor for the text detector demo. */
class TextRecognitionProcessor(
    context: Context,
    textRecognizerOptions: TextRecognizerOptionsInterface
) : VisionProcessorBase<Text>(context) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)
    private val shouldGroupRecognizedTextInBlocks: Boolean =
        PreferenceUtils.shouldGroupRecognizedTextInBlocks(context)
    private val showLanguageTag: Boolean = PreferenceUtils.showLanguageTag(context)
    private val showConfidence: Boolean = PreferenceUtils.shouldShowTextConfidence(context)
    private var onTaskListenerMap =
        Collections.synchronizedMap(mutableMapOf<Int, WeakReference<OnTaskListener<Text>>>())

    override fun stop() {
        super.stop()
        textRecognizer.close()
        onTaskListenerMap.clear()
    }

    fun processBitmap(
        bitmap: Bitmap?, graphicOverlay: GraphicOverlay?,
        onTaskListener: OnTaskListener<Text>? = null
    ): Int {
        val taskId = super.processBitmap(bitmap, graphicOverlay)
        if (onTaskListener != null) {
            onTaskListenerMap[taskId] = WeakReference(onTaskListener)
        }
        return taskId
    }

    override fun detectInImage(image: InputImage): Task<Text> {
        return textRecognizer.process(image)
    }

    override fun onSuccess(taskId: Int, results: Text, graphicOverlay: GraphicOverlay?) {
        LogWrapper.d(TAG, "$taskId On-device Text detection successful text=${results.text}")
        logExtrasForTesting(results)
        graphicOverlay?.add(
            TextGraphic(
                graphicOverlay,
                results,
                shouldGroupRecognizedTextInBlocks,
                showLanguageTag,
                showConfidence
            )
        )
        onTaskListenerMap[taskId]?.get()?.apply {
            onSuccess(taskId, results)
            onTaskListenerMap.remove(taskId)
        }
    }

    override fun onFailure(taskId: Int, e: Exception) {
        LogWrapper.w(TAG, "$taskId Text detection failed.$e")
        onTaskListenerMap[taskId]?.get()?.apply {
            onFailure(taskId, e)
            onTaskListenerMap.remove(taskId)
        }
    }

    companion object {
        private const val TAG = "TextRecProcessor"
        private fun logExtrasForTesting(text: Text?) {
            if (text != null) {
                LogWrapper.v(
                    MANUAL_TESTING_LOG,
                    "Detected text has : " + text.textBlocks.size + " blocks"
                )
                for (i in text.textBlocks.indices) {
                    val lines = text.textBlocks[i].lines
                    LogWrapper.v(
                        MANUAL_TESTING_LOG,
                        String.format("Detected text block %d has %d lines", i, lines.size)
                    )
                    for (j in lines.indices) {
                        val elements = lines[j].elements
                        LogWrapper.v(
                            MANUAL_TESTING_LOG,
                            String.format("Detected text line %d has %d elements", j, elements.size)
                        )
                        for (k in elements.indices) {
                            val element = elements[k]
                            LogWrapper.v(
                                MANUAL_TESTING_LOG,
                                String.format("Detected text element %d says: %s", k, element.text)
                            )
                            LogWrapper.v(
                                MANUAL_TESTING_LOG,
                                String.format(
                                    "Detected text element %d has a bounding box: %s",
                                    k,
                                    element.boundingBox!!.flattenToString()
                                )
                            )
                            LogWrapper.v(
                                MANUAL_TESTING_LOG,
                                String.format(
                                    "Expected corner point size is 4, get %d",
                                    element.cornerPoints!!.size
                                )
                            )
                            for (point in element.cornerPoints!!) {
                                LogWrapper.v(
                                    MANUAL_TESTING_LOG,
                                    String.format(
                                        "Corner point for element %d is located at: x - %d, y = %d",
                                        k,
                                        point.x,
                                        point.y
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
