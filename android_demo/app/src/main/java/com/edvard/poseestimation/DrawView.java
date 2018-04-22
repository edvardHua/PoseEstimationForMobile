package com.edvard.poseestimation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by edvard on 18-3-23.
 */

public class DrawView extends View {

    public static final int COLOR_TOP = Color.parseColor("#980000");
    public static final int COLOR_NECK = Color.parseColor("#ff0000");
    public static final int COLOR_RSHOULDER = Color.parseColor("#ff9900");
    public static final int COLOR_RELBOW = Color.parseColor("#ffff00");
    public static final int COLOR_RWRIST = Color.parseColor("#00ff00");
    public static final int COLOR_LSHOULDER = Color.parseColor("#00ffff");
    public static final int COLOR_LELBOW = Color.parseColor("#4a86e8");
    public static final int COLOR_LWRIST = Color.parseColor("#0000ff");
    public static final int COLOR_RHIP = Color.parseColor("#9900ff");
    public static final int COLOR_RKNEE = Color.parseColor("#274e13");
    public static final int COLOR_RANKLE = Color.parseColor("#e6b8af");
    public static final int COLOR_LHIP = Color.parseColor("#0c343d");
    public static final int COLOR_LKNEE = Color.parseColor("#1c4587");
    public static final int COLOR_LANKLE = Color.parseColor("#073763");
    public static final int COLOR_BACKGROUND = Color.parseColor("#20124d");

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private final ArrayList<PointF> mDrawPoint = new ArrayList<>();
    private int mWidth, mHeight;
    private float mRatioX, mRatioY;
    private int mImgWidth, mImgHeight;

    private final int[] mColorArray = new int[]{
            COLOR_TOP, COLOR_NECK,
            COLOR_RSHOULDER, COLOR_RELBOW, COLOR_RWRIST,
            COLOR_LSHOULDER, COLOR_LELBOW, COLOR_LWRIST,
            COLOR_RHIP, COLOR_RKNEE, COLOR_RANKLE,
            COLOR_LHIP, COLOR_LKNEE, COLOR_LANKLE,
            COLOR_BACKGROUND
    };
    private Paint mPaint;

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImgSize(int width, int height) {
        mImgWidth = width;
        mImgHeight = height;
        requestLayout();
    }

    /**
     * 输入的为92*92的图,然后按照比例放大
     * 先按ratio放大,再按机器实际尺寸放大
     *
     * @param point 2*14
     */
    public void setDrawPoint(float[][] point, float ratio) {
        mDrawPoint.clear();

        float tempX, tempY;
        for (int i = 0; i < 15; i++) {
            tempX = point[0][i] / ratio / mRatioX;
            tempY = point[1][i] / ratio / mRatioY;
            mDrawPoint.add(new PointF(tempX, tempY));
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
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);
        }

        canvas.drawColor(Color.TRANSPARENT);

        int colorIndex = 0;
        for (PointF pointF : mDrawPoint) {
//            if (pointF.x == 0 && pointF.y == 0) {
//                colorIndex++;
//                continue;
//            }
            mPaint.setColor(mColorArray[colorIndex++]);
            canvas.drawCircle(pointF.x, pointF.y, 8, mPaint);
        }

        mPaint.setColor(Color.parseColor("#6fa8dc"));
        mPaint.setStrokeWidth(5);

        if (mDrawPoint.size() <= 0)
            return;

        PointF p0 = mDrawPoint.get(0);
        PointF p1 = mDrawPoint.get(1);
        PointF p2 = mDrawPoint.get(2);
        PointF p3 = mDrawPoint.get(3);
        PointF p4 = mDrawPoint.get(4);
        PointF p5 = mDrawPoint.get(5);
        PointF p6 = mDrawPoint.get(6);
        PointF p7 = mDrawPoint.get(7);
        PointF p8 = mDrawPoint.get(8);
        PointF p9 = mDrawPoint.get(9);
        PointF p10 = mDrawPoint.get(10);
        PointF p11 = mDrawPoint.get(11);
        PointF p12 = mDrawPoint.get(12);
        PointF p13 = mDrawPoint.get(13);

        //0-1
        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, mPaint);

        //1-2
        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mPaint);

        //2-3
        canvas.drawLine(p2.x, p2.y, p3.x, p3.y, mPaint);

        //3-4
        canvas.drawLine(p3.x, p3.y, p4.x, p4.y, mPaint);

        //1-5
        canvas.drawLine(p1.x, p1.y, p5.x, p5.y, mPaint);

        //5-6
        canvas.drawLine(p5.x, p5.y, p6.x, p6.y, mPaint);

        //6-7
        canvas.drawLine(p6.x, p6.y, p7.x, p7.y, mPaint);

        //1-11
        canvas.drawLine(p1.x, p1.y, p11.x, p11.y, mPaint);

        //11-12
        canvas.drawLine(p11.x, p11.y, p12.x, p12.y, mPaint);

        //12-13
        canvas.drawLine(p12.x, p12.y, p13.x, p13.y, mPaint);

        //1-8
        canvas.drawLine(p1.x, p1.y, p8.x, p8.y, mPaint);

        //8-9
        canvas.drawLine(p8.x, p8.y, p9.x, p9.y, mPaint);

        //9-10
        canvas.drawLine(p9.x, p9.y, p10.x, p10.y, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                mWidth = width;
                mHeight = width * mRatioHeight / mRatioWidth;
            } else {
                mWidth = height * mRatioWidth / mRatioHeight;
                mHeight = height;
            }
        }

        setMeasuredDimension(mWidth, mHeight);

        mRatioX = ((float) mImgWidth) / mWidth;
        mRatioY = ((float) mImgHeight) / mHeight;
    }
}
