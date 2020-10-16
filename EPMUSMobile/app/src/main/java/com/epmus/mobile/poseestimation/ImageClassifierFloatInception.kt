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

import android.app.Activity
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

/**
 * Pose Estimator
 */
class ImageClassifierFloatInception private constructor(
    activity: Activity,
    imageSizeX: Int,
    imageSizeY: Int,
    private val outputW: Int,
    private val outputH: Int,
    numBytesPerChannel: Int = 4 // a 32bit float value requires 4 bytes
) : ImageClassifier(activity, imageSizeX, imageSizeY, numBytesPerChannel) {

    private val pointQty = 14
    private val yShift = pointQty * outputH
    private val tempArraySize = outputW * outputH
    private var mMat: Mat? = null

    override fun addPixelValue(pixelValue: Int) {
        //bgr
        imgData!!.putFloat((pixelValue and 0xFF).toFloat())
        imgData!!.putFloat((pixelValue shr 8 and 0xFF).toFloat())
        imgData!!.putFloat((pixelValue shr 16 and 0xFF).toFloat())
    }

    override fun getProbability(labelIndex: Int): Float {
        return 0f
    }

    override fun setProbability(labelIndex: Int, value: Number) {}

    override fun getNormalizedProbability(labelIndex: Int): Float {
        return getProbability(labelIndex)
    }

    override fun runInference() {
        val inputFeature0 =
            TensorBuffer.createFixedSize(intArrayOf(1, imageSizeX, imageSizeY, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(imgData!!)

        val outputBuffer = tflite?.process(inputFeature0)?.outputFeature0AsTensorBuffer
        val outputValues = outputBuffer?.floatArray

        if (mPrintPointArray == null)
            mPrintPointArray = Array(2) { FloatArray(pointQty) }

        if (!CameraActivity.isOpenCVInit)
            return

        // Gaussian Filter 5*5
        if (mMat == null)
            mMat = Mat(outputW, outputH, CvType.CV_32F)

        val tempArray = FloatArray(tempArraySize)
        val outTempArray = FloatArray(tempArraySize)
        for (i in 0 until pointQty) {
            var index = 0
            for (x in 0 until outputW) {
                for (y in 0 until outputH) {
                    tempArray[index] = outputValues?.get(i + (x * pointQty) + (y * yShift))!!
                    index++
                }
            }

            mMat!!.put(0, 0, tempArray)
            Imgproc.GaussianBlur(mMat!!, mMat!!, Size(5.0, 5.0), 0.0, 0.0)
            mMat!!.get(0, 0, outTempArray)

            var maxX = 0f
            var maxY = 0f
            var max = 0f

            // Find keypoint coordinate through maximum values
            for (x in 0 until outputW) {
                for (y in 0 until outputH) {
                    val center = get(x, y, outTempArray)
                    if (center > max) {
                        max = center
                        maxX = x.toFloat()
                        maxY = y.toFloat()
                    }
                }
            }

            if (max == 0f) {
                mPrintPointArray = Array(2) { FloatArray(14) }
                return
            }

            mPrintPointArray!![0][i] = maxX
            mPrintPointArray!![1][i] = maxY
        }
    }

    private operator fun get(
        x: Int,
        y: Int,
        arr: FloatArray
    ): Float {
        return if (x < 0 || y < 0 || x >= outputW || y >= outputH) -1f else arr[x * outputW + y]
    }

    companion object {

        /**
         * Create ImageClassifierFloatInception instance
         *
         * @param imageSizeX Get the image size along the x axis.
         * @param imageSizeY Get the image size along the y axis.
         * @param outputW The output width of model
         * @param outputH The output height of model
         * @param numBytesPerChannel Get the number of bytes that is used to store a single
         * color channel value.
         */
        fun create(
            activity: Activity,
            imageSizeX: Int = 192,
            imageSizeY: Int = 192,
            outputW: Int = 96,
            outputH: Int = 96,
            numBytesPerChannel: Int = 4
        ): ImageClassifierFloatInception =
            ImageClassifierFloatInception(
                activity,
                imageSizeX,
                imageSizeY,
                outputW,
                outputH,
                numBytesPerChannel
            )
    }
}
