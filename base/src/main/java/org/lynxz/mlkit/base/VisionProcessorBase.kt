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

package org.lynxz.mlkit.base

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.android.gms.tasks.Tasks
import com.google.android.odml.image.BitmapMlImageBuilder
import com.google.android.odml.image.ByteBufferMlImageBuilder
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.android.odml.image.MlImage
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import org.lynxz.mlkit.base.ui.CameraImageGraphic
import org.lynxz.mlkit.base.ui.GraphicOverlay
import org.lynxz.mlkit.base.ui.InferenceInfoGraphic
import org.lynxz.mlkit.base.util.BitmapUtils
import org.lynxz.mlkit.base.util.LogWrapper
import org.lynxz.mlkit.base.util.PreferenceUtils
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
abstract class VisionProcessorBase<T>(private val context: Context) : VisionImageProcessor {

    companion object {
        const val MANUAL_TESTING_LOG = "LogTagForTest"
        private const val TAG = "VisionProcessorBase"
    }

    private var activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val fpsTimer = Timer()
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    // Whether this processor is already shut down
    private var isShutdown = false

    // Used to calculate latency, running in the same thread, no sync needed.
    private var numRuns = 0
    private var totalFrameMs = 0L
    private var maxFrameMs = 0L
    private var minFrameMs = Long.MAX_VALUE
    private var totalDetectorMs = 0L
    private var maxDetectorMs = 0L
    private var minDetectorMs = Long.MAX_VALUE

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    init {
        fpsTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                }
            },
            0,
            1000
        )
    }

    private val taskId = AtomicInteger(0)

    // -----------------Code for processing single still image----------------------------------------
    override fun processBitmap(bitmap: Bitmap?, graphicOverlay: GraphicOverlay?): Int {
        val frameStartMs = SystemClock.elapsedRealtime()
        val id = taskId.incrementAndGet()

        if (isMlImageEnabled(context)) {
            val mlImage = BitmapMlImageBuilder(bitmap!!).build()
            requestDetectInImage(
                mlImage,
                graphicOverlay,
                /* originalCameraImage= */ null,
                /* shouldShowFps= */ false,
                frameStartMs,
                id
            )
            mlImage.close()
            return id
        }

        requestDetectInImage(
            InputImage.fromBitmap(bitmap!!, 0),
            graphicOverlay,
            /* originalCameraImage= */ null,
            /* shouldShowFps= */ false,
            frameStartMs,
            id
        )
        return id
    }

    // -----------------Code for processing live preview frame from Camera1 API-----------------------
    @Synchronized
    override fun processByteBuffer(
        data: ByteBuffer?,
        frameMetadata: FrameMetadata?,
        graphicOverlay: GraphicOverlay
    ) {
        latestImage = data
        latestImageMetaData = frameMetadata
        if (processingImage == null && processingMetaData == null) {
            processLatestImage(graphicOverlay)
        }
    }

    @Synchronized
    private fun processLatestImage(graphicOverlay: GraphicOverlay) {
        processingImage = latestImage
        processingMetaData = latestImageMetaData
        latestImage = null
        latestImageMetaData = null
        if (processingImage != null && processingMetaData != null && !isShutdown) {
            processImage(processingImage!!, processingMetaData!!, graphicOverlay)
        }
    }

    private fun processImage(
        data: ByteBuffer,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay
    ) {
        val id = taskId.incrementAndGet()
        val frameStartMs = SystemClock.elapsedRealtime()
        // If live viewport is on (that is the underneath surface view takes care of the camera preview
        // drawing), skip the unnecessary bitmap creation that used for the manual preview drawing.
        val bitmap =
            if (PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.context)) null
            else BitmapUtils.getBitmap(data, frameMetadata)

        if (isMlImageEnabled(graphicOverlay.context)) {
            val mlImage =
                ByteBufferMlImageBuilder(
                    data,
                    frameMetadata.width,
                    frameMetadata.height,
                    MlImage.IMAGE_FORMAT_NV21
                )
                    .setRotation(frameMetadata.rotation)
                    .build()
            requestDetectInImage(
                mlImage,
                graphicOverlay,
                bitmap, /* shouldShowFps= */
                true,
                frameStartMs,
                id
            ).addOnSuccessListener(executor) { processLatestImage(graphicOverlay) }

            // This is optional. Java Garbage collection can also close it eventually.
            mlImage.close()
            return
        }

        requestDetectInImage(
            InputImage.fromByteBuffer(
                data,
                frameMetadata.width,
                frameMetadata.height,
                frameMetadata.rotation,
                InputImage.IMAGE_FORMAT_NV21
            ),
            graphicOverlay,
            bitmap,
            /* shouldShowFps= */ true,
            frameStartMs,
            id
        ).addOnSuccessListener(executor) { processLatestImage(graphicOverlay) }
    }

    // -----------------Code for processing live preview frame from CameraX API-----------------------
    @ExperimentalGetImage
    override fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay) {
        val id = taskId.incrementAndGet()
        val frameStartMs = SystemClock.elapsedRealtime()
        if (isShutdown) {
            return
        }
        var bitmap: Bitmap? = null
        if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.context)) {
            bitmap = BitmapUtils.getBitmap(image)
        }

        if (isMlImageEnabled(graphicOverlay.context)) {
            val mlImage =
                MediaMlImageBuilder(image.image!!).setRotation(image.imageInfo.rotationDegrees)
                    .build()
            requestDetectInImage(
                mlImage,
                graphicOverlay,
                /* originalCameraImage= */ bitmap,
                /* shouldShowFps= */ true,
                frameStartMs, id
            )
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                // Currently MlImage doesn't support ImageProxy directly, so we still need to call
                // ImageProxy.close() here.
                .addOnCompleteListener { image.close() }

            return
        }

        requestDetectInImage(
            InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees),
            graphicOverlay,
            /* originalCameraImage= */ bitmap,
            /* shouldShowFps= */ true,
            frameStartMs, id
        )
            // When the image is from CameraX analysis use case, must call image.close() on received
            // images when finished using them. Otherwise, new images may not be received or the camera
            // may stall.
            .addOnCompleteListener { image.close() }
    }

    private fun requestDetectInImage(
        image: MlImage,
        graphicOverlay: GraphicOverlay?,
        originalCameraImage: Bitmap?,
        shouldShowFps: Boolean,
        frameStartMs: Long,
        taskId: Int
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            graphicOverlay,
            originalCameraImage,
            shouldShowFps,
            frameStartMs,
            taskId
        )
    }

    private fun setUpListener(
        task: Task<T>,
        graphicOverlay: GraphicOverlay?,
        originalCameraImage: Bitmap?,
        shouldShowFps: Boolean,
        frameStartMs: Long,
        taskId: Int
    ): Task<T> {
        val detectorStartMs = SystemClock.elapsedRealtime()
        return task.addOnSuccessListener(executor) { results: T ->
            val endMs = SystemClock.elapsedRealtime()
            val currentFrameLatencyMs = endMs - frameStartMs
            val currentDetectorLatencyMs = endMs - detectorStartMs
            if (numRuns >= 500) {
                resetLatencyStats()
            }
            numRuns++
            frameProcessedInOneSecondInterval++
            totalFrameMs += currentFrameLatencyMs
            maxFrameMs = currentFrameLatencyMs.coerceAtLeast(maxFrameMs)
            minFrameMs = currentFrameLatencyMs.coerceAtMost(minFrameMs)
            totalDetectorMs += currentDetectorLatencyMs
            maxDetectorMs = currentDetectorLatencyMs.coerceAtLeast(maxDetectorMs)
            minDetectorMs = currentDetectorLatencyMs.coerceAtMost(minDetectorMs)

            // Only log inference info once per second. When frameProcessedInOneSecondInterval is
            // equal to 1, it means this is the first frame processed during the current second.
            if (frameProcessedInOneSecondInterval == 1) {
                LogWrapper.d(TAG, "Num of Runs: $numRuns")
                LogWrapper.d(
                    TAG,
                    "Frame latency: max=" +
                            maxFrameMs +
                            ", min=" +
                            minFrameMs +
                            ", avg=" +
                            totalFrameMs / numRuns
                )
                LogWrapper.d(
                    TAG,
                    "Detector latency: max=" +
                            maxDetectorMs +
                            ", min=" +
                            minDetectorMs +
                            ", avg=" +
                            totalDetectorMs / numRuns
                )
                val mi = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(mi)
                val availableMegs: Long = mi.availMem / 0x100000L
                LogWrapper.d(TAG, "Memory available in system: $availableMegs MB")
            }

            graphicOverlay?.clear()
            if (originalCameraImage != null) {
                graphicOverlay?.add(
                    CameraImageGraphic(
                        graphicOverlay,
                        originalCameraImage
                    )
                )
            }
            this@VisionProcessorBase.onSuccess(taskId, results, graphicOverlay)
            if (!PreferenceUtils.shouldHideDetectionInfo(context)) {
                graphicOverlay?.add(
                    InferenceInfoGraphic(
                        graphicOverlay,
                        currentFrameLatencyMs,
                        currentDetectorLatencyMs,
                        if (shouldShowFps) framesPerSecond else null
                    )
                )
            }
            graphicOverlay?.postInvalidate()
        }.addOnFailureListener(executor) { e: Exception ->
            graphicOverlay?.clear()
            graphicOverlay?.postInvalidate()
            val error = "Failed to process. Error: " + e.localizedMessage
            Toast.makeText(
                context,
                """
          $error
          Cause: ${e.cause}
          """.trimIndent(),
                Toast.LENGTH_SHORT
            ).show()
            LogWrapper.d(TAG, error)
            e.printStackTrace()
            this@VisionProcessorBase.onFailure(taskId, e)
        }
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
        resetLatencyStats()
        fpsTimer.cancel()
    }

    private fun resetLatencyStats() {
        numRuns = 0
        totalFrameMs = 0
        maxFrameMs = 0
        minFrameMs = Long.MAX_VALUE
        totalDetectorMs = 0
        maxDetectorMs = 0
        minDetectorMs = Long.MAX_VALUE
    }

    // -----------------Common processing logic-------------------------------------------------------
    private fun requestDetectInImage(
        image: InputImage,
        graphicOverlay: GraphicOverlay?,
        originalCameraImage: Bitmap?,
        shouldShowFps: Boolean,
        frameStartMs: Long,
        taskId: Int
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            graphicOverlay,
            originalCameraImage,
            shouldShowFps,
            frameStartMs,
            taskId
        )
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected open fun detectInImage(image: MlImage): Task<T> {
        return Tasks.forException(
            MlKitException(
                "MlImage is currently not demonstrated for this feature",
                MlKitException.INVALID_ARGUMENT
            )
        )
    }

    protected abstract fun onSuccess(taskId: Int, results: T, graphicOverlay: GraphicOverlay?)

    protected abstract fun onFailure(taskId: Int, e: Exception)

    protected open fun isMlImageEnabled(context: Context?): Boolean {
        return false
    }
}
