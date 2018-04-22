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

package com.edvard.poseestimation;

import android.app.Activity;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

import static com.edvard.poseestimation.CameraActivity.isOpenCVInit;

/**
 * This classifier works with the Inception-v3 slim model.
 * It applies floating point inference rather than using a quantized model.
 */
public class ImageClassifierFloatInception extends ImageClassifier {

    /**
     * The inception net requires additional normalization of the used input.
     */
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs.
     * This isn't part of the super class, because we need a primitive array here.
     */
    private float[][][][] labelProbArray = null;

    /**
     * Initializes an {@code ImageClassifier}.
     *
     * @param activity
     */
    ImageClassifierFloatInception(Activity activity) throws IOException {
        super(activity);
        labelProbArray = new float[1][112][112][15];
    }

    @Override
    protected String getModelPath() {
        // you can download this file from
        // https://storage.googleapis.com/download.tensorflow.org/models/tflite/inception_v3_slim_2016_android_2017_11_10.zip
        return "model-85.tflite";
    }

    @Override
    protected String getLabelPath() {
        return "labels.txt";
    }

    @Override
    protected int getImageSizeX() {
        return 224;
    }

    @Override
    protected int getImageSizeY() {
        return 224;
    }

    @Override
    protected int getNumBytesPerChannel() {
        // a 32bit float value requires 4 bytes
        return 4;
    }

    @Override
    protected void addPixelValue(int pixelValue) {
//        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

        //rgb
//        imgData.put((byte) ((pixelValue >> 16) & 0xFF));
//        imgData.put((byte) ((pixelValue >> 8) & 0xFF));
//        imgData.put((byte) (pixelValue & 0xFF));

        //bgr
        imgData.putFloat(pixelValue & 0xFF);
        imgData.putFloat((pixelValue >> 8) & 0xFF);
        imgData.putFloat((pixelValue >> 16) & 0xFF);

//        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    }

    @Override
    protected float getProbability(int labelIndex) {
//    return labelProbArray[0][labelIndex];
        return 0.f;
    }

    @Override
    protected void setProbability(int labelIndex, Number value) {
//    labelProbArray[0][labelIndex] = value.floatValue();
    }

    @Override
    protected float getNormalizedProbability(int labelIndex) {
        // TODO the following value isn't in [0,1] yet, but may be greater. Why?
        return getProbability(labelIndex);
    }

    private Mat mMat;

    @Override
    protected void runInference() {
        tflite.run(imgData, labelProbArray);

        if (mPrintPointArray == null)
            mPrintPointArray = new float[2][15];

        if (!isOpenCVInit)
            return;

        //先进行高斯滤波,5*5
        if (mMat == null)
            mMat = new Mat(112, 112, CvType.CV_32F);

        float[] tempArray = new float[112 * 112];
        float[] outTempArray = new float[112 * 112];
        for (int i = 0; i < 15; i++) {
            int index = 0;
            for (int x = 0; x < 112; x++) {
                for (int y = 0; y < 112; y++) {
                    tempArray[index] = labelProbArray[0][y][x][i];
                    index++;
                }
            }

            mMat.put(0, 0, tempArray);
            Imgproc.GaussianBlur(mMat, mMat, new Size(5, 5), 0, 0);
            mMat.get(0, 0, outTempArray);

            float maxX = 0, maxY = 0;
            float max = 0;

            //比较上下左右4个点与自身的点的最大值
            for (int x = 0; x < 112; x++) {
                for (int y = 0; y < 112; y++) {
                    float top = get(x, y - 1, outTempArray);
                    float left = get(x - 1, y, outTempArray);
                    float right = get(x + 1, y, outTempArray);
                    float bottom = get(x, y + 1, outTempArray);
                    float center = get(x, y, outTempArray);

                    if (center > top &&
                            center > left &&
                            center > right &&
                            center > bottom &&
                            center >= 0.01) {

                        if (center > max) {
                            max = center;
                            maxX = x;
                            maxY = y;
                        }
                    }
                }
            }

            if (max == 0) {
                mPrintPointArray = new float[2][15];
                return;
            }

            mPrintPointArray[0][i] = maxX;
            mPrintPointArray[1][i] = maxY;
            Log.i("TestOutPut", "pic[" + i + "] (" + maxX + "," + maxY + ") " + max);
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

    private float get(int x, int y, float[] arr) {
        if (x < 0 || y < 0 || x >= 112 || y >= 112)
            return -1;
        return arr[x * 112 + y];
    }
}
