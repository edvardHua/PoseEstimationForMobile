package com.edvard.poseestimation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style.FILL
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import java.util.ArrayList

/**
 * Created by edvard on 18-3-23.
 */

class DrawView : View {

  private var mRatioWidth = 0
  private var mRatioHeight = 0

  private val mDrawPoint = ArrayList<PointF>()
  private var mWidth: Int = 0
  private var mHeight: Int = 0
  private var mRatioX: Float = 0.toFloat()
  private var mRatioY: Float = 0.toFloat()
  private var mImgWidth: Int = 0
  private var mImgHeight: Int = 0

  private val mColorArray = intArrayOf(
      resources.getColor(R.color.color_top, null),
      resources.getColor(R.color.color_neck, null),
      resources.getColor(R.color.color_l_shoulder, null),
      resources.getColor(R.color.color_l_elbow, null),
      resources.getColor(R.color.color_l_wrist, null),
      resources.getColor(R.color.color_r_shoulder, null),
      resources.getColor(R.color.color_r_elbow, null),
      resources.getColor(R.color.color_r_wrist, null),
      resources.getColor(R.color.color_l_hip, null),
      resources.getColor(R.color.color_l_knee, null),
      resources.getColor(R.color.color_l_ankle, null),
      resources.getColor(R.color.color_r_hip, null),
      resources.getColor(R.color.color_r_knee, null),
      resources.getColor(R.color.color_r_ankle, null),
      resources.getColor(R.color.color_background, null)
  )

  private val circleRadius: Float by lazy {
    dip(3).toFloat()
  }

  private val mPaint: Paint by lazy {
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
      style = FILL
      strokeWidth = dip(2).toFloat()
      textSize = sp(13).toFloat()
    }
  }

  constructor(context: Context) : super(context)

  constructor(
    context: Context,
    attrs: AttributeSet?
  ) : super(context, attrs)

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
  ) : super(context, attrs, defStyleAttr)

  fun setImgSize(
    width: Int,
    height: Int
  ) {
    mImgWidth = width
    mImgHeight = height
    requestLayout()
  }

  /**
   * Scale according to the device.
   * @param point 2*14
   */
  fun setDrawPoint(
    point: Array<FloatArray>,
    ratio: Float
  ) {
    mDrawPoint.clear()

    var tempX: Float
    var tempY: Float
    for (i in 0..13) {
      tempX = point[0][i] / ratio / mRatioX
      tempY = point[1][i] / ratio / mRatioY
      mDrawPoint.add(PointF(tempX, tempY))
    }
  }

  /**
   * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
   * calculated from the parameters. Note that the actual sizes of parameters don't matter, that is,
   * calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
   *
   * @param width  Relative horizontal size
   * @param height Relative vertical size
   */
  fun setAspectRatio(
    width: Int,
    height: Int
  ) {
    if (width < 0 || height < 0) {
      throw IllegalArgumentException("Size cannot be negative.")
    }
    mRatioWidth = width
    mRatioHeight = height
    requestLayout()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (mDrawPoint.isEmpty()) return
    var prePointF: PointF? = null
    mPaint.color = 0xff6fa8dc.toInt()
    val p1 = mDrawPoint[1]
    for ((index, pointF) in mDrawPoint.withIndex()) {
      if (index == 1) continue
      when (index) {
      //0-1
        0 -> {
          canvas.drawLine(pointF.x, pointF.y, p1.x, p1.y, mPaint)
        }
      // 1-2, 1-5, 1-8, 1-11
        2, 5, 8, 11 -> {
          canvas.drawLine(p1.x, p1.y, pointF.x, pointF.y, mPaint)
        }
        else -> {
          if (prePointF != null) {
            mPaint.color = 0xff6fa8dc.toInt()
            canvas.drawLine(prePointF.x, prePointF.y, pointF.x, pointF.y, mPaint)
          }
        }
      }
      prePointF = pointF
    }

    for ((index, pointF) in mDrawPoint.withIndex()) {
      mPaint.color = mColorArray[index]
      canvas.drawCircle(pointF.x, pointF.y, circleRadius, mPaint)
    }
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    val width = View.MeasureSpec.getSize(widthMeasureSpec)
    val height = View.MeasureSpec.getSize(heightMeasureSpec)
    if (0 == mRatioWidth || 0 == mRatioHeight) {
      setMeasuredDimension(width, height)
    } else {
      if (width < height * mRatioWidth / mRatioHeight) {
        mWidth = width
        mHeight = width * mRatioHeight / mRatioWidth
      } else {
        mWidth = height * mRatioWidth / mRatioHeight
        mHeight = height
      }
    }

    setMeasuredDimension(mWidth, mHeight)

    mRatioX = mImgWidth.toFloat() / mWidth
    mRatioY = mImgHeight.toFloat() / mHeight
  }
}
