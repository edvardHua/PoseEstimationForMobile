package com.edvard.poseestimation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/**
 * Created by edvard on 18-3-23.
 */

class AutoFitFrameLayout : FrameLayout {

  private var mRatioWidth = 0
  private var mRatioHeight = 0

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
        setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
      } else {
        setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
      }
    }
  }
}
