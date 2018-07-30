/*
 * Copyright 2018 Zihua Zeng (edvard_hua@live.com)
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

package com.example;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import com.xiaomi.mace.JniMaceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;


public abstract class PoseEstimation {

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "PoseEstimationDemo";

    /* Preallocated buffers for storing image data in. */
    private int[] intValues = new int[getImageSizeX() * getImageSizeY()];

    /**
     * A FloatBuffer to hold input image data, to be feed into Tensorflow Lite as inputs.
     */
    protected FloatBuffer floatBuffer = null;

    public float[][] mPrintPointArray = null;

    private String kernelPath = null;

    /**
     * Initializes an {@code PoseEstimation}.
     */
    PoseEstimation(Activity activity) throws IOException {

        int lengthValues = getImageSizeY() * getImageSizeX() * 3;
        float[] floatValues = new float[lengthValues];
        floatBuffer = FloatBuffer.wrap(floatValues, 0, lengthValues);
        kernelPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "mace";
        File file = new File(kernelPath);
        if (!file.exists()) {
            file.mkdir();
        }

        int result = JniMaceUtils.maceMobilenetSetAttrs(
                2, 0,
                3, 3,
                kernelPath);
        Log.i("APPModel", "maceMobilenetSetAttrs result = " + result);
        JniMaceUtils.maceMobilenetCreateEngine("cpm_v1", "GPU");
    }

    /**
     * Classifies a frame from the preview stream.
     */
    String classifyFrame(Bitmap bitmap) {
        convertBitmapToByteBuffer(bitmap);
        // Here's where the magic happens!!!
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        String textToShow = Long.toString(endTime - startTime) + "ms";
        return textToShow;
    }

    public void close() {
    }

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (floatBuffer == null) {
            return;
        }
        floatBuffer.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }


    /**
     * Get the image size along the x axis.
     *
     * @return
     */
    protected abstract int getImageSizeX();

    /**
     * Get the image size along the y axis.
     *
     * @return
     */
    protected abstract int getImageSizeY();

    /**
     * Get the image size along the x axis.
     *
     * @return
     */
    protected abstract int getOutputSizeX();

    /**
     * Get the image size along the y axis.
     *
     * @return
     */
    protected abstract int getOutputSizeY();

    /**
     * Add pixelValue to byteBuffer.
     *
     * @param pixelValue
     */
    protected abstract void addPixelValue(int pixelValue);

    /**
     * Run inference using the prepared input in {@link #floatBuffer}. Afterwards, the result will be
     * provided by getProbability().
     * <p>
     * <p>This additional method is necessary, because we don't have a common base for different
     * primitive data types.
     */
    protected abstract void runInference();
}
