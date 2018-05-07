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
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Long
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.util.AbstractMap.SimpleEntry
import java.util.ArrayList
import java.util.Comparator
import java.util.PriorityQueue

/**
 * Classifies images with Tensorflow Lite.
 */
abstract class ImageClassifier
/** Initializes an `ImageClassifier`.  */
@Throws(IOException::class)
internal constructor(activity: Activity) {

  /* Preallocated buffers for storing image data in. */
  private val intValues = IntArray(imageSizeX * imageSizeY)

  /** An instance of the driver class to run model inference with Tensorflow Lite.  */
  protected var tflite: Interpreter? = null

  /** Labels corresponding to the output of the vision model.  */
  private val labelList: List<String>

  /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.  */
  protected var imgData: ByteBuffer? = null

  /** multi-stage low pass filter *  */
  private var filterLabelProbArray: Array<FloatArray>? = null

  var mPrintPointArray: Array<FloatArray>? = null

  private val sortedLabels = PriorityQueue<Map.Entry<String, Float>>(
      RESULTS_TO_SHOW,
      Comparator<Map.Entry<String, Float>> { o1, o2 -> o1.value.compareTo(o2.value) })

  /**
   * Get the name of the model file stored in Assets.
   *
   * @return
   */
  protected abstract val modelPath: String

  /**
   * Get the name of the label file stored in Assets.
   *
   * @return
   */
  protected abstract val labelPath: String

  /**
   * Get the image size along the x axis.
   *
   * @return
   */
  abstract val imageSizeX: Int

  /**
   * Get the image size along the y axis.
   *
   * @return
   */
  abstract val imageSizeY: Int

  /**
   * Get the number of bytes that is used to store a single color channel value.
   *
   * @return
   */
  protected abstract val numBytesPerChannel: Int

  /**
   * Get the total number of labels.
   *
   * @return
   */
  private val numLabels: Int
    get() = labelList.size

  init {
    tflite = Interpreter(loadModelFile(activity))
    labelList = loadLabelList(activity)
    imgData = ByteBuffer.allocateDirect(
        DIM_BATCH_SIZE
            * imageSizeX
            * imageSizeY
            * DIM_PIXEL_SIZE
            * numBytesPerChannel
    )
    imgData!!.order(ByteOrder.nativeOrder())
    filterLabelProbArray = Array(FILTER_STAGES) { FloatArray(numLabels) }
    Log.d(TAG, "Created a Tensorflow Lite Image Classifier.")
  }

  /** Classifies a frame from the preview stream.  */
  public fun classifyFrame(bitmap: Bitmap): String {
    if (tflite == null) {
      Log.e(TAG, "Image classifier has not been initialized; Skipped.")
      return "Uninitialized Classifier."
    }
    convertBitmapToByteBuffer(bitmap)
    // Here's where the magic happens!!!
    val startTime = SystemClock.uptimeMillis()
    runInference()
    val endTime = SystemClock.uptimeMillis()
    Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime))

    // Smooth the results across frames.
    applyFilter()

    // Print the results.
    //    String textToShow = printTopKLabels();
    return Long.toString(endTime - startTime) + "ms"
  }

  internal fun applyFilter() {
    val numLabels = numLabels

    // Low pass filter `labelProbArray` into the first stage of the filter.
    for (j in 0 until numLabels) {
      filterLabelProbArray!![0][j] += FILTER_FACTOR * (getProbability(
          j
      ) - filterLabelProbArray!![0][j])
    }
    // Low pass filter each stage into the next.
    for (i in 1 until FILTER_STAGES) {
      for (j in 0 until numLabels) {
        filterLabelProbArray!![i][j] += FILTER_FACTOR * (filterLabelProbArray!![i - 1][j] - filterLabelProbArray!![i][j])
      }
    }

    // Copy the last stage filter output back to `labelProbArray`.
    for (j in 0 until numLabels) {
      setProbability(j, filterLabelProbArray!![FILTER_STAGES - 1][j])
    }
  }

  /** Closes tflite to release resources.  */
  fun close() {
    tflite!!.close()
    tflite = null
  }

  /** Reads label list from Assets.  */
  @Throws(IOException::class)
  private fun loadLabelList(activity: Activity): List<String> {
    val labelList = ArrayList<String>()
    val reader = BufferedReader(InputStreamReader(activity.assets.open(labelPath)))
    var line: String? = reader.readLine()
    while (line != null) {
      labelList.add(line)
      line = reader.readLine()
    }
    reader.close()
    return labelList
  }

  /** Memory-map the model file in Assets.  */
  @Throws(IOException::class)
  private fun loadModelFile(activity: Activity): MappedByteBuffer {
    val fileDescriptor = activity.assets.openFd(modelPath)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(MapMode.READ_ONLY, startOffset, declaredLength)
  }

  /** Writes Image data into a `ByteBuffer`.  */
  private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
    if (imgData == null) {
      return
    }
    imgData!!.rewind()
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    // Convert the image to floating point.
    var pixel = 0
    val startTime = SystemClock.uptimeMillis()
    for (i in 0 until imageSizeX) {
      for (j in 0 until imageSizeY) {
        val v = intValues[pixel++]
        addPixelValue(v)
      }
    }
    val endTime = SystemClock.uptimeMillis()
    Log.d(
        TAG,
        "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime)
    )
  }

  /** Prints top-K labels, to be shown in UI as the results.  */
  private fun printTopKLabels(): String {
    for (i in 0 until numLabels) {
      sortedLabels.add(
          SimpleEntry(labelList[i], getNormalizedProbability(i))
      )
      if (sortedLabels.size > RESULTS_TO_SHOW) {
        sortedLabels.poll()
      }
    }
    var textToShow = ""
    val size = sortedLabels.size
    for (i in 0 until size) {
      val label = sortedLabels.poll()
      textToShow = String.format("\n%s: %4.2f", label.key, label.value) + textToShow
    }
    return textToShow
  }

  /**
   * Add pixelValue to byteBuffer.
   *
   * @param pixelValue
   */
  protected abstract fun addPixelValue(pixelValue: Int)

  /**
   * Read the probability value for the specified label This is either the original value as it was
   * read from the net's output or the updated value after the filter was applied.
   *
   * @param labelIndex
   * @return
   */
  protected abstract fun getProbability(labelIndex: Int): Float

  /**
   * Set the probability value for the specified label.
   *
   * @param labelIndex
   * @param value
   */
  protected abstract fun setProbability(
    labelIndex: Int,
    value: Number
  )

  /**
   * Get the normalized probability value for the specified label. This is the final value as it
   * will be shown to the user.
   *
   * @return
   */
  protected abstract fun getNormalizedProbability(labelIndex: Int): Float

  /**
   * Run inference using the prepared input in [.imgData]. Afterwards, the result will be
   * provided by getProbability().
   *
   *
   * This additional method is necessary, because we don't have a common base for different
   * primitive data types.
   */
  protected abstract fun runInference()

  companion object {

    /** Tag for the [Log].  */
    private val TAG = "TfLiteCameraDemo"

    /** Number of results to show in the UI.  */
    private val RESULTS_TO_SHOW = 3

    /** Dimensions of inputs.  */
    private val DIM_BATCH_SIZE = 1

    private val DIM_PIXEL_SIZE = 3

    private val FILTER_STAGES = 3
    private val FILTER_FACTOR = 0.4f
  }
}
