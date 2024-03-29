package org.lynxz.mlkitdemo

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.text.Text
import org.lynxz.mlkit.base.OnTaskListener
import org.lynxz.mlkit.base.util.FileUtils
import org.lynxz.mlkit.base.util.LogWrapper
import org.lynxz.mlkit.objectdetector.ObjectDetectionUtil
import org.lynxz.mlkit.textdetector.TextRecognitionUtil
import org.lynxz.mlkitdemo.databinding.ActivityFullscreenBinding

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : AppCompatActivity() {

    private val TAG = "FullscreenActivity"
    private lateinit var binding: ActivityFullscreenBinding
    private lateinit var fullscreenContent: TextView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()

    private val sdPath by lazy { "${this.applicationContext.getExternalFilesDir(null)}/" }
    private val textRecognitionUtil by lazy { TextRecognitionUtil(this).apply { init(BuildConfig.DEBUG) } }
    private val objectDetectorUtil by lazy {
        ObjectDetectionUtil(
            this,
            customTFilePath = "custom_models/object_labeler.tflite"
        )
    }


    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
                val taskId1 = textRecognitionUtil.detectInImage(
                    BitmapFactory.decodeStream(assets.open("img_text.png")),
                    onTaskListener = object : OnTaskListener<Text> {
                        override fun onSuccess(taskId: Int, result: Text) {
                            LogWrapper.d(
                                TAG,
                                "$taskId detectInImage img_text.png result=${result.text}"
                            )
                        }

                        override fun onFailure(taskId: Int, e: Exception) {
                            super.onFailure(taskId, e)
                            LogWrapper.e(
                                TAG,
                                "$taskId detectInImage img_text.png exception=${e.message}"
                            )
                        }
                    })
                LogWrapper.d(TAG, "start detectInImage img_text.png taskId=$taskId1")


                val taskId2 = textRecognitionUtil.detectInImage(
                    BitmapFactory.decodeStream(assets.open("img_text_light.png")),
                    onTaskListener = object : OnTaskListener<Text> {
                        override fun onSuccess(taskId: Int, result: Text) {
                            LogWrapper.d(
                                TAG,
                                "$taskId detectInImage img_text_light.png result=${result.text}"
                            )
                        }

                        override fun onFailure(taskId: Int, e: Exception) {
                            super.onFailure(taskId, e)
                            LogWrapper.e(
                                TAG,
                                "$taskId detectInImage img_text.png exception=${e.message}"
                            )
                        }
                    })
                LogWrapper.d(TAG, "start detectInImage img_text_light.png taskId=$taskId2")


                val srcBitmap = BitmapFactory.decodeStream(
                    assets.open("cat_dog.jpg"),
                )
                objectDetectorUtil.detectInImage(
                    srcBitmap, true,
                    onTaskListener = object : OnTaskListener<List<DetectedObject>> {
                        override fun onSuccess(taskId: Int, result: List<DetectedObject>) {
                            val rectBitmap = objectDetectorUtil.drawWithRectangle(srcBitmap, result)
                            FileUtils.saveImage(rectBitmap, "${sdPath}cat_dog_rect.png")
                            LogWrapper.d(
                                TAG,
                                "$taskId detectInImage cat_dog.jpg result=${result}"
                            )

                            // objectDetectorUtil.drawWithLabels(srcBitmap, result)
                        }

                        override fun onFailure(taskId: Int, e: Exception) {
                            super.onFailure(taskId, e)
                            LogWrapper.e(
                                TAG,
                                "$taskId detectInImage cat_dog.jpg exception=${e.message}"
                            )
                        }
                    }
                )
            }

            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.fullscreenContent
        fullscreenContent.setOnClickListener { toggle() }

        fullscreenContentControls = binding.fullscreenContentControls

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        binding.dummyButton.setOnTouchListener(delayHideTouchListener)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognitionUtil.uninit()
        objectDetectorUtil.uninit()
    }
}