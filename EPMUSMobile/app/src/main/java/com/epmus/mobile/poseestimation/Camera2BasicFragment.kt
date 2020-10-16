/*
 * Copyright 2018 Zihua Zeng (edvard_hua@live.com), Lang Feng (tearjeaker@hotmail.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epmus.mobile.poseestimation

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.epmus.mobile.R
import com.epmus.mobile.program.ProgramListActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.Serializable
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


/**
 * Basic fragments for the Camera.
 */
class Camera2BasicFragment : Fragment() {

    private val statistiques = ArrayList<Exercice>()

    private val lock = Any()
    private var runClassifier = false
    private var checkedPermissions = false
    private var textView: TextView? = null
    private var debugView: TextView? = null
    private var textureView: AutoFitTextureView? = null
    private var layoutFrame: AutoFitFrameLayout? = null
    private var drawView: DrawView? = null
    private var classifier: ImageClassifier? = null
    private var layoutBottom: ViewGroup? = null
    private var radiogroup: RadioGroup? = null

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a [ ].
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int
        ) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture,
                width: Int,
                height: Int
        ) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    /**
     * ID of the current [CameraDevice].
     */
    private var cameraId: String? = null

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private var previewSize: Size? = null

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(currentCameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release()
            cameraDevice = currentCameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(currentCameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            currentCameraDevice.close()
            cameraDevice = null
        }

        override fun onError(
                currentCameraDevice: CameraDevice,
                error: Int
        ) {
            cameraOpenCloseLock.release()
            currentCameraDevice.close()
            cameraDevice = null
            val activity = activity
            activity?.finish()
        }
    }

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

    /**
     * An [ImageReader] that handles image capture.
     */
    private var imageReader: ImageReader? = null

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    /**
     * [CaptureRequest] generated by [.previewRequestBuilder]
     */
    private var previewRequest: CaptureRequest? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to capture.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult
        ) {
        }

        override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
        ) {
        }
    }

    private val requiredPermissions: Array<String>
        get() {
            val activity = activity
            return try {
                val info = activity
                    ?.packageManager
                    ?.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
                val ps = info?.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOf()
                }
            } catch (e: Exception) {
                arrayOf()
            }

        }

    /**
     * Takes photos and classify them periodically.
     */
    private val periodicClassify = object : Runnable {
        override fun run() {
            synchronized(lock) {
                if (runClassifier) {
                    classifyFrame()
                }
            }
            backgroundHandler!!.post(this)
        }
    }

    /**
     * Shows a [Toast] on the UI thread for the classification results.
     *
     * @param text The message to show
     */
    private fun showToast(text: String) {
        val activity = activity
        activity?.runOnUiThread {
            textView!!.text = text
            drawView!!.invalidate()
        }
    }

    private fun showValues(exercises: Exercice)
    {
        var labelVitesse: String = ""
        if (exercises.mouvementSpeedTime != null)
        {
            if (exercises.mouvementSpeedTime!! < exercises.minExecutionTime!!)
            {
                labelVitesse = "-"
            }
            else if (exercises.mouvementSpeedTime!! > exercises.maxExecutionTime!!)
            {
                labelVitesse = "+"
            }
            else
            {
                labelVitesse = "="
            }
        }

        var text = ""
        var debug = ""
        when(exercises.exerciceType)
        {
            ExerciceType.HOLD -> {
                text = "holdTime: " + ((exercises.holdTime + exercises.currentHoldTime)/1000).toInt()
                debug = "; Fini? " + exercises.exitStateReached
            }

            ExerciceType.REPETITION -> {
                text = "ϴ: " + exercises.movementList[0].angleAvg +
                        "; État: " + exercises.movementList[0].movementState.ordinal +
                        "; Vit.: " + exercises.mouvementSpeedTime + " s (" + labelVitesse + ")"
                debug = "Répét.: " + exercises.numberOfRepetition +
                        "; Tot.: " + exercises.numberOfRepetitionToDo +
                        "; Fini? " + exercises.exitStateReached
            }

            ExerciceType.CHRONO -> {
                text = "ϴ: " + exercises.movementList[0].angleAvg +
                        "; État: " + exercises.movementList[0].movementState.ordinal +
                        "; Vit.: " + exercises.mouvementSpeedTime + " s (" + labelVitesse + ")"
                debug = "Répét.: " + exercises.numberOfRepetition +
                        "; Chrono.: " + exercises.chronoTime!! +
                        "; Fini? " + exercises.exitStateReached
            }
        }


        val activity = activity
        activity?.runOnUiThread {
            textView!!.text = text
            drawView!!.invalidate()

            var textView2: TextView? = view?.findViewById(R.id.debug)
            textView2!!.text = debug
            drawView!!.invalidate()
        }

    }
    /**
     * Layout the preview and buttons.
     */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false)
    }

    /**
     * Connect the buttons to their event handler.
     */
    override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?
    ) {
        textureView = view.findViewById(R.id.texture)
        textView = view.findViewById(R.id.text)
        debugView = view.findViewById(R.id.debug)
        layoutFrame = view.findViewById(R.id.layout_frame)
        drawView = view.findViewById(R.id.drawview)
        layoutBottom = view.findViewById(R.id.layout_bottom)

        drawView!!.exercice = arguments?.getSerializable("exercice") as Exercice
    }

    /**
     * Load the model and labels.
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            //      classifier = new ImageClassifierQuantizedMobileNet(getActivity());
            classifier = activity?.let { ImageClassifierFloatInception.create(it) }
            if (drawView != null)
                drawView!!.setImgSize(classifier!!.imageSizeX, classifier!!.imageSizeY)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize an image classifier.", e)
        }
    }

    @Synchronized
    override fun onResume() {
        super.onResume()

        backgroundThread = HandlerThread(HANDLE_THREAD_NAME)
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
        runClassifier = true

        startBackgroundThread { classifier!!.initTflite() }
        startBackgroundThread(periodicClassify)

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView!!.isAvailable) {
            openCamera(textureView!!.width, textureView!!.height)
        } else {
            textureView!!.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onDestroy() {
        classifier!!.close()
        super.onDestroy()
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private fun setUpCameraOutputs(
            width: Int,
            height: Int
    ) {
        val activity = activity
        val manager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                val map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                ?: continue

                // // For still image captures, we use the largest available size.
                val largest = Collections.max(
                        listOf(*map.getOutputSizes(ImageFormat.JPEG)), CompareSizesByArea()
                )
                imageReader = ImageReader.newInstance(
                        largest.width, largest.height, ImageFormat.JPEG, /*maxImages*/ 2
                )

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = activity.windowManager.defaultDisplay.rotation

                /* Orientation of the camera sensor */
                val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                var swappedDimensions = false
                when (displayRotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true
                    }
                    else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
                }

                val displaySize = Point()
                activity.windowManager.defaultDisplay.getSize(displaySize)
                var rotatedPreviewWidth = width
                var rotatedPreviewHeight = height
                var maxPreviewWidth = displaySize.x
                var maxPreviewHeight = displaySize.y

                if (swappedDimensions) {
                    rotatedPreviewWidth = height
                    rotatedPreviewHeight = width
                    maxPreviewWidth = displaySize.y
                    maxPreviewHeight = displaySize.x
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT
                }

                previewSize = chooseOptimalSize(
                        map.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewWidth,
                        rotatedPreviewHeight,
                        maxPreviewWidth,
                        maxPreviewHeight,
                        largest
                )

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    layoutFrame!!.setAspectRatio(previewSize!!.width, previewSize!!.height)
                    textureView!!.setAspectRatio(previewSize!!.width, previewSize!!.height)
                    drawView!!.setAspectRatio(previewSize!!.width, previewSize!!.height)

                    //Adjust textfield background_initialize to fit the camera overlay
                    activity.runOnUiThread {
                        var textViewBackground: TextView? = view?.findViewById(R.id.background_initialize)
                        var tmpHeight : Int = displaySize.x * previewSize!!.height / previewSize!!.width // to keep the ratio
                        var tmpLayout: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, tmpHeight)
                        tmpLayout.gravity = Gravity.CENTER
                        textViewBackground!!.layoutParams = tmpLayout
                        drawView!!.invalidate()
                    }

                }
                // This one will run most of the times
                else {
                    var newWidth = previewSize!!.height
                    var newHeight = previewSize!!.width

                    layoutFrame!!.setAspectRatio(newWidth, newHeight)
                    textureView!!.setAspectRatio(newWidth, newHeight)
                    drawView!!.setAspectRatio(newWidth, newHeight)

                    activity.runOnUiThread {
                        val textViewBackground: TextView? = view?.findViewById(R.id.background_initialize)
                        val tmpHeight : Int = displaySize.x * newHeight / newWidth
                        val tmpLayout: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, tmpHeight)
                        tmpLayout.gravity = Gravity.CENTER
                        textViewBackground!!.layoutParams = tmpLayout
                        drawView!!.invalidate()
                    }
                }

                //this.cameraId = cameraId
                // In my case (razer phone 1)
                // "0" back camera
                // "1" front camera
                this.cameraId = "1"
                return
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to access Camera", e)
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
        }

    }

    /**
     * Opens the camera specified by [Camera2BasicFragment.cameraId].
     */
    @SuppressLint("MissingPermission")
    private fun openCamera(
            width: Int,
            height: Int
    ) {
        if (!checkedPermissions && !allPermissionsGranted()) {
            this.activity?.let { ActivityCompat.requestPermissions(it, requiredPermissions, PERMISSIONS_REQUEST_CODE) }
            return
        } else {
            checkedPermissions = true
        }
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val activity = activity
        val manager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open Camera", e)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (activity?.let {
                    ContextCompat.checkSelfPermission(
                        it, permission
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Closes the current [CameraDevice].
     */
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            if (null != captureSession) {
                captureSession!!.close()
                captureSession = null
            }
            if (null != cameraDevice) {
                cameraDevice!!.close()
                cameraDevice = null
            }
            if (null != imageReader) {
                imageReader!!.close()
                imageReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    @Synchronized
    protected fun startBackgroundThread(r: Runnable) {
        if (backgroundHandler != null) {
            backgroundHandler!!.post(r)
        }
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            backgroundHandler = null
            synchronized(lock) {
                runClassifier = false
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted when stopping background thread", e)
        }

    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        try {
            val texture = textureView!!.surfaceTexture!!

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice!!.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return
                            }

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder!!.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder!!.build()
                                captureSession!!.setRepeatingRequest(
                                        previewRequest!!, captureCallback, backgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                Log.e(TAG, "Failed to set up config to capture Camera", e)
                            }

                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            showToast("Failed")
                        }
                    }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to preview Camera", e)
        }

    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`. This
     * method should be called after the camera preview size is determined in setUpCameraOutputs and
     * also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private fun configureTransform(
            viewWidth: Int,
            viewHeight: Int
    ) {
        val activity = activity
        if (null == textureView || null == previewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = (viewHeight.toFloat() / previewSize!!.height).coerceAtLeast(viewWidth.toFloat() / previewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView!!.setTransform(matrix)
    }

    /**
     * Classifies a frame from the preview stream.
     */
    private fun classifyFrame() {
        if (classifier == null || activity == null || cameraDevice == null) {
            showToast("Uninitialized Classifier or invalid context.")
            return
        }
        val bitmap = textureView!!.getBitmap(classifier!!.imageSizeX, classifier!!.imageSizeY)
        classifier!!.classifyFrame(bitmap)
        bitmap.recycle()

        drawView!!.setDrawPoint(classifier!!.mPrintPointArray!!, 0.5f)

        //initialize bodyparts
        if (drawView!!.exercice!!.initList.count() == 0)
        {
            repeat(enumValues<BodyPart>().count()) {
                var pF = PointF(-1.0f, -1.0f)
                var aList = arrayListOf<PointF>(pF)
                drawView!!.exercice!!.initList.add(aList)
                drawView!!.exercice!!.notMovingInitList.add(false)
            }
        }


        //if not initialized yet
        if (drawView!!.exercice?.isInit == false)
        {
            showToast("")
            showDebugUI("")

            drawView!!.exercice?.initialisationVerification(drawView!!)
            //debug
            if (drawView!!.exercice!!.initList[0].count() > 1)
            {
                // show timer to start
                if (drawView!!.exercice!!.notMovingTimer < drawView!!.exercice!!.targetTime.toInt()/1000 &&
                            drawView!!.exercice!!.notMovingTimer > 0)
                {
                    val activity = activity
                    activity?.runOnUiThread {
                        var textViewBackground: TextView? = view?.findViewById(R.id.background_initialize)
                        textViewBackground!!.alpha = 0.7F

                        var textViewCountdown: TextView? = view?.findViewById(R.id.countdown)
                        textViewCountdown!!.text = drawView!!.exercice!!.notMovingTimer.toString()

                        drawView!!.invalidate()
                    }
                }

                // hide background if moving
                else {
                    val activity = activity
                    activity?.runOnUiThread {
                        var textViewBackground: TextView? = view?.findViewById(R.id.background_initialize)
                        textViewBackground!!.alpha = 0.0F

                        var textViewCountdown: TextView? = view?.findViewById(R.id.countdown)
                        textViewCountdown!!.text = ""

                        drawView!!.invalidate()
                    }

                }

            }
        }

        else
        {
            // Verify angle
            drawView!!.exercice!!.exerciceVerification(drawView!!)
            showValues(drawView!!.exercice!!)
            statistiques.add(drawView!!.exercice!!.copy())

            // Done -> exit exercise
            if (drawView!!.exercice!!.exitStateReached == true) {
                val activity = activity

                activity?.runOnUiThread {
                    var textViewBackground: TextView? = view?.findViewById(R.id.background_initialize)
                    textViewBackground!!.alpha = 1.0F

                    var textViewCountdown: TextView? = view?.findViewById(R.id.countdown)
                    textViewCountdown!!.text = "Terminé"
                    drawView!!.invalidate()

                    // must do a separate thread or the background wont show
                    GlobalScope.launch {
                        delay(2000L)

                        //EXIT !
                        activity.let {
                            val intent = Intent(it, ProgramListActivity::class.java)
                            it.startActivity(intent)
                            it.finish()
                        }
                    }
                }
            }
        }
    }

    private fun showDebugUI(text: String)
    {
        val activity = activity
        activity?.runOnUiThread {
            debugView!!.text = text
            drawView!!.invalidate()
        }
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    private class CompareSizesByArea : Comparator<Size> {

        override fun compare(
                lhs: Size,
                rhs: Size
        ): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                    lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height
            )
        }
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            return AlertDialog.Builder(activity)
                .setMessage(arguments?.getString(ARG_MESSAGE))
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ -> activity?.finish() }
                .create()
        }

        companion object {

            private val ARG_MESSAGE = "message"

            fun newInstance(message: String): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }

    companion object {

        /**
         * Tag for the [Log].
         */
        private const val TAG = "TfLiteCameraDemo"

        private const val FRAGMENT_DIALOG = "dialog"

        private const val HANDLE_THREAD_NAME = "CameraBackground"

        private const val PERMISSIONS_REQUEST_CODE = 1

        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_HEIGHT = 1080

        /**
         * Resizes image.
         *
         *
         * Attempting to use too large a preview size could  exceed the camera bus' bandwidth limitation,
         * resulting in gorgeous previews but the storage of garbage capture data.
         *
         *
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that is
         * at least as large as the respective texture view size, and that is at most as large as the
         * respective max size, and whose aspect ratio matches with the specified value. If such size
         * doesn't exist, choose the largest one that is at most as large as the respective max size, and
         * whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended output class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        private fun chooseOptimalSize(
                choices: Array<Size>,
                textureViewWidth: Int,
                textureViewHeight: Int,
                maxWidth: Int,
                maxHeight: Int,
                aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth
                        && option.height <= maxHeight
                        && option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            return when {
                bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
                notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
                else -> {
                    Log.e(TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }

        fun newInstance(exerice: Serializable?): Camera2BasicFragment {
            val myFragment = Camera2BasicFragment()

            val args = Bundle()
            args.putSerializable("exercice", exerice)
            myFragment.arguments = args

            return myFragment
        }
    }
}
