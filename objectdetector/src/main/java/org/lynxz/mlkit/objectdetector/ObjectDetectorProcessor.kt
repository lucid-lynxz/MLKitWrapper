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
package org.lynxz.mlkit.objectdetector

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase
import org.lynxz.mlkit.base.VisionProcessorBase
import org.lynxz.mlkit.base.ui.GraphicOverlay

/**
 * A processor to run object detector.
 */
class ObjectDetectorProcessor(context: Context?, options: ObjectDetectorOptionsBase?) :
    VisionProcessorBase<List<DetectedObject>>(
        context!!
    ) {
    private val detector = ObjectDetection.getClient(options!!)

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<List<DetectedObject>> {
        return detector.process(image)
    }

    override fun onSuccess(
        taskId: Int,
        results: List<DetectedObject>, graphicOverlay: GraphicOverlay?
    ) {
        super.onSuccess(taskId, results, graphicOverlay)
        for (obj in results) {
            graphicOverlay?.add(ObjectGraphic(graphicOverlay, obj))
        }
    }

    companion object {
        private const val TAG = "ObjectDetectorProcessor"
    }
}
