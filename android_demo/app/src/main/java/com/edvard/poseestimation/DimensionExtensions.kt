package com.edvard.poseestimation

import android.content.Context
import android.view.View

/** Copy from [anko](https://github.com/Kotlin/anko/blob/d5a526512b48c5cd2e3b8f6ff14b153c2337aa22/anko/library/static/commons/src/Dimensions.kt)
 */
//returns dip(dp) dimension value in pixels
fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun Context.dip(value: Float): Int = (value * resources.displayMetrics.density).toInt()

//return sp dimension value in pixels
fun Context.sp(value: Int): Int = (value * resources.displayMetrics.scaledDensity).toInt()

fun Context.sp(value: Float): Int = (value * resources.displayMetrics.scaledDensity).toInt()

//the same for the views
inline fun View.dip(value: Int): Int = context.dip(value)

inline fun View.dip(value: Float): Int = context.dip(value)
inline fun View.sp(value: Int): Int = context.sp(value)
inline fun View.sp(value: Float): Int = context.sp(value)