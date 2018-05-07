/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.edvard.poseestimation

import android.app.Activity
import android.util.Log

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

import java.io.IOException

/**
 * This classifier works with the Inception-v3 slim model.
 * It applies floating point inference rather than using a quantized model.
 */
class ImageClassifierFloatInception
/**
 * Initializes an `ImageClassifier`.
 *
 * @param activity
 */
@Throws(IOException::class)
internal constructor(activity: Activity) : ImageClassifier(activity) {

  /**
   * An array to hold inference results, to be feed into Tensorflow Lite as outputs.
   * This isn't part of the super class, because we need a primitive array here.
   */
  private var labelProbArray: Array<Array<Array<FloatArray>>>? = null

  override// you can download this file from
  // https://storage.googleapis.com/download.tensorflow.org/models/tflite/inception_v3_slim_2016_android_2017_11_10.zip
  val modelPath: String
    get() = "model-85.tflite"

  override val labelPath: String
    get() = "labels.txt"

  override val imageSizeX: Int
    get() = 224

  override val imageSizeY: Int
    get() = 224

  override// a 32bit float value requires 4 bytes
  val numBytesPerChannel: Int
    get() = 4

  private var mMat: Mat? = null

  init {
    labelProbArray = Array(1) { Array(112) { Array(112) { FloatArray(15) } } }
  }

  override fun addPixelValue(pixelValue: Int) {
    //        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    //        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    //        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

    //rgb
    //        imgData.put((byte) ((pixelValue >> 16) & 0xFF));
    //        imgData.put((byte) ((pixelValue >> 8) & 0xFF));
    //        imgData.put((byte) (pixelValue & 0xFF));

    //bgr
    imgData!!.putFloat((pixelValue and 0xFF).toFloat())
    imgData!!.putFloat((pixelValue shr 8 and 0xFF).toFloat())
    imgData!!.putFloat((pixelValue shr 16 and 0xFF).toFloat())

    //        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    //        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    //        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
  }

  override fun getProbability(labelIndex: Int): Float {
    //    return labelProbArray[0][labelIndex];
    return 0f
  }

  override fun setProbability(
    labelIndex: Int,
    value: Number
  ) {
    //    labelProbArray[0][labelIndex] = value.floatValue();
  }

  override fun getNormalizedProbability(labelIndex: Int): Float {
    // TODO the following value isn't in [0,1] yet, but may be greater. Why?
    return getProbability(labelIndex)
  }

  override fun runInference() {
    tflite?.run(imgData!!, labelProbArray!!)

    if (mPrintPointArray == null)
      mPrintPointArray = Array(2) { FloatArray(15) }

    if (!CameraActivity.isOpenCVInit)
      return

    //先进行高斯滤波,5*5
    if (mMat == null)
      mMat = Mat(112, 112, CvType.CV_32F)

    val tempArray = FloatArray(112 * 112)
    val outTempArray = FloatArray(112 * 112)
    for (i in 0..14) {
      var index = 0
      for (x in 0..111) {
        for (y in 0..111) {
          tempArray[index] = labelProbArray!![0][y][x][i]
          index++
        }
      }

      mMat!!.put(0, 0, tempArray)
      Imgproc.GaussianBlur(mMat!!, mMat!!, Size(5.0, 5.0), 0.0, 0.0)
      mMat!!.get(0, 0, outTempArray)

      var maxX = 0f
      var maxY = 0f
      var max = 0f

      //比较上下左右4个点与自身的点的最大值
      for (x in 0..111) {
        for (y in 0..111) {
          val top = get(x, y - 1, outTempArray)
          val left = get(x - 1, y, outTempArray)
          val right = get(x + 1, y, outTempArray)
          val bottom = get(x, y + 1, outTempArray)
          val center = get(x, y, outTempArray)

          if (center > top &&
              center > left &&
              center > right &&
              center > bottom &&
              center >= 0.01
          ) {

            if (center > max) {
              max = center
              maxX = x.toFloat()
              maxY = y.toFloat()
            }
          }
        }
      }

      if (max == 0f) {
        mPrintPointArray = Array(2) { FloatArray(15) }
        return
      }

      mPrintPointArray!![0][i] = maxX
      mPrintPointArray!![1][i] = maxY
      Log.i("TestOutPut", "pic[$i] ($maxX,$maxY) $max")
    }

    //        if (mPrintPointArray == null)
    //            mPrintPointArray = new float[2][15];
    //
    //        for (int i = 0; i < 15; i++) {
    //            float maxX = 0, maxY = 0;
    //            float max = 0;
    //            for (int x = 0; x < 112; x++) {
    //                for (int y = 0; y < 112; y++) {
    ////                    float t = Math.abs(labelProbArray[0][x][y][i]);
    //                    float t = labelProbArray[0][x][y][i];
    //                    if (t >= max) {
    //                        max = t;
    ////                        maxX = x;
    ////                        maxY = y;
    //                        maxX = y;
    //                        maxY = x;
    //                    }
    //                }
    //            }
    //            mPrintPointArray[0][i] = maxX;
    //            mPrintPointArray[1][i] = maxY;
    //            Log.i("Fucker", "pic[" + i + "] (" + maxX + "," + maxY + ") " + max);
    //        }
  }

  private operator fun get(
    x: Int,
    y: Int,
    arr: FloatArray
  ): Float {
    return if (x < 0 || y < 0 || x >= 112 || y >= 112) -1f else arr[x * 112 + y]
  }

  companion object {

    /**
     * The inception net requires additional normalization of the used input.
     */
    private val IMAGE_MEAN = 128
    private val IMAGE_STD = 128.0f
  }
}
